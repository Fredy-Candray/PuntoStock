package sv.puntostock.repository;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import sv.puntostock.entity.*;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
  List<Movimiento> findTop200ByOrderByFechaDesc();

  @Query(
      "select m from Movimiento m where (:desde is null or m.fecha>=:desde) and (:hasta is null or m.fecha<:hasta) and (:producto is null or m.producto.id=:producto) order by m.fecha desc,m.id desc")
  org.springframework.data.domain.Page<Movimiento> buscar(
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta,
      @Param("producto") Long producto,
      org.springframework.data.domain.Pageable pagina);
}
