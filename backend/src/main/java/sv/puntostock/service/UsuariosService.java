package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos.UsuarioInput;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.*;

@Service
public class UsuariosService {
  public record Vista(
      Long id,
      String username,
      String nombre,
      String correo,
      String telefono,
      boolean activo,
      List<String> roles,
      OffsetDateTime fechaCreacion) {}

  public record Detalle(Vista usuario, String fotoPerfil) {}

  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PasswordEncoder passwords;

  public UsuariosService(
      UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwords) {
    this.usuarios = usuarios;
    this.roles = roles;
    this.passwords = passwords;
  }

  private Vista vista(Usuario u) {
    return new Vista(
        u.id,
        u.username,
        u.nombre == null ? u.username : u.nombre,
        u.correo,
        u.telefono,
        u.activo,
        u.roles.stream().map(r -> r.nombre).sorted().toList(),
        u.fechaCreacion == null ? null : u.fechaCreacion.atOffset(ZoneOffset.UTC));
  }

  private Vista vista(UsuarioRepository.Resumen u) {
    return new Vista(
        u.getId(),
        u.getUsername(),
        u.getNombre(),
        u.getCorreo(),
        u.getTelefono(),
        u.getActivo(),
        u.getRolesTexto() == null
            ? List.of()
            : Arrays.stream(u.getRolesTexto().split(",")).sorted().toList(),
        u.getFechaCreacion() == null ? null : u.getFechaCreacion().atOffset(ZoneOffset.UTC));
  }

  @Transactional(readOnly = true)
  public List<Vista> listar() {
    return usuarios
        .resumen("", null, null, org.springframework.data.domain.Pageable.unpaged())
        .map(this::vista)
        .getContent();
  }

  @Transactional(readOnly = true)
  public ConsultasService.Pagina<Vista> pagina(
      String buscar, Boolean activo, String rol, int pagina, int tamano) {
    var p = usuarios.resumen(buscar, activo, rol, ConsultasService.pagina(pagina, tamano));
    return new ConsultasService.Pagina<>(
        p.map(this::vista).getContent(), p.getTotalElements(), pagina, p.getTotalPages());
  }

  @Transactional(readOnly = true)
  public Detalle detalle(Long id) {
    var u = obtener(id);
    return new Detalle(vista(u), u.fotoPerfil);
  }

  private Usuario obtener(Long id) {
    return usuarios
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No se encontró el usuario."));
  }

  private void bloquear(String actor) {
    roles.bloquearAdministracion().orElseThrow();
    usuarios
        .bloquear(actor)
        .filter(u -> u.activo && u.roles.stream().anyMatch(r -> r.nombre.equals("ADMIN")))
        .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
  }

  @Transactional
  public Vista guardar(Long id, UsuarioInput in, String actor) {
    bloquear(actor);
    var u = id == null ? new Usuario() : obtener(id);
    String username = in.username().trim();
    if (username.isBlank())
      throw new ResponseStatusException(BAD_REQUEST, "El usuario es obligatorio.");
    usuarios
        .findByUsername(username)
        .filter(x -> !Objects.equals(x.id, id))
        .ifPresent(
            x -> {
              throw new ResponseStatusException(CONFLICT, "Ese usuario ya existe.");
            });
    boolean activo = in.activo() == null ? u.activo : in.activo();
    if (id != null
        && u.username.equals(actor)
        && (!activo || !in.rol().equals("ADMIN") || !username.equals(u.username)))
      throw new ResponseStatusException(
          CONFLICT,
          "No puedes desactivar tu cuenta, cambiar tu usuario ni quitar tu rol de administrador.");
    validarClave(in.password(), id == null);
    boolean cambioAcceso =
        id != null
            && (!username.equals(u.username)
                || u.activo != activo
                || !u.roles.stream().map(r -> r.nombre).toList().equals(List.of(in.rol())));
    if (in.password() != null && !in.password().isEmpty()) {
      u.passwordHash = passwords.encode(in.password());
      cambioAcceso = id != null;
      u.recuperacionHash = null;
      u.recuperacionExpira = null;
    }
    if (cambioAcceso) u.authVersion++;
    u.username = username;
    u.nombre = in.nombre() == null || in.nombre().isBlank() ? username : in.nombre().trim();
    Identidad.cambiarCorreo(u, in.correo());
    u.telefono = in.telefono() == null || in.telefono().isBlank() ? null : in.telefono().trim();
    u.activo = activo;
    u.roles.clear();
    u.roles.add(
        roles
            .findByNombre(in.rol())
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Rol inválido.")));
    return vista(usuarios.saveAndFlush(u));
  }

  @Transactional
  public void estado(Long id, boolean activo, String actor) {
    bloquear(actor);
    var u = obtener(id);
    if (!activo && u.username.equals(actor))
      throw new ResponseStatusException(CONFLICT, "No puedes desactivar tu propia cuenta.");
    if (u.activo != activo) {
      u.activo = activo;
      u.authVersion++;
      u.recuperacionHash = null;
      u.recuperacionExpira = null;
    }
  }

  public static void validarClave(String password, boolean crear) {
    if (!crear && (password == null || password.isEmpty())) return;
    if (password == null
        || password.isBlank()
        || password.length() < 12
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          BAD_REQUEST, "La contraseña requiere al menos 12 caracteres y como máximo 72 bytes.");
  }
}
