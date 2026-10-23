package sv.puntostock.repository;
import sv.puntostock.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface RolRepository extends JpaRepository<Rol,Long> {
 Optional<Rol> findByNombre(String nombre);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select r from Rol r where r.nombre='ADMIN'")
 Optional<Rol> bloquearAdministracion();
}
