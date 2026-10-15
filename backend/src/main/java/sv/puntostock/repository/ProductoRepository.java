package sv.puntostock.repository;
import sv.puntostock.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface ProductoRepository extends JpaRepository<Producto,Long> { List<Producto> findByActivoTrueOrderByNombreAsc();
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from Producto p where p.id=:id") Optional<Producto> bloquear(@Param("id") Long id); }
