package sv.puntostock.security;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.repository.UsuarioRepository;

@Service
public class Sesiones {
  public record Acceso(String accessToken, long expiresIn, List<String> roles, String username) {}

  public record Emitida(Acceso acceso, String renovacion, boolean recordar) {}

  private final JdbcTemplate sql;
  private final UsuarioRepository usuarios;
  private final PasswordEncoder claves;
  private final JwtEncoder jwt;
  private final Clock clock;
  private final LimiteIntentos limites;
  private final String ficticia;

  public Sesiones(
      JdbcTemplate sql,
      UsuarioRepository usuarios,
      PasswordEncoder claves,
      JwtEncoder jwt,
      Clock clock,
      LimiteIntentos limites) {
    this.sql = sql;
    this.usuarios = usuarios;
    this.claves = claves;
    this.jwt = jwt;
    this.clock = clock;
    this.limites = limites;
    this.ficticia = claves.encode(UUID.randomUUID().toString());
  }

  @Transactional
  public Emitida ingresar(String username, String password, boolean recordar) {
    limites.comprobar("login", null, username);
    var encontrado = usuarios.acceso(username.trim());
    boolean coincide =
        claves.matches(
            password, encontrado.map(UsuarioRepository.Acceso::getPasswordHash).orElse(ficticia));
    var u = encontrado.orElse(null);
    if (!coincide || u == null || !u.getActivo()) throw invalida();
    String id = UUID.randomUUID().toString(), secreto = aleatorio();
    long fin = clock.instant().plusSeconds(recordar ? 604800 : 28800).getEpochSecond();
    sql.update(
        "insert into sesiones(id,usuario_id,renovacion_hash,expira_epoch,auth_version,recordar) values(?,?,?,?,?,?)",
        id,
        u.getId(),
        hash(secreto),
        fin,
        u.getAuthVersion(),
        recordar);
    return emitir(u, id, secreto, recordar);
  }

  @Transactional
  public Emitida renovar(String valor) {
    String[] partes = partes(valor);
    String id = partes[0];
    var filas =
        sql.queryForList(
            "select s.*,u.username from sesiones s with (updlock,rowlock) join usuarios u on u.id=s.usuario_id where s.id=?",
            id);
    if (filas.isEmpty()) throw invalida();
    var s = filas.getFirst();
    if (((Number) s.get("expira_epoch")).longValue() <= clock.instant().getEpochSecond()
        || !MessageDigest.isEqual(
            hash(partes[1]).getBytes(StandardCharsets.UTF_8),
            s.get("renovacion_hash").toString().getBytes(StandardCharsets.UTF_8))) throw invalida();
    var u =
        usuarios
            .acceso(s.get("username").toString())
            .filter(UsuarioRepository.Acceso::getActivo)
            .orElseThrow(Sesiones::invalida);
    if (u.getAuthVersion() != ((Number) s.get("auth_version")).longValue()) throw invalida();
    String nuevo = aleatorio();
    sql.update("update sesiones set renovacion_hash=? where id=?", hash(nuevo), id);
    return emitir(u, id, nuevo, (Boolean) s.get("recordar"));
  }

  private Emitida emitir(UsuarioRepository.Acceso u, String id, String secreto, boolean recordar) {
    var now = clock.instant();
    var claims =
        JwtClaimsSet.builder()
            .issuer("puntostock")
            .subject(u.getUsername())
            .claim("authVersion", u.getAuthVersion())
            .claim("sid", id)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .build();
    String token =
        jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new Emitida(
        new Acceso(token, 900, usuarios.rolesAcceso(u.getId()), u.getUsername()),
        id + "." + secreto,
        recordar);
  }

  public boolean vigente(String id, long usuario, long version) {
    return id != null
        && Boolean.TRUE.equals(
            sql.queryForObject(
                "select cast(case when exists(select 1 from sesiones where id=? and usuario_id=? and auth_version=? and expira_epoch>?) then 1 else 0 end as bit)",
                Boolean.class,
                id,
                usuario,
                version,
                clock.instant().getEpochSecond()));
  }

  public void salir(String valor) {
    if (valor != null) {
      try {
        sql.update(
            "delete from sesiones where id=? and renovacion_hash=?",
            partes(valor)[0],
            hash(partes(valor)[1]));
      } catch (ResponseStatusException ignored) {
      }
    }
  }

  @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3600000)
  public void limpiar() {
    sql.update("delete from sesiones where expira_epoch<=?", clock.instant().getEpochSecond());
  }

  private static String[] partes(String valor) {
    if (valor == null || !valor.matches("[a-fA-F0-9-]{36}\\.[A-Za-z0-9_-]{43}")) throw invalida();
    return valor.split("\\.", 2);
  }

  private static String aleatorio() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String valor) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static ResponseStatusException invalida() {
    return new ResponseStatusException(UNAUTHORIZED, "Sesión no válida. Inicia sesión nuevamente.");
  }
}
