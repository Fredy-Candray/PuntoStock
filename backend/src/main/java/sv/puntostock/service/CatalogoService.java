package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos.ProductoInput;
import sv.puntostock.entity.Producto;
import sv.puntostock.repository.ProductoRepository;

@Service
@Transactional
public class CatalogoService {
  private final ProductoRepository productos;

  public CatalogoService(ProductoRepository productos) {
    this.productos = productos;
  }

  private Producto bloquear(Long id) {
    return productos
        .bloquear(id)
        .filter(p -> p.activo)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no disponible"));
  }

  public Producto guardar(Long id, ProductoInput in) {
    Producto p = id == null ? new Producto() : bloquear(id);
    String tipo = in.tipo() == null ? "PRODUCTO" : Tipos.TipoProducto.valueOf(in.tipo()).name();
    if (tipo.equals("SERVICIO") && p.stock != 0)
      throw new ResponseStatusException(
          CONFLICT, "Deja las existencias en cero antes de convertir un artículo en servicio.");
    p.sku = in.sku().trim();
    p.nombre = in.nombre().trim();
    p.descripcion = in.descripcion().trim();
    p.precioCompra = in.precioCompra();
    p.precioVenta = in.precioVenta();
    p.fotoUrl =
        in.fotoUrl() == null || in.fotoUrl().isBlank()
            ? "/assets/producto.svg"
            : in.fotoUrl().trim();
    if (!p.fotoUrl.startsWith("/assets/") && !p.fotoUrl.matches("https://[^\\s]+"))
      throw new ResponseStatusException(
          BAD_REQUEST, "La imagen debe usar HTTPS o una ruta de recursos del sistema.");
    p.tipo = tipo;
    p.categoria =
        in.categoria() == null || in.categoria().isBlank() ? "General" : in.categoria().trim();
    p.marca = limpio(in.marca());
    p.ubicacion = limpio(in.ubicacion());
    p.unidad = in.unidad() == null ? (tipo.equals("SERVICIO") ? "TRABAJO" : "UNIDAD") : in.unidad();
    p.stockMinimo = tipo.equals("SERVICIO") ? 0 : (in.stockMinimo() == null ? 5 : in.stockMinimo());
    return productos.save(p);
  }

  public void eliminar(Long id) {
    bloquear(id).activo = false;
  }

  private static String limpio(String s) {
    return s == null ? "" : s.trim();
  }
}
