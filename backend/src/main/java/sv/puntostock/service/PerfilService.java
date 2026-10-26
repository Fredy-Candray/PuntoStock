package sv.puntostock.service;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.UsuarioRepository;

@Service
@Transactional
public class PerfilService {
  private final UsuarioRepository usuarios;

  public PerfilService(UsuarioRepository usuarios) {
    this.usuarios = usuarios;
  }

  public record Perfil(
      Long id,
      String username,
      String nombre,
      String correo,
      String telefono,
      List<String> roles,
      boolean activo,
      OffsetDateTime fechaCreacion,
      String fotoPerfil,
      String fotoPortada) {}

  private Usuario actual(String username) {
    return usuarios
        .findByUsername(username)
        .filter(u -> u.activo)
        .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
  }

  private Perfil respuesta(Usuario u) {
    return new Perfil(
        u.id,
        u.username,
        u.nombre == null ? u.username : u.nombre,
        u.correo,
        u.telefono,
        u.roles.stream().map(r -> r.nombre).sorted().toList(),
        u.activo,
        u.fechaCreacion == null ? null : u.fechaCreacion.atOffset(ZoneOffset.UTC),
        u.fotoPerfil,
        u.fotoPortada);
  }

  @Transactional(readOnly = true)
  public Perfil obtener(String username) {
    return respuesta(actual(username));
  }

  public Perfil actualizar(Contratos.PerfilInput in, String username) {
    var u = actual(username);
    u.nombre = in.nombre().trim();
    sv.puntostock.service.Identidad.cambiarCorreo(u, in.correo());
    u.telefono = in.telefono() == null || in.telefono().isBlank() ? null : in.telefono().trim();
    return respuesta(u);
  }

  public Perfil foto(Contratos.FotoInput in, String username) {
    var u = actual(username);
    u.fotoPerfil = FotoPerfilValidator.validar(in.imagen());
    return respuesta(u);
  }

  public Perfil portada(Contratos.FotoInput in, String username) {
    var u = actual(username);
    u.fotoPortada = FotoPerfilValidator.validar(in.imagen());
    return respuesta(u);
  }
}
