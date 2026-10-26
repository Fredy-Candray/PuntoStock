package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
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
public class VentaService {
  private final Clock clock;
  private final ProductoRepository productos;
  private final VentaRepository ventas;
  private final UsuarioRepository usuarios;
  private final InventarioService inventario;

  public VentaService(
      ProductoRepository productos,
      VentaRepository ventas,
      UsuarioRepository usuarios,
      InventarioService inventario,
      Clock clock) {
    this.productos = productos;
    this.ventas = ventas;
    this.usuarios = usuarios;
    this.inventario = inventario;
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

  public VentaResultado vender(VentaInput in, String username) {
    Usuario u =
        usuarios
            .bloquear(username)
            .filter(x -> x.activo)
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    if (in.lineas() == null || in.lineas().isEmpty() || in.lineas().size() > 100)
      throw new ResponseStatusException(BAD_REQUEST, "La venta requiere entre 1 y 100 líneas.");
    Set<Long> ids = new HashSet<>();
    for (Linea l : in.lineas()) {
      if (l.productoId() == null || l.cantidad() < 1 || l.cantidad() > 100000)
        throw new ResponseStatusException(BAD_REQUEST, "Cantidad inválida.");
      if (!ids.add(l.productoId()))
        throw new ResponseStatusException(BAD_REQUEST, "Producto repetido");
    }
    String firma = firma(in);
    if (in.solicitudId() != null) {
      var anterior = ventas.findByUsuarioIdAndSolicitudId(u.id, in.solicitudId());
      if (anterior.isPresent()) {
        if (!firma.equals(anterior.get().firmaSolicitud))
          throw new ResponseStatusException(
              CONFLICT, "Esta solicitud ya fue usada para una venta diferente.");
        return ticket(anterior.get());
      }
    }
    Venta v = new Venta();
    v.usuario = u;
    v.fecha = LocalDateTime.now(clock);
    v.solicitudId = in.solicitudId();
    v.firmaSolicitud = firma;
    // Orden de bloqueo consistente para reducir deadlocks entre ventas.
    var lineas = in.lineas().stream().sorted(Comparator.comparing(Linea::productoId)).toList();
    Map<Long, Producto> bloqueados = new HashMap<>();
    for (Linea l : lineas) bloqueados.put(l.productoId(), bloquear(l.productoId()));
    for (Linea l : lineas) {
      Producto p = bloqueados.get(l.productoId());
      if (p.precioVenta.signum() <= 0)
        throw new ResponseStatusException(
            CONFLICT, "Configura un precio de venta mayor que cero para " + p.nombre);
      if (!p.tipo.equals("SERVICIO") && p.stock < l.cantidad())
        throw new ResponseStatusException(CONFLICT, "Stock insuficiente: " + p.sku);
      DetalleVenta d = new DetalleVenta();
      d.venta = v;
      d.producto = p;
      d.cantidad = l.cantidad();
      d.costoUnitario = p.precioCompra;
      d.precioUnitario = p.precioVenta;
      v.detalles.add(d);
      d.nombreProducto = p.nombre;
      d.skuProducto = p.sku;
      d.observaciones = limpio(l.observaciones());
      BigDecimal cantidad = BigDecimal.valueOf(l.cantidad());
      v.total = v.total.add(p.precioVenta.multiply(cantidad));
      v.ganancia = v.ganancia.add(p.precioVenta.subtract(p.precioCompra).multiply(cantidad));
    }
    if (v.total.precision() - v.total.scale() > 17)
      throw new ResponseStatusException(BAD_REQUEST, "El importe supera el límite permitido.");
    if (in.totalEsperado() != null && in.totalEsperado().compareTo(v.total) != 0)
      throw new ResponseStatusException(
          CONFLICT, "Los precios cambiaron. Revisa el total actualizado antes de cobrar.");
    v.metodoPago = in.metodoPago() == null ? "EFECTIVO" : in.metodoPago();
    if (!Set.of("EFECTIVO", "TARJETA", "TRANSFERENCIA").contains(v.metodoPago))
      throw new ResponseStatusException(BAD_REQUEST, "Medio de pago inválido.");
    v.recibido = in.recibido() == null ? v.total : in.recibido();
    if (v.recibido.compareTo(v.total) < 0)
      throw new ResponseStatusException(BAD_REQUEST, "El importe recibido no cubre el total.");
    if (!v.metodoPago.equals("EFECTIVO") && v.recibido.compareTo(v.total) != 0)
      throw new ResponseStatusException(
          BAD_REQUEST, "Solo el pago en efectivo puede generar cambio.");
    v.cambio = v.recibido.subtract(v.total);
    v.cliente = limpio(in.cliente());
    v.referenciaPago = limpio(in.referenciaPago());
    ventas.save(v);
    for (Linea l : lineas) {
      Producto p = bloqueados.get(l.productoId());
      if (!p.tipo.equals("SERVICIO"))
        inventario.registrar(p, u, "VENTA", -l.cantidad(), "Venta #" + v.id, v);
    }
    return ticket(v);
  }

  private static String limpio(String value) {
    return value == null ? "" : value.trim();
  }

  private static String firma(VentaInput in) {
    var parts = new ArrayList<String>();
    in.lineas().stream()
        .sorted(Comparator.comparing(Linea::productoId))
        .forEach(
            l -> {
              parts.add(l.productoId().toString());
              parts.add(Integer.toString(l.cantidad()));
              parts.add(limpio(l.observaciones()));
            });
    parts.add(in.metodoPago() == null ? "EFECTIVO" : in.metodoPago());
    parts.add(in.recibido() == null ? "" : in.recibido().stripTrailingZeros().toPlainString());
    parts.add(
        in.totalEsperado() == null ? "" : in.totalEsperado().stripTrailingZeros().toPlainString());
    parts.add(limpio(in.cliente()));
    parts.add(limpio(in.referenciaPago()));
    String canonical =
        parts.stream()
            .map(s -> s.length() + ":" + s)
            .collect(java.util.stream.Collectors.joining());
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private VentaResultado ticket(Venta v) {
    return new VentaResultado(
        v.id,
        v.total,
        v.fecha.atOffset(ZoneOffset.UTC),
        v.usuario.username,
        v.metodoPago,
        v.recibido,
        v.cambio,
        v.cliente,
        v.referenciaPago,
        v.detalles.stream()
            .map(
                d ->
                    new TicketLinea(
                        d.producto.id,
                        d.skuProducto == null ? d.producto.sku : d.skuProducto,
                        d.nombreProducto == null ? d.producto.nombre : d.nombreProducto,
                        d.cantidad,
                        d.precioUnitario,
                        d.precioUnitario.multiply(BigDecimal.valueOf(d.cantidad)),
                        d.observaciones))
            .toList());
  }

  @Transactional(readOnly = true)
  public VentaResultado comprobante(Long id, String username) {
    var u = actor(username);
    var v = ventas.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    if (!v.usuario.id.equals(u.id) && u.roles.stream().noneMatch(r -> r.nombre.equals("ADMIN")))
      throw new ResponseStatusException(NOT_FOUND);
    return ticket(v);
  }

  public VentaResultado solicitud(UUID id, String username) {
    var u =
        usuarios
            .bloquear(username)
            .filter(x -> x.activo)
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    return ticket(
        ventas
            .findByUsuarioIdAndSolicitudId(u.id, id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
  }

  @Transactional(readOnly = true)
  public HistorialVentas historial(LocalDate dia, int pagina, String username) {
    if (pagina < 0) throw new ResponseStatusException(BAD_REQUEST, "Página inválida.");
    var u = actor(username);
    Long usuarioId = u.roles.stream().anyMatch(r -> r.nombre.equals("ADMIN")) ? null : u.id;
    ZoneId zona = Tiempo.ZONA;
    var desde = LocalDateTime.ofInstant(dia.atStartOfDay(zona).toInstant(), ZoneOffset.UTC);
    var hasta = desde.plusDays(1);
    var page =
        ventas.historial(
            desde, hasta, usuarioId, org.springframework.data.domain.PageRequest.of(pagina, 20));
    var lista =
        page.getContent().stream()
            .map(
                v ->
                    new VentaResumen(
                        v.id,
                        v.fecha.atOffset(ZoneOffset.UTC),
                        v.usuario.username,
                        v.total,
                        v.metodoPago,
                        v.cliente))
            .toList();
    Map<String, BigDecimal> totales = new HashMap<>();
    ventas.totales(desde, hasta, usuarioId).forEach(t -> totales.put(t.getMetodo(), t.getTotal()));
    return new HistorialVentas(
        lista,
        page.getTotalElements(),
        pagina,
        page.getTotalPages(),
        totales.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
        totales.getOrDefault("EFECTIVO", BigDecimal.ZERO),
        totales.getOrDefault("TARJETA", BigDecimal.ZERO),
        totales.getOrDefault("TRANSFERENCIA", BigDecimal.ZERO),
        totales.getOrDefault("SIN_REGISTRO", BigDecimal.ZERO));
  }
}
