package sv.puntostock.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import sv.puntostock.entity.*;

public interface VentaRepository extends JpaRepository<Venta, Long> {
  @Query("select coalesce(sum(v.ganancia),0) from Venta v where v.fecha>=:desde and v.fecha<:hasta")
  BigDecimal ganancia(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

  Optional<Venta> findByUsuarioIdAndSolicitudId(Long usuarioId, UUID solicitudId);

  @EntityGraph(attributePaths = {"usuario"})
  @Query(
      "select v from Venta v where v.fecha>=:desde and v.fecha<:hasta and (:usuarioId is null or v.usuario.id=:usuarioId) order by v.fecha desc,v.id desc")
  org.springframework.data.domain.Page<Venta> historial(
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta,
      @Param("usuarioId") Long usuarioId,
      org.springframework.data.domain.Pageable pageable);

  @Query(
      "select coalesce(sum(v.total),0) from Venta v where v.fecha>=:desde and v.fecha<:hasta and (:usuarioId is null or v.usuario.id=:usuarioId) and (:metodo is null or v.metodoPago=:metodo)")
  BigDecimal totalVendido(
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta,
      @Param("usuarioId") Long usuarioId,
      @Param("metodo") String metodo);

  interface TotalMedio {
    String getMetodo();

    BigDecimal getTotal();
  }

  @Query(
      "select v.metodoPago as metodo,sum(v.total) as total from Venta v where v.fecha>=:desde and v.fecha<:hasta and (:usuarioId is null or v.usuario.id=:usuarioId) group by v.metodoPago")
  List<TotalMedio> totales(
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta,
      @Param("usuarioId") Long usuarioId);
}
