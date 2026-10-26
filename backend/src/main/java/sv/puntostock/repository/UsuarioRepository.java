package sv.puntostock.repository;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import sv.puntostock.entity.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  interface Acceso {
    Long getId();

    String getUsername();

    String getPasswordHash();

    boolean getActivo();

    long getAuthVersion();
  }

  @Query(
      "select u.id as id,u.username as username,u.passwordHash as passwordHash,u.activo as activo,u.authVersion as authVersion from Usuario u where u.username=:username")
  Optional<Acceso> acceso(@Param("username") String username);

  @Query("select r.nombre from Usuario u join u.roles r where u.id=:id")
  List<String> rolesAcceso(@Param("id") Long id);

  interface Resumen {
    Long getId();

    String getUsername();

    String getNombre();

    String getCorreo();

    String getTelefono();

    boolean getActivo();

    LocalDateTime getFechaCreacion();

    String getRolesTexto();
  }

  @Query(
      value =
          "select u.id,u.username,coalesce(u.nombre,u.username) as nombre,u.correo,u.telefono,u.activo,u.fecha_creacion as fechaCreacion,(select string_agg(r.nombre,',') from usuario_roles ur join roles r on r.id=ur.rol_id where ur.usuario_id=u.id) as rolesTexto from usuarios u where (:buscar='' or u.username like concat('%',:buscar,'%') or u.nombre like concat('%',:buscar,'%') or u.correo like concat('%',:buscar,'%')) and (:activo is null or u.activo=:activo) and (:rol is null or exists(select 1 from usuario_roles ur join roles r on r.id=ur.rol_id where ur.usuario_id=u.id and r.nombre=:rol)) order by u.id",
      countQuery =
          "select count(*) from usuarios u where (:buscar='' or u.username like concat('%',:buscar,'%') or u.nombre like concat('%',:buscar,'%') or u.correo like concat('%',:buscar,'%')) and (:activo is null or u.activo=:activo) and (:rol is null or exists(select 1 from usuario_roles ur join roles r on r.id=ur.rol_id where ur.usuario_id=u.id and r.nombre=:rol))",
      nativeQuery = true)
  org.springframework.data.domain.Page<Resumen> resumen(
      @Param("buscar") String buscar,
      @Param("activo") Boolean activo,
      @Param("rol") String rol,
      org.springframework.data.domain.Pageable pagina);

  Optional<Usuario> findByUsername(String username);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from Usuario u where u.username=:username")
  Optional<Usuario> bloquear(@Param("username") String username);
}
