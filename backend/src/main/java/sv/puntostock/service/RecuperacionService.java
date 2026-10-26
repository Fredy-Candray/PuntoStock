package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.repository.UsuarioRepository;

@Service
public class RecuperacionService {
  private final Clock clock;
  private final UsuarioRepository usuarios;
  private final PasswordEncoder passwords;
  private final CorreoOutbox correo;
  private final String baseUrl;
  private final sv.puntostock.security.LimiteIntentos limites;

  public RecuperacionService(
      UsuarioRepository usuarios,
      PasswordEncoder passwords,
      CorreoOutbox correo,
      Clock clock,
      sv.puntostock.security.LimiteIntentos limites,
      @Value("${app.recovery.base-url:http://127.0.0.1:14200}") String baseUrl) {
    this.usuarios = usuarios;
    this.passwords = passwords;
    this.correo = correo;
    this.clock = clock;
    this.limites = limites;
    this.baseUrl = baseUrl;
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public void solicitar(String username, String correo) {
    limites.comprobar("recuperacion", null, username);
    if (!this.correo.disponible())
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "Recuperación por correo aún no configurada");
    var usuario =
        usuarios
            .bloquear(username.trim())
            .filter(u -> u.activo && u.correo != null && u.correo.equalsIgnoreCase(correo.trim()));
    if (usuario.isEmpty()) return;
    var u = usuario.get();
    var now = LocalDateTime.now(clock);
    if (u.recuperacionSolicitada != null && u.recuperacionSolicitada.plusMinutes(2).isAfter(now))
      return;
    byte[] random = new byte[32];
    new SecureRandom().nextBytes(random);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    u.recuperacionHash = hash(token);
    u.recuperacionExpira = now.plusMinutes(15);
    u.recuperacionSolicitada = now;
    String link =
        baseUrl.replaceAll("/+$", "")
            + "/login#recuperar="
            + token
            + "&usuario="
            + java.net.URLEncoder.encode(u.username, StandardCharsets.UTF_8);
    this.correo.encolar(u, link);
  }

  @Transactional
  public void restablecer(String username, String token, String password) {
    limites.comprobar("restablecer", null, username);
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(BAD_REQUEST, "La contraseña excede 72 bytes");
    var u =
        usuarios
            .bloquear(username.trim())
            .filter(x -> x.activo)
            .orElseThrow(RecuperacionService::invalido);
    if (u.recuperacionHash == null
        || u.recuperacionExpira == null
        || !u.recuperacionExpira.isAfter(LocalDateTime.now(clock))
        || !MessageDigest.isEqual(
            u.recuperacionHash.getBytes(StandardCharsets.UTF_8),
            hash(token).getBytes(StandardCharsets.UTF_8))) throw invalido();
    u.passwordHash = passwords.encode(password);
    u.authVersion++;
    u.recuperacionHash = null;
    u.recuperacionExpira = null;
  }

  private static ResponseStatusException invalido() {
    return new ResponseStatusException(
        BAD_REQUEST, "El enlace es inválido o venció. Solicita uno nuevo.");
  }
}
