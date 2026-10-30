package sv.puntostock.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import sv.puntostock.entity.*;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
  List<Producto> findByActivoTrueOrderByNombreAsc();

  boolean existsBySku(String sku);

  boolean existsByIdAndActivoTrue(Long id);

  @Query(
      "select p from Producto p where p.activo=true and (:buscar='' or lower(p.nombre) like lower(concat('%',:buscar,'%')) or lower(p.sku) like lower(concat('%',:buscar,'%')) or lower(p.marca) like lower(concat('%',:buscar,'%'))) and (:tipo is null or p.tipo=:tipo) and (:categoria is null or p.categoria=:categoria) order by p.nombre,p.id")
  org.springframework.data.domain.Page<Producto> buscar(
      @Param("buscar") String buscar,
      @Param("tipo") String tipo,
      @Param("categoria") String categoria,
      org.springframework.data.domain.Pageable pagina);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Producto p where p.id=:id")
  Optional<Producto> bloquear(@Param("id") Long id);
}
