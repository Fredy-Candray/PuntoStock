package sv.puntostock.repository;
import sv.puntostock.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface MovimientoRepository extends JpaRepository<Movimiento,Long> { List<Movimiento> findTop200ByOrderByFechaDesc(); }
