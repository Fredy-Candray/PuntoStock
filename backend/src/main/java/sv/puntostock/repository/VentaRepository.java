package sv.puntostock.repository;
import sv.puntostock.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface VentaRepository extends JpaRepository<Venta,Long> { @Query("select coalesce(sum(v.ganancia),0) from Venta v where v.fecha>=:desde and v.fecha<:hasta")
 BigDecimal ganancia(@Param("desde") LocalDateTime desde,@Param("hasta") LocalDateTime hasta); }
