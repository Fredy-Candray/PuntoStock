package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.PedidoContratos.*;
import sv.puntostock.repository.ProductoRepository;
import sv.puntostock.service.ConsultasService.Pagina;
import sv.puntostock.service.Tipos.MedioPago;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class PedidoService {
  private static final int TAMANO = 6;
  private static final String RESUMEN =
      """
      SELECT p.*,p.total-COALESCE(a.pagado,0) AS saldo
      FROM pedidos p
      OUTER APPLY (SELECT CAST(SUM(monto) AS decimal(19,2)) pagado
        FROM pedido_abonos WHERE pedido_id=p.id) a
      """;

  private final JdbcTemplate sql;
  private final CajaService caja;
  private final InventarioService inventario;
  private final ProductoRepository productos;
  private final ObjectMapper mapper;
  private final Clock clock;

  public PedidoService(
      JdbcTemplate sql,
      CajaService caja,
      InventarioService inventario,
      ProductoRepository productos,
      ObjectMapper mapper,
      Clock clock) {
    this.sql = sql;
    this.caja = caja;
    this.inventario = inventario;
    this.productos = productos;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Pagina<Resumen> listar(int pagina) {
    ConsultasService.pagina(pagina, TAMANO);
    long total = sql.queryForObject("SELECT COUNT_BIG(*) FROM pedidos", Long.class);
    var lista =
        sql.query(
            RESUMEN + " ORDER BY p.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            this::resumen,
            (long) pagina * TAMANO,
            TAMANO);
    return new Pagina<>(lista, total, pagina, (int) ((total + TAMANO - 1) / TAMANO));
  }

  private Resumen resumen(ResultSet r, int n) throws SQLException {
    return new Resumen(
        r.getLong("id"),
        r.getString("cliente"),
        r.getString("descripcion"),
        r.getString("servicios"),
        r.getInt("cantidad"),
        r.getBigDecimal("precio_unitario"),
        r.getBigDecimal("total"),
        r.getBigDecimal("saldo"),
        r.getDate("entrega").toLocalDate(),
        Estado.valueOf(r.getString("estado")));
  }

  private static OffsetDateTime fecha(ResultSet r, String columna) throws SQLException {
    return r.getTimestamp(columna).toLocalDateTime().atOffset(ZoneOffset.UTC);
  }

  private void bloquear(long id) {
    var encontrados =
        sql.query(
            "SELECT id FROM pedidos WITH (UPDLOCK,HOLDLOCK) WHERE id=?",
            (r, n) -> r.getLong(1),
            id);
    if (encontrados.isEmpty())
      throw new ResponseStatusException(NOT_FOUND, "Pedido no encontrado.");
  }

  public Detalle detalle(long id) {
    bloquear(id);
    return leer(id);
  }

  private Resumen consultar(long id) {
    return sql.query(RESUMEN + " WHERE p.id=?", this::resumen, id).getFirst();
  }

  private Detalle leer(long id) {
    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
    boolean verCostos =
        autenticacion != null
            && autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    var materiales =
        sql.query(
            """
            SELECT id,nombre_insumo,unidad_insumo,cantidad,fecha,costo_unitario_insumo
            FROM movimientos_inventario WHERE pedido_id=? ORDER BY id DESC
            """,
            (r, n) -> {
              int cantidad = -r.getInt("cantidad");
              var costo = verCostos ? r.getBigDecimal("costo_unitario_insumo") : null;
              return new Consumo(
                  r.getLong("id"),
                  r.getString("nombre_insumo"),
                  r.getString("unidad_insumo"),
                  cantidad,
                  fecha(r, "fecha"),
                  costo,
                  costo == null ? null : costo.multiply(BigDecimal.valueOf(cantidad)));
            },
            id);
    var abonos =
        sql.query(
            "SELECT * FROM pedido_abonos WHERE pedido_id=? ORDER BY id DESC",
            (r, n) ->
                new Abono(
                    r.getLong("id"),
                    fecha(r, "fecha"),
                    r.getBigDecimal("monto"),
                    r.getBigDecimal("saldo"),
                    MedioPago.valueOf(r.getString("metodo_pago"))),
            id);
    var total =
        verCostos
            ? materiales.stream()
                .map(Consumo::costoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            : null;
    return new Detalle(consultar(id), abonos, materiales, total);
  }

  public Detalle crear(Crear in, String username) {
    long actor = inventario.actor(username).id;
    caja.bloquear();
    String solicitud = in.anticipo().solicitudId().toString();
    String huella = huella(in);
    var anterior =
        sql.query(
            "SELECT id,usuario_id,huella FROM pedidos WHERE solicitud_id=?",
            (r, n) ->
                new Object[] {r.getLong("id"), r.getLong("usuario_id"), r.getString("huella")},
            solicitud);
    if (!anterior.isEmpty()) {
      var fila = anterior.getFirst();
      if (!fila[1].equals(actor) || !fila[2].equals(huella)) throw solicitudUsada();
      return detalle((Long) fila[0]);
    }
    var total = in.precioUnitario().multiply(BigDecimal.valueOf(in.cantidad()));
    if (total.compareTo(new BigDecimal("999999999.99")) > 0)
      throw new ResponseStatusException(
          BAD_REQUEST, "El total del pedido supera el límite permitido.");
    validarMonto(in.anticipo().monto(), total);
    long cajaId = caja.paraVenta();
    long id =
        sql.queryForObject(
            """
        INSERT INTO pedidos(solicitud_id,huella,cliente,descripcion,servicios,cantidad,
           precio_unitario,total,entrega,estado,fecha,usuario_id)
        OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,?,?,'PENDIENTE',?,?)
        """,
            Long.class,
            solicitud,
            huella,
            in.cliente().strip(),
            in.descripcion().strip(),
            in.servicios().strip(),
            in.cantidad(),
            in.precioUnitario(),
            total,
            in.entrega(),
            LocalDateTime.now(clock),
            actor);
    registrarAbono(id, cajaId, actor, in.anticipo(), total);
    return leer(id);
  }

  private String huella(Crear in) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(in)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private ResponseStatusException solicitudUsada() {
    return new ResponseStatusException(CONFLICT, "La solicitud ya se utilizó para otra operación.");
  }

  public Detalle abonar(long id, Abonar in, String username) {
    long actor = inventario.actor(username).id;
    caja.bloquear();
    bloquear(id);
    var anterior =
        sql.query(
            "SELECT pedido_id,usuario_id,monto,metodo_pago FROM pedido_abonos WHERE solicitud_id=?",
            (r, n) -> new Object[] {r.getLong(1), r.getLong(2), r.getBigDecimal(3), r.getString(4)},
            in.solicitudId().toString());
    if (!anterior.isEmpty()) {
      var fila = anterior.getFirst();
      if (!fila[0].equals(id)
          || !fila[1].equals(actor)
          || ((BigDecimal) fila[2]).compareTo(in.monto()) != 0
          || !fila[3].equals(in.metodoPago().name())) throw solicitudUsada();
      return leer(id);
    }
    var pedido = consultar(id);
    if (pedido.estado() == Estado.CANCELADO)
      throw new ResponseStatusException(CONFLICT, "El pedido está cancelado.");
    var saldo = pedido.saldo();
    validarMonto(in.monto(), saldo);
    registrarAbono(id, caja.paraVenta(), actor, in, saldo);
    return leer(id);
  }

  private void validarMonto(BigDecimal monto, BigDecimal saldo) {
    if (monto.compareTo(saldo) > 0)
      throw new ResponseStatusException(CONFLICT, "El monto supera el saldo del pedido.");
  }

  private void registrarAbono(long id, long cajaId, long actor, Abonar in, BigDecimal saldo) {
    sql.update(
        """
        INSERT INTO pedido_abonos(pedido_id,caja_id,usuario_id,solicitud_id,fecha,monto,saldo,metodo_pago)
        VALUES(?,?,?,?,?,?,?,?)
        """,
        id,
        cajaId,
        actor,
        in.solicitudId().toString(),
        LocalDateTime.now(clock),
        in.monto(),
        saldo.subtract(in.monto()),
        in.metodoPago().name());
  }

  public Detalle consumir(long id, Consumir in, String username) {
    var actor = inventario.actor(username);
    bloquear(id);
    var anterior =
        sql.query(
            """
        SELECT producto_id,usuario_id,cantidad FROM movimientos_inventario
        WHERE pedido_id=? AND solicitud_consumo=?
        """,
            (r, n) -> new long[] {r.getLong(1), r.getLong(2), r.getLong(3)},
            id,
            in.solicitudId().toString());
    if (!anterior.isEmpty()) {
      var fila = anterior.getFirst();
      if (fila[0] != in.productoId() || fila[1] != actor.id || fila[2] != -in.cantidad())
        throw solicitudUsada();
      return leer(id);
    }
    var actual = consultar(id).estado();
    if (actual == Estado.CANCELADO)
      throw new ResponseStatusException(CONFLICT, "El pedido está cancelado.");
    if (actual == Estado.ENTREGADO)
      throw new ResponseStatusException(CONFLICT, "El pedido ya fue entregado.");
    var producto =
        productos
            .bloquear(in.productoId())
            .filter(p -> p.activo && p.tipo.equals("PRODUCTO"))
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Material no disponible."));
    inventario.consumirPedido(producto, actor, in.cantidad(), id, in.solicitudId());
    return leer(id);
  }

  public Detalle editar(long id, Editar in) {
    bloquear(id);
    var pedido = consultar(id);
    if (pedido.estado() != Estado.PENDIENTE)
      throw new ResponseStatusException(CONFLICT, "Solo se pueden editar pedidos pendientes.");
    var total = in.precioUnitario().multiply(BigDecimal.valueOf(in.cantidad()));
    if (total.compareTo(new BigDecimal("999999999.99")) > 0)
      throw new ResponseStatusException(
          BAD_REQUEST, "El total del pedido supera el límite permitido.");
    if (total.compareTo(pedido.total().subtract(pedido.saldo())) < 0)
      throw new ResponseStatusException(CONFLICT, "El total no puede ser menor que lo ya abonado.");
    sql.update(
        """
        UPDATE pedidos SET cliente=?,descripcion=?,servicios=?,cantidad=?,
          precio_unitario=?,total=?,entrega=? WHERE id=?
        """,
        in.cliente().strip(),
        in.descripcion().strip(),
        in.servicios().strip(),
        in.cantidad(),
        in.precioUnitario(),
        total,
        in.entrega(),
        id);
    return leer(id);
  }

  public Detalle estado(long id, CambiarEstado in) {
    bloquear(id);
    var nuevo = in.estado();
    var actual = consultar(id).estado();
    if (actual == nuevo) return leer(id);
    if (actual == Estado.ENTREGADO || actual == Estado.CANCELADO || nuevo == Estado.PENDIENTE)
      throw new ResponseStatusException(CONFLICT, "El estado del pedido no admite ese cambio.");
    if (nuevo == Estado.CANCELADO) {
      var abono =
          sql.queryForObject("SELECT MAX(id) FROM pedido_abonos WHERE pedido_id=?", Long.class, id);
      var consumo =
          sql.queryForObject(
              "SELECT MAX(id) FROM movimientos_inventario WHERE pedido_id=?", Long.class, id);
      if (!Objects.equals(abono, in.ultimoAbono()) || !Objects.equals(consumo, in.ultimoConsumo()))
        throw new ResponseStatusException(
            CONFLICT, "Los abonos o materiales cambiaron. Revisa el pedido antes de cancelarlo.");
    }
    sql.update(
        "UPDATE pedidos SET estado=?,fecha_entregado=? WHERE id=?",
        nuevo.name(),
        nuevo == Estado.ENTREGADO ? LocalDateTime.now(clock) : null,
        id);
    return leer(id);
  }
}
