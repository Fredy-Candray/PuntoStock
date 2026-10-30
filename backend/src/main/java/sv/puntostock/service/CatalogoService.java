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
  private final InventarioService inventario;
  private final CategoriasProductoService categorias;
  private final ProductoImagenService imagenes;

  public CatalogoService(
      ProductoRepository productos,
      InventarioService inventario,
      CategoriasProductoService categorias,
      ProductoImagenService imagenes) {
    this.productos = productos;
    this.inventario = inventario;
    this.categorias = categorias;
    this.imagenes = imagenes;
  }

  private Producto bloquear(Long id) {
    return productos
        .bloquear(id)
        .filter(p -> p.activo)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no disponible"));
  }

  public Producto guardar(Long id, ProductoInput in, String username) {
    boolean automatico = id == null && (in.sku() == null || in.sku().isBlank());
    if (id != null && (in.sku() == null || in.sku().isBlank()))
      throw new ResponseStatusException(
          BAD_REQUEST, "El código del producto no puede quedar vacío al editar.");
    int inicial = in.existenciasIniciales() == null ? 0 : in.existenciasIniciales();
    if (inicial < 0 || inicial > 1000000)
      throw new ResponseStatusException(BAD_REQUEST, "Existencias iniciales fuera de rango.");
    if (id != null && inicial != 0)
      throw new ResponseStatusException(
          BAD_REQUEST, "Las existencias iniciales solo se indican al crear el producto.");
    Producto p = id == null ? new Producto() : bloquear(id);
    String fotoAnterior = p.fotoUrl;
    String nuevaImagen = in.imagen();
    boolean quitarImagen = Boolean.TRUE.equals(in.quitarImagen());
    if (nuevaImagen != null && (quitarImagen || FotoPerfilValidator.validar(nuevaImagen) == null))
      throw new ResponseStatusException(
          BAD_REQUEST, "Selecciona una imagen o elige quitarla, no ambas opciones.");
    if (in.caja() != null) {
      var c = in.caja();
      if (c.unidades() == null
          || c.unidades() < 2
          || c.unidades() > 1000000
          || c.precioCompra() == null
          || c.precioCompra().signum() < 0
          || (c.precioVenta() != null && c.precioVenta().signum() <= 0))
        throw new ResponseStatusException(BAD_REQUEST, "Revisa las unidades y precios de la caja.");
      int cajas = c.cajasIniciales() == null ? 0 : c.cajasIniciales();
      long total = (long) inicial + (long) cajas * c.unidades();
      if (cajas < 0 || cajas > 1000000 || total > 1000000 || (id != null && cajas != 0))
        throw new ResponseStatusException(BAD_REQUEST, "Revisa las cajas y unidades iniciales.");
      if (!"PRODUCTO".equals(in.tipo() == null ? "PRODUCTO" : in.tipo())
          || !"UNIDAD".equals(in.unidad() == null ? "UNIDAD" : in.unidad()))
        throw new ResponseStatusException(
            BAD_REQUEST, "Las cajas desglosadas se venden por unidades individuales.");
      if (id != null && p.stock > 0 && !"UNIDAD".equals(p.unidad))
        throw new ResponseStatusException(
            CONFLICT, "Deja las existencias en cero antes de cambiar la unidad de inventario.");
      inicial = (int) total;
      p.unidadesPorCaja = c.unidades();
      p.precioCompraCaja = c.precioCompra();
      p.precioVentaCaja = c.precioVenta();
    } else {
      p.unidadesPorCaja = null;
      p.precioCompraCaja = null;
      p.precioVentaCaja = null;
    }
    String tipo = in.tipo() == null ? "PRODUCTO" : Tipos.TipoProducto.valueOf(in.tipo()).name();
    if (tipo.equals("SERVICIO") && inicial != 0)
      throw new ResponseStatusException(BAD_REQUEST, "Los servicios no llevan existencias.");
    if (tipo.equals("SERVICIO") && p.stock != 0)
      throw new ResponseStatusException(
          CONFLICT, "Deja las existencias en cero antes de convertir un artículo en servicio.");
    p.sku = automatico ? "__PS_AUTO_" + java.util.UUID.randomUUID() : in.sku().trim();
    p.nombre = in.nombre().trim();
    p.descripcion = in.descripcion().trim();
    p.precioCompra =
        in.caja() == null
            ? in.precioCompra()
            : in.caja()
                .precioCompra()
                .divide(
                    java.math.BigDecimal.valueOf(in.caja().unidades()),
                    2,
                    java.math.RoundingMode.HALF_UP);
    p.precioVenta = in.precioVenta();
    String rutaImagen = in.fotoUrl() == null && id != null ? fotoAnterior : in.fotoUrl();
    p.fotoUrl =
        nuevaImagen != null || quitarImagen || rutaImagen == null || rutaImagen.isBlank()
            ? "/assets/producto.svg"
            : rutaImagen.trim();
    boolean propia =
        id != null
            && p.fotoUrl.equals(fotoAnterior)
            && p.fotoUrl.startsWith("/api/productos/" + id + "/imagen?");
    if (!propia && !p.fotoUrl.startsWith("/assets/") && !p.fotoUrl.matches("https://[^\\s]+"))
      throw new ResponseStatusException(
          BAD_REQUEST, "La imagen debe usar HTTPS o una ruta de recursos del sistema.");
    p.tipo = tipo;
    p.categoria = categorias.paraProducto(in.categoria(), id == null ? null : p.categoria);
    p.marca = limpio(in.marca());
    p.ubicacion = limpio(in.ubicacion());
    p.unidad = in.unidad() == null ? (tipo.equals("SERVICIO") ? "TRABAJO" : "UNIDAD") : in.unidad();
    p.stockMinimo = tipo.equals("SERVICIO") ? 0 : (in.stockMinimo() == null ? 5 : in.stockMinimo());
    productos.save(p);
    if (nuevaImagen != null) p.fotoUrl = imagenes.guardar(p.id, nuevaImagen);
    else if (id != null && (quitarImagen || !java.util.Objects.equals(fotoAnterior, p.fotoUrl)))
      imagenes.quitar(p.id);
    if (automatico) {
      String base = CodigoProducto.crear(p.tipo, p.categoria, p.nombre, p.id);
      String codigo = base;
      int intento = 1;
      while (productos.existsBySku(codigo) && intento < 100) codigo = base + "-" + (++intento);
      if (productos.existsBySku(codigo))
        throw new ResponseStatusException(
            CONFLICT, "No se pudo asignar un código. Inténtalo nuevamente.");
      p.sku = codigo;
    }
    if (inicial > 0)
      inventario.registrar(
          p,
          inventario.actor(username),
          "AJUSTE",
          inicial,
          "Existencias iniciales al crear el producto",
          null);
    return p;
  }

  public void eliminar(Long id) {
    bloquear(id).activo = false;
  }

  private static String limpio(String s) {
    return s == null ? "" : s.trim();
  }
}
