package sv.puntostock.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;

/** Lecturas paginadas; no altera el coordinador transaccional de venta. */
@Service
@Transactional(readOnly = true)
public class ConsultasService {
  public record Pagina<T>(List<T> contenido, long cantidad, int pagina, int paginas) {}

  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  public record ProductoVista(
      Long id,
      String sku,
      String nombre,
      String descripcion,
      BigDecimal precioVenta,
      int stock,
      String fotoUrl,
      String tipo,
      String categoria,
      String marca,
      String unidad,
      String ubicacion,
      int stockMinimo,
      BigDecimal precioCompra) {}

  public record MovimientoVista(
      Long id,
      Long productoId,
      String tipo,
      int cantidad,
      LocalDateTime fecha,
      String motivo,
      int stockAnterior,
      int stockPosterior) {}

  private final ProductoRepository productos;
  private final MovimientoRepository movimientos;

  public ConsultasService(ProductoRepository productos, MovimientoRepository movimientos) {
    this.productos = productos;
    this.movimientos = movimientos;
  }

  public static Pageable pagina(int pagina, int tamano) {
    if (pagina < 0 || tamano < 1 || tamano > 100)
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Página o tamaño inválido.");
    return PageRequest.of(pagina, tamano);
  }

  public static ProductoVista producto(Producto p, boolean admin) {
    return new ProductoVista(
        p.id,
        p.sku,
        p.nombre,
        p.descripcion,
        p.precioVenta,
        p.stock,
        p.fotoUrl,
        p.tipo,
        p.categoria,
        p.marca,
        p.unidad,
        p.ubicacion,
        p.stockMinimo,
        admin ? p.precioCompra : null);
  }

  public static MovimientoVista movimiento(Movimiento m) {
    return new MovimientoVista(
        m.id,
        m.producto.id,
        m.tipo,
        m.cantidad,
        m.fecha,
        m.motivo,
        m.stockAnterior,
        m.stockPosterior);
  }

  public List<ProductoVista> catalogo(boolean admin) {
    return productos.findByActivoTrueOrderByNombreAsc().stream()
        .map(p -> producto(p, admin))
        .toList();
  }

  public List<MovimientoVista> ultimosMovimientos() {
    return movimientos.findTop200ByOrderByFechaDesc().stream()
        .map(ConsultasService::movimiento)
        .toList();
  }

  public Pagina<ProductoVista> productos(
      String buscar, String tipo, String categoria, int pagina, int tamano, boolean admin) {
    var p = productos.buscar(buscar.trim(), tipo, categoria, pagina(pagina, tamano));
    return new Pagina<>(
        p.map(x -> producto(x, admin)).getContent(),
        p.getTotalElements(),
        pagina,
        p.getTotalPages());
  }

  public Pagina<MovimientoVista> movimientos(
      LocalDate desde, LocalDate hasta, Long producto, int pagina, int tamano) {
    if (desde != null && hasta != null && hasta.isBefore(desde))
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Rango de fechas inválido.");
    var p =
        movimientos.buscar(
            desde == null ? null : Tiempo.inicio(desde),
            hasta == null ? null : Tiempo.inicio(hasta.plusDays(1)),
            producto,
            pagina(pagina, tamano));
    return new Pagina<>(
        p.map(ConsultasService::movimiento).getContent(),
        p.getTotalElements(),
        pagina,
        p.getTotalPages());
  }
}
