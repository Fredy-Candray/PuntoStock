package sv.puntostock.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import sv.puntostock.entity.CategoriaProducto;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {
  List<CategoriaProducto> findAllByOrderByNombreAsc();

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CategoriaProducto c where c.nombre=:nombre")
  Optional<CategoriaProducto> bloquearNombre(@Param("nombre") String nombre);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CategoriaProducto c where c.id=:id")
  Optional<CategoriaProducto> bloquear(@Param("id") Long id);
}
