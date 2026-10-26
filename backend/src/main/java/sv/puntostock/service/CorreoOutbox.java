package sv.puntostock.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sv.puntostock.entity.Usuario;

@Service
public class CorreoOutbox {
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CorreoOutbox.class);
  private final JdbcTemplate sql;
  private final ObjectProvider<JavaMailSender> sender;
  private final Clock clock;
  private final String from;
  private final boolean enabled;
  private final SecretKeySpec key;

  public CorreoOutbox(
      JdbcTemplate sql,
      ObjectProvider<JavaMailSender> sender,
      Clock clock,
      @Value("${app.recovery.from:}") String from,
      @Value("${app.recovery.enabled:false}") boolean enabled,
      @Value("${app.jwt-secret}") String secret) {
    this.sql = sql;
    this.sender = sender;
    this.clock = clock;
    this.from = from;
    this.enabled = enabled;
    try {
      key =
          new SecretKeySpec(
              MessageDigest.getInstance("SHA-256")
                  .digest(("puntostock-outbox:" + secret).getBytes(StandardCharsets.UTF_8)),
              "AES");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean disponible() {
    return enabled && !from.isBlank() && sender.getIfAvailable() != null;
  }

  public void encolar(Usuario u, String enlace) {
    sql.update(
        "update correo_pendiente set estado='REEMPLAZADO',contenido_cifrado=null where usuario_id=? and estado='PENDIENTE'",
        u.id);
    long now = clock.instant().getEpochSecond();
    sql.update(
        "insert into correo_pendiente(usuario_id,recuperacion_hash,contenido_cifrado,expira_epoch,proximo_epoch) values(?,?,?,?,?)",
        u.id,
        u.recuperacionHash,
        cifrar(u.correo + "\n" + enlace),
        now + 900,
        now);
  }

  @Scheduled(fixedDelayString = "${app.recovery.worker-delay:10000}")
  public void enviarPendientes() {
    if (!disponible()) return;
    long now = clock.instant().getEpochSecond();
    sql.update(
        "update correo_pendiente set estado='VENCIDO',contenido_cifrado=null where expira_epoch<=? and contenido_cifrado is not null",
        now);
    for (int i = 0; i < 5; i++) {
      var rows =
          sql.queryForList(
              "update top (1) correo_pendiente with (updlock,readpast,rowlock) set lease_epoch=?,intentos=intentos+1 output inserted.* where estado='PENDIENTE' and expira_epoch>? and proximo_epoch<=? and lease_epoch<=? and intentos<5",
              now + 60,
              now,
              now,
              now);
      if (rows.isEmpty()) break;
      var row = rows.getFirst();
      long id = ((Number) row.get("id")).longValue();
      var valido =
          sql.queryForObject(
              "select count(*) from usuarios where id=? and activo=1 and recuperacion_hash=?",
              Integer.class,
              row.get("usuario_id"),
              row.get("recuperacion_hash"));
      if (valido == null || valido != 1) {
        sql.update(
            "update correo_pendiente set estado='CANCELADO',contenido_cifrado=null where id=?", id);
        continue;
      }
      try {
        String[] contenido = descifrar(row.get("contenido_cifrado").toString()).split("\n", 2);
        var m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(contenido[0]);
        m.setSubject("Restablece tu contraseña de PuntoStock");
        m.setText(
            "Solicitaste restablecer tu contraseña de PuntoStock.\n\nEnlace de un solo uso, válido por 15 minutos desde la solicitud:\n"
                + contenido[1]
                + "\n\nSi no lo solicitaste, ignora este correo. Tu contraseña no ha cambiado.");
        sender.getObject().send(m);
        sql.update(
            "update correo_pendiente set estado='ENVIADO',contenido_cifrado=null where id=?", id);
        LOG.info("Correo de recuperación aceptado por SMTP; solicitud={}", id);
      } catch (Exception e) {
        int intentos = ((Number) row.get("intentos")).intValue();
        String estado = intentos >= 5 ? "FALLIDO" : "PENDIENTE";
        sql.update(
            "update correo_pendiente set estado=?,proximo_epoch=?,lease_epoch=0,contenido_cifrado=case when ?='FALLIDO' then null else contenido_cifrado end where id=?",
            estado,
            now + Math.min(300, 30L * intentos),
            estado,
            id);
        LOG.warn(
            "Fallo de envío de recuperación; solicitud={}, intento={}, causa={}",
            id,
            intentos,
            e.getClass().getSimpleName());
      }
    }
    sql.update("delete from correo_pendiente where expira_epoch<?", now - 86400);
  }

  private String cifrar(String contenido) {
    try {
      byte[] nonce = new byte[12];
      new SecureRandom().nextBytes(nonce);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      byte[] encrypted = c.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
      byte[] result = new byte[nonce.length + encrypted.length];
      System.arraycopy(nonce, 0, result, 0, 12);
      System.arraycopy(encrypted, 0, result, 12, encrypted.length);
      return Base64.getEncoder().encodeToString(result);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("No se pudo proteger el correo pendiente", e);
    }
  }

  private String descifrar(String contenido) throws GeneralSecurityException {
    byte[] bytes = Base64.getDecoder().decode(contenido);
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, bytes, 0, 12));
    return new String(c.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
  }
}
