package sv.puntostock.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import sv.puntostock.entity.*;

public interface RolRepository extends JpaRepository<Rol, Long> {
  Optional<Rol> findByNombre(String nombre);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Rol r where r.nombre='ADMIN'")
  Optional<Rol> bloquearAdministracion();
}
