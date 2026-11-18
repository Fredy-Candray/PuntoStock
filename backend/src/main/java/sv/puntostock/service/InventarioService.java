package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos.*;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;

@Service
@Transactional
public class InventarioService {
  private final ProductoRepository productos;
  private final MovimientoRepository movimientos;
  private final UsuarioRepository usuarios;
  private final Clock clock;

  public InventarioService(
      ProductoRepository productos,
      MovimientoRepository movimientos,
      UsuarioRepository usuarios,
      Clock clock) {
    this.productos = productos;
    this.movimientos = movimientos;
    this.usuarios = usuarios;
    this.clock = clock;
  }

  public Usuario actor(String username) {
    return usuarios
        .findByUsername(username)
        .filter(u -> u.activo)
        .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
  }

  private Producto bloquear(Long id) {
    return productos
        .bloquear(id)
        .filter(p -> p.activo)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no disponible"));
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void registrar(
      Producto p, Usuario u, String tipo, int cantidad, String motivo, Venta venta) {
    Movimiento m = new Movimiento();
    m.venta = venta;
    registrarMovimiento(m, p, u, tipo, cantidad, motivo);
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void consumirPedido(Producto p, Usuario u, int cantidad, long pedido, UUID solicitud) {
    Movimiento m = new Movimiento();
    m.pedidoId = pedido;
    m.solicitudConsumo = solicitud;
    m.nombreInsumo = p.nombre;
    m.unidadInsumo = p.unidad;
    m.costoUnitarioInsumo = p.precioCompra;
    registrarMovimiento(m, p, u, "CONSUMO_PEDIDO", -cantidad, "Pedido #" + pedido);
    movimientos.flush();
  }

  private void registrarMovimiento(
      Movimiento m, Producto p, Usuario u, String tipo, int cantidad, String motivo) {
    long nuevo = (long) p.stock + cantidad;
    if (nuevo < 0 || nuevo > Integer.MAX_VALUE)
      throw new ResponseStatusException(CONFLICT, "Stock insuficiente o fuera de rango: " + p.sku);
    m.producto = p;
    m.usuario = u;
    m.tipo = tipo;
    m.cantidad = cantidad;
    m.motivo = motivo;
    m.fecha = LocalDateTime.now(clock);
    m.stockAnterior = p.stock;
    m.stockPosterior = (int) nuevo;
    p.aplicarMovimiento(cantidad);
    movimientos.save(m);
  }

  public void mover(MovimientoInput in, String username) {
    boolean entrada = Set.of("COMPRA", "DEVOLUCION_CLIENTE").contains(in.tipo());
    boolean salida = Set.of("MERMA", "DEVOLUCION_PROVEEDOR").contains(in.tipo());
    if (in.cantidad() == 0
        || !(entrada || salida || in.tipo().equals("AJUSTE"))
        || (entrada && in.cantidad() < 0)
        || (salida && in.cantidad() > 0))
      throw new ResponseStatusException(BAD_REQUEST, "Tipo o signo de cantidad inválido");
    Usuario u = actor(username);
    Producto p = bloquear(in.productoId());
    if (p.tipo.equals("SERVICIO"))
      throw new ResponseStatusException(BAD_REQUEST, "Los servicios no llevan existencias.");
    String presentacion = in.presentacion() == null ? "UNIDAD" : in.presentacion();
    if (!Set.of("UNIDAD", "CAJA").contains(presentacion)
        || (presentacion.equals("CAJA") && p.unidadesPorCaja == null))
      throw new ResponseStatusException(BAD_REQUEST, "Presentación de inventario no disponible.");
    int factor = presentacion.equals("CAJA") ? p.unidadesPorCaja : 1;
    if (in.unidadesPorPresentacion() != null && in.unidadesPorPresentacion() != factor)
      throw new ResponseStatusException(
          CONFLICT, "Cambió el contenido de la caja. Actualiza el producto.");
    long cantidad = (long) in.cantidad() * factor;
    if (cantidad < Integer.MIN_VALUE || cantidad > Integer.MAX_VALUE)
      throw new ResponseStatusException(BAD_REQUEST, "Cantidad fuera de rango.");
    String motivo =
        presentacion.equals("CAJA")
            ? in.cantidad() + " caja(s) de " + factor + " unidades · " + in.motivo()
            : in.motivo();
    if (motivo.length() > 500)
      throw new ResponseStatusException(BAD_REQUEST, "Acorta el motivo del movimiento.");
    registrar(p, u, in.tipo(), (int) cantidad, motivo, null);
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void recibirCompra(
      long id, int cantidad, BigDecimal total, String motivo, String username) {
    var usuario = actor(username);
    var producto = bloquear(id);
    if (!producto.tipo.equals("PRODUCTO"))
      throw new ResponseStatusException(BAD_REQUEST, "Selecciona un artículo con existencias.");
    var valorExistente = producto.precioCompra.multiply(BigDecimal.valueOf(producto.stock));
    registrar(producto, usuario, "COMPRA", cantidad, motivo, null);
    producto.precioCompra =
        valorExistente
            .add(total)
            .divide(BigDecimal.valueOf(producto.stock), 2, RoundingMode.HALF_UP);
    if (producto.unidadesPorCaja != null)
      producto.precioCompraCaja =
          valorExistente
              .add(total)
              .multiply(BigDecimal.valueOf(producto.unidadesPorCaja))
              .divide(BigDecimal.valueOf(producto.stock), 2, RoundingMode.HALF_UP);
  }
}
