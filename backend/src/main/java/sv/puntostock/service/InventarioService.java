package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

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
    long nuevo = (long) p.stock + cantidad;
    if (nuevo < 0 || nuevo > Integer.MAX_VALUE)
      throw new ResponseStatusException(CONFLICT, "Stock insuficiente o fuera de rango: " + p.sku);
    Movimiento m = new Movimiento();
    m.producto = p;
    m.usuario = u;
    m.tipo = tipo;
    m.cantidad = cantidad;
    m.motivo = motivo;
    m.venta = venta;
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
    registrar(p, u, in.tipo(), in.cantidad(), in.motivo(), null);
  }
}
