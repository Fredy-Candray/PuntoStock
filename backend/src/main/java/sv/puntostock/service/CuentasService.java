package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.CuentasContratos.*;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class CuentasService {
  private static final int LIMITE = 6;
  private static final String SALDOS =
      """
      SELECT c.id,c.nombre,c.ambito,c.inicial+COALESCE(d.entradas,0)-COALESCE(o.salidas,0) saldo
      FROM cuentas c
      OUTER APPLY (SELECT CAST(SUM(monto) AS decimal(19,2)) entradas FROM cuenta_movimientos WHERE destino=c.id) d
      OUTER APPLY (SELECT CAST(SUM(COALESCE(pagado_inicial,monto)) AS decimal(19,2)) salidas FROM cuenta_movimientos WHERE origen=c.id) o
      """;
  private static final String DEUDAS =
      """
      SELECT m.id,m.concepto,m.monto-m.pagado_inicial-COALESCE(p.pagado,0) pendiente
      FROM cuenta_movimientos m
      OUTER APPLY (SELECT CAST(SUM(monto) AS decimal(19,2)) pagado FROM cuenta_movimientos WHERE compra_id=m.id) p
      WHERE m.tipo='COMPRA' AND m.monto>m.pagado_inicial+COALESCE(p.pagado,0)
      """;
  private static final String COBROS =
      """
      SELECT p.id,p.cliente,p.total-COALESCE(a.pagado,0) pendiente
      FROM pedidos p
      OUTER APPLY (SELECT CAST(SUM(monto) AS decimal(19,2)) pagado FROM pedido_abonos WHERE pedido_id=p.id) a
      WHERE p.estado<>'CANCELADO' AND p.total>COALESCE(a.pagado,0)
      """;
  private final JdbcTemplate sql;
  private final CajaService caja;
  private final InventarioService inventario;
  private final ObjectMapper json;
  private final Clock clock;

  public CuentasService(
      JdbcTemplate sql,
      CajaService caja,
      InventarioService inventario,
      ObjectMapper json,
      Clock clock) {
    this.sql = sql;
    this.caja = caja;
    this.inventario = inventario;
    this.json = json;
    this.clock = clock;
  }

  public record Vista(
      List<Cuenta> cuentas,
      List<Movimiento> movimientos,
      int pagina,
      long totalMovimientos,
      List<Deuda> porPagar,
      List<Cobro> porCobrar,
      Resultado resultado,
      CajaEstado caja) {}

  public record CajaEstado(Long id, boolean pendienteDeCierre) {}

  @Transactional(readOnly = true)
  public Vista consultar(LocalDate desde, LocalDate hasta, int pagina) {
    ConsultasService.pagina(pagina, LIMITE);
    if (desde.getYear() < 1900 || hasta.getYear() > 9998 || desde.isAfter(hasta))
      throw new ResponseStatusException(BAD_REQUEST, "Revisa el intervalo de fechas.");
    var inicio = Tiempo.inicio(desde);
    var fin = Tiempo.inicio(hasta.plusDays(1));
    var cuentas = sql.query(SALDOS + " ORDER BY c.ambito,c.nombre,c.id", this::cuenta);
    var movimientos =
        sql.query(
            """
        SELECT m.*,o.nombre origen_nombre,d.nombre destino_nombre FROM cuenta_movimientos m
        LEFT JOIN cuentas o ON o.id=m.origen LEFT JOIN cuentas d ON d.id=m.destino
        WHERE m.fecha>=? AND m.fecha<? ORDER BY m.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        """,
            (r, n) ->
                new Movimiento(
                    r.getLong("id"),
                    r.getObject("fecha", LocalDateTime.class).atOffset(ZoneOffset.UTC),
                    Tipo.valueOf(r.getString("tipo")),
                    r.getString("concepto"),
                    r.getBigDecimal("monto"),
                    r.getString("origen_nombre"),
                    r.getString("destino_nombre"),
                    r.getObject("caja_id", Long.class),
                    r.getObject("cantidad", Integer.class),
                    r.getBigDecimal("pagado_inicial"),
                    r.getObject("compra_id", Long.class)),
            inicio,
            fin,
            (long) pagina * LIMITE,
            LIMITE);
    long total =
        sql.queryForObject(
            "SELECT COUNT_BIG(*) FROM cuenta_movimientos WHERE fecha>=? AND fecha<?",
            Long.class,
            inicio,
            fin);
    var deudas =
        sql.query(
            DEUDAS + " ORDER BY m.id DESC",
            (r, n) -> new Deuda(r.getLong(1), r.getString(2), r.getBigDecimal(3)));
    var cobros =
        sql.query(
            COBROS + " ORDER BY p.id DESC",
            (r, n) -> new Cobro(r.getLong(1), r.getString(2), r.getBigDecimal(3)));
    var estado = caja.estado();
    return new Vista(
        cuentas,
        movimientos,
        pagina,
        total,
        deudas,
        cobros,
        resultado(inicio, fin, deudas, cobros),
        new CajaEstado(estado.id(), estado.pendienteDeCierre()));
  }

  private Resultado resultado(
      LocalDateTime inicio, LocalDateTime fin, List<Deuda> deudas, List<Cobro> cobros) {
    var venta =
        suma("SELECT COALESCE(SUM(total),0) FROM ventas WHERE fecha>=? AND fecha<?", inicio, fin);
    var bruta =
        suma(
            "SELECT COALESCE(SUM(ganancia),0) FROM ventas WHERE fecha>=? AND fecha<?", inicio, fin);
    var gastos =
        suma(
            """
            SELECT COALESCE(SUM(m.monto),0) FROM cuenta_movimientos m
            LEFT JOIN cuentas c ON c.id=m.origen
            WHERE m.tipo='GASTO' AND (m.caja_id IS NOT NULL OR c.ambito='NEGOCIO')
              AND m.fecha>=? AND m.fecha<?
            """,
            inicio,
            fin);
    var otrosIngresos =
        suma(
            """
            SELECT COALESCE(SUM(m.monto),0) FROM cuenta_movimientos m
            JOIN cuentas c ON c.id=m.destino
            WHERE m.tipo='INGRESO' AND c.ambito='NEGOCIO' AND m.fecha>=? AND m.fecha<?
            """,
            inicio,
            fin);
    var compras =
        suma(
            "SELECT COALESCE(SUM(monto),0) FROM cuenta_movimientos WHERE tipo='COMPRA' AND fecha>=? AND fecha<?",
            inicio,
            fin);
    var pedidos =
        suma(
            "SELECT COALESCE(SUM(total),0) FROM pedidos WHERE fecha_entregado>=? AND fecha_entregado<?",
            inicio,
            fin);
    var costo =
        suma(
            """
        SELECT COALESCE(SUM(-m.cantidad*m.costo_unitario_insumo),0) FROM movimientos_inventario m
        JOIN pedidos p ON p.id=m.pedido_id WHERE p.fecha_entregado>=? AND p.fecha_entregado<?
        """,
            inicio,
            fin);
    long sinCosto =
        sql.queryForObject(
            """
        SELECT COUNT_BIG(*) FROM pedidos p WHERE p.fecha_entregado>=? AND p.fecha_entregado<?
        AND (NOT EXISTS (SELECT 1 FROM movimientos_inventario WHERE pedido_id=p.id)
          OR EXISTS (SELECT 1 FROM movimientos_inventario WHERE pedido_id=p.id AND costo_unitario_insumo IS NULL))
        """,
            Long.class,
            inicio,
            fin);
    long sinFecha =
        sql.queryForObject(
            "SELECT COUNT_BIG(*) FROM pedidos WHERE estado='ENTREGADO' AND fecha_entregado IS NULL",
            Long.class);
    var inventario =
        suma("SELECT COALESCE(SUM(stock*precio_compra),0) FROM productos WHERE tipo='PRODUCTO'");
    return new Resultado(
        venta,
        bruta,
        otrosIngresos,
        gastos,
        pedidos,
        costo,
        sinCosto,
        bruta.add(pedidos).add(otrosIngresos).subtract(costo).subtract(gastos),
        compras,
        inventario,
        sinFecha,
        deudas.stream().map(Deuda::pendiente).reduce(BigDecimal.ZERO, BigDecimal::add),
        cobros.stream().map(Cobro::pendiente).reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private BigDecimal suma(String consulta, Object... parametros) {
    return sql.queryForObject(consulta, BigDecimal.class, parametros);
  }

  private Cuenta cuenta(java.sql.ResultSet r, int fila) throws java.sql.SQLException {
    return new Cuenta(
        r.getLong("id"),
        r.getString("nombre"),
        Ambito.valueOf(r.getString("ambito")),
        r.getBigDecimal("saldo"));
  }

  public void alta(Alta in) {
    caja.bloquear();
    var previa =
        sql.query(
            "SELECT nombre,ambito,inicial FROM cuentas WHERE solicitud=?",
            (r, n) -> new Object[] {r.getString(1), r.getString(2), r.getBigDecimal(3)},
            in.solicitud().toString());
    if (!previa.isEmpty()) {
      var datos = previa.getFirst();
      exigir(
          datos[0].equals(in.nombre().strip())
              && datos[1].equals(in.ambito().name())
              && ((BigDecimal) datos[2]).compareTo(in.inicial()) == 0,
          "La solicitud ya se usó para otra cuenta.");
      return;
    }
    sql.update(
        "INSERT INTO cuentas(nombre,ambito,inicial,solicitud) VALUES(?,?,?,?)",
        in.nombre().strip(),
        in.ambito().name(),
        in.inicial(),
        in.solicitud().toString());
  }

  public void mover(Operacion in, String username) {
    long actor = inventario.actor(username).id;
    caja.bloquear();
    String solicitud = in.solicitud().toString();
    String huella = huella(in);
    var previa =
        sql.query(
            "SELECT huella,usuario_id FROM cuenta_movimientos WHERE solicitud=?",
            (r, n) -> new Object[] {r.getString(1), r.getLong(2)},
            solicitud);
    if (!previa.isEmpty()) {
      var datos = previa.getFirst();
      exigir(
          datos[0].equals(huella) && datos[1].equals(actor),
          "La solicitud ya se usó para otro movimiento.");
      return;
    }
    var origen = buscar(in.origen());
    var destino = buscar(in.destino());
    var tipo = in.tipo();
    boolean cajaEntrada = tipo == Tipo.TRASPASO && in.cajaId() != null && origen != null;
    boolean cajaSalida = tipo == Tipo.TRASPASO && in.cajaId() != null && destino != null;
    exigir(
        in.cajaId() == null
            || cajaEntrada
            || cajaSalida
            || tipo == Tipo.GASTO
            || tipo == Tipo.COMPRA
            || tipo == Tipo.PAGO_COMPRA,
        "Selecciona una operación compatible con caja.");
    exigir(
        origen == null || destino == null || origen.id() != destino.id(),
        "Elige cuentas distintas.");
    switch (tipo) {
      case APORTE ->
          exigir(
              es(origen, Ambito.PERSONAL) && es(destino, Ambito.NEGOCIO),
              "El aporte va de una cuenta personal a una del negocio.");
      case RETIRO ->
          exigir(
              es(origen, Ambito.NEGOCIO) && es(destino, Ambito.PERSONAL),
              "El retiro va del negocio a una cuenta personal.");
      case TRASPASO ->
          exigir(
              in.cajaId() == null
                  ? origen != null && destino != null && origen.ambito() == destino.ambito()
                  : cajaEntrada && es(origen, Ambito.NEGOCIO) && destino == null
                      || cajaSalida && es(destino, Ambito.NEGOCIO) && origen == null,
              "El traspaso requiere cuentas del mismo ámbito. La caja pertenece al negocio.");
      case INGRESO ->
          exigir(origen == null && destino != null, "Selecciona la cuenta que recibe el dinero.");
      case GASTO ->
          exigir(
              destino == null && (origen != null ^ in.cajaId() != null),
              "Selecciona un solo origen de pago.");
      case COMPRA, PAGO_COMPRA ->
          exigir(
              destino == null
                  && (origen == null || es(origen, Ambito.NEGOCIO))
                  && !(origen != null && in.cajaId() != null),
              "Selecciona un solo origen del negocio.");
    }
    BigDecimal salida = in.monto();
    if (tipo == Tipo.COMPRA) {
      salida = in.pagadoInicial();
      exigir(
          in.productoId() != null
              && in.cantidad() != null
              && salida != null
              && salida.compareTo(in.monto()) <= 0,
          "Indica producto, cantidad y cuánto pagaste de la compra.");
      exigir(
          (salida.signum() > 0) == (origen != null || in.cajaId() != null),
          "Indica el origen solo si hubo un pago.");
    } else {
      exigir(
          in.productoId() == null && in.cantidad() == null && in.pagadoInicial() == null,
          "Los datos de inventario solo se incluyen en una compra.");
    }
    if (tipo == Tipo.PAGO_COMPRA) {
      exigir(
          in.compraId() != null && (origen != null || in.cajaId() != null),
          "Selecciona una compra y el origen del pago.");
      var saldo =
          sql.query(
              "SELECT pendiente FROM (" + DEUDAS + ") d WHERE id=?",
              (r, n) -> r.getBigDecimal(1),
              in.compraId());
      exigir(
          !saldo.isEmpty() && salida.compareTo(saldo.getFirst()) <= 0,
          "El pago supera la deuda pendiente.");
    } else {
      exigir(in.compraId() == null, "Selecciona una deuda solo al pagar una compra.");
    }
    if (origen != null)
      exigir(origen.saldo().compareTo(salida) >= 0, "Saldo insuficiente en la cuenta de origen.");
    String concepto = in.concepto().strip();
    if (in.cajaId() != null) {
      exigir(
          Objects.equals(caja.estado().id(), in.cajaId()),
          "Cambió la caja abierta. Actualiza antes de guardar.");
      caja.mover(
          in.cajaId(),
          new sv.puntostock.application.CajaContratos.Movimiento(
              cajaEntrada ? "ENTRADA" : "SALIDA", salida, "Cuentas · " + concepto, in.solicitud()),
          username);
    }
    if (tipo == Tipo.COMPRA)
      inventario.recibirCompra(
          in.productoId(), in.cantidad(), in.monto(), "Compra · " + concepto, username);
    sql.update(
        """
        INSERT INTO cuenta_movimientos(solicitud,huella,fecha,usuario_id,tipo,origen,destino,caja_id,concepto,monto,
        compra_id,producto_id,cantidad,pagado_inicial) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """,
        solicitud,
        huella,
        LocalDateTime.now(clock),
        actor,
        tipo.name(),
        in.origen(),
        in.destino(),
        in.cajaId(),
        concepto,
        in.monto(),
        in.compraId(),
        in.productoId(),
        in.cantidad(),
        in.pagadoInicial());
  }

  private Cuenta buscar(Long id) {
    if (id == null) return null;
    return sql.query(SALDOS + " WHERE c.id=?", this::cuenta, id).stream()
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cuenta no encontrada."));
  }

  private static boolean es(Cuenta cuenta, Ambito ambito) {
    return cuenta != null && cuenta.ambito() == ambito;
  }

  private static void exigir(boolean valido, String mensaje) {
    if (!valido) throw new ResponseStatusException(CONFLICT, mensaje);
  }

  private String huella(Operacion in) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(in)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
