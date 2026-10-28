package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.CajaContratos.*;
import sv.puntostock.repository.UsuarioRepository;
import sv.puntostock.service.ConsultasService.Pagina;

@Service
@Transactional
public class CajaService {
  private final JdbcTemplate sql;
  private final UsuarioRepository usuarios;
  private final Clock clock;

  public CajaService(JdbcTemplate sql, UsuarioRepository usuarios, Clock clock) {
    this.sql = sql;
    this.usuarios = usuarios;
    this.clock = clock;
  }

  private static ResponseStatusException conflicto(String s) {
    return new ResponseStatusException(CONFLICT, s);
  }

  private long administrador(String username) {
    var u =
        usuarios
            .findByUsername(username)
            .filter(x -> x.activo)
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    if (u.roles.stream().noneMatch(r -> r.nombre.equals("ADMIN")))
      throw new ResponseStatusException(FORBIDDEN);
    return u.id;
  }

  private void bloquear() {
    // Esta fila existe incluso con caja cerrada: serializa apertura, venta, movimiento y cierre.
    sql.queryForObject(
        "SELECT id FROM caja_control WITH (UPDLOCK,HOLDLOCK) WHERE id=1", Integer.class);
  }

  private Long abierta() {
    return sql.query("SELECT id FROM cajas WHERE cierre IS NULL", (r, n) -> r.getLong(1)).stream()
        .findFirst()
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public Estado estado() {
    var id = abierta();
    return new Estado(id != null, id);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public long paraVenta() {
    bloquear();
    var id = abierta();
    if (id == null)
      throw conflicto(
          "La caja está cerrada. Solicita al administrador que la abra antes de vender.");
    return id;
  }

  private LocalDateTime ahora() {
    return LocalDateTime.now(clock);
  }

  public Resumen abrir(Apertura in, String username) {
    long u = administrador(username);
    bloquear();
    var previa =
        sql.query(
            "SELECT id,inicial,nota_apertura,abierto_por FROM cajas WHERE solicitud_apertura=?",
            (r, n) -> new Object[] {r.getLong(1), r.getBigDecimal(2), r.getString(3), r.getLong(4)},
            in.solicitudId().toString());
    if (!previa.isEmpty()) {
      var p = previa.getFirst();
      if (((BigDecimal) p[1]).compareTo(in.inicial()) != 0
          || !p[2].equals(in.nota().trim())
          || !p[3].equals(u)) throw conflicto("La solicitud ya se utilizó con otra apertura.");
      return resumen((Long) p[0]);
    }
    if (abierta() != null) throw conflicto("Ya hay una caja abierta. Actualiza la pantalla.");
    long id =
        sql.queryForObject(
            "INSERT INTO cajas(apertura,abierto_por,inicial,nota_apertura,solicitud_apertura) OUTPUT INSERTED.id VALUES(?,?,?,?,?)",
            Long.class,
            ahora(),
            u,
            in.inicial(),
            in.nota().trim(),
            in.solicitudId().toString());
    return resumen(id);
  }

  public Resumen mover(long id, Movimiento in, String username) {
    long u = administrador(username);
    bloquear();
    var previa =
        sql.query(
            "SELECT caja_id,usuario_id,tipo,monto,motivo FROM caja_movimientos WHERE solicitud_id=?",
            (r, n) ->
                new Object[] {
                  r.getLong(1), r.getLong(2), r.getString(3), r.getBigDecimal(4), r.getString(5)
                },
            in.solicitudId().toString());
    if (!previa.isEmpty()) {
      var p = previa.getFirst();
      if (!p[0].equals(id)
          || !p[1].equals(u)
          || !p[2].equals(in.tipo())
          || ((BigDecimal) p[3]).compareTo(in.monto()) != 0
          || !p[4].equals(in.motivo().trim()))
        throw conflicto("La solicitud ya se utilizó con otro movimiento.");
      return resumen(id);
    }
    var caja = resumen(id);
    if (caja.cierre() != null) throw conflicto("Esta caja ya está cerrada.");
    if (in.tipo().equals("SALIDA") && in.monto().compareTo(caja.esperado()) > 0)
      throw conflicto("La salida supera el efectivo esperado en caja.");
    sql.update(
        "INSERT INTO caja_movimientos(caja_id,usuario_id,fecha,tipo,monto,motivo,solicitud_id) VALUES(?,?,?,?,?,?,?)",
        id,
        u,
        ahora(),
        in.tipo(),
        in.monto(),
        in.motivo().trim(),
        in.solicitudId().toString());
    return resumen(id);
  }

  public Resumen cerrar(long id, Cierre in, String username) {
    long u = administrador(username);
    bloquear();
    var caja = resumen(id);
    if (caja.cierre() != null) {
      boolean mismo =
          Boolean.TRUE.equals(
              sql.queryForObject(
                  "SELECT CASE WHEN solicitud_cierre=? AND cerrado_por=? AND contado=? AND nota_cierre=? THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END FROM cajas WHERE id=?",
                  Boolean.class,
                  in.solicitudId().toString(),
                  u,
                  in.contado(),
                  in.nota().trim(),
                  id));
      if (!mismo || caja.esperado().compareTo(in.esperado()) != 0 || caja.ventas() != in.ventas())
        throw conflicto("La caja ya se cerró. Consulta el cierre registrado.");
      return caja;
    }
    if (caja.esperado().compareTo(in.esperado()) != 0 || caja.ventas() != in.ventas())
      throw conflicto(
          "La caja recibió operaciones nuevas. Actualiza el resumen y revisa el conteo antes de cerrar.");
    if (caja.esperado().precision() - caja.esperado().scale() > 17)
      throw conflicto("El saldo supera el límite de cierre permitido.");
    BigDecimal diferencia = in.contado().subtract(caja.esperado());
    if (diferencia.signum() != 0 && in.nota().isBlank())
      throw new ResponseStatusException(
          BAD_REQUEST, "Explica la diferencia en la observación del cierre.");
    sql.update(
        "UPDATE cajas SET cierre=?,cerrado_por=?,contado=?,esperado=?,diferencia=?,nota_cierre=?,solicitud_cierre=? WHERE id=? AND cierre IS NULL",
        ahora(),
        u,
        in.contado(),
        caja.esperado(),
        diferencia,
        in.nota().trim(),
        in.solicitudId().toString(),
        id);
    return resumen(id);
  }

  // Una sola consulta evita combinar saldos de instantes distintos.
  private static final String RESUMEN =
      """
    SELECT c.*,a.username AS abierto_nombre,z.username AS cerrado_nombre,
      COALESCE(v.efectivo,0) AS efectivo,COALESCE(v.tarjeta,0) AS tarjeta,
      COALESCE(v.transferencia,0) AS transferencia,COALESCE(v.cantidad,0) AS cantidad,
      COALESCE(m.entradas,0) AS entradas,COALESCE(m.salidas,0) AS salidas
    FROM cajas c JOIN usuarios a ON a.id=c.abierto_por LEFT JOIN usuarios z ON z.id=c.cerrado_por
    OUTER APPLY (SELECT SUM(CASE WHEN metodo_pago='EFECTIVO' THEN total ELSE 0 END) efectivo,
      SUM(CASE WHEN metodo_pago='TARJETA' THEN total ELSE 0 END) tarjeta,
      SUM(CASE WHEN metodo_pago='TRANSFERENCIA' THEN total ELSE 0 END) transferencia,
      COUNT_BIG(*) cantidad FROM ventas WHERE caja_id=c.id) v
    OUTER APPLY (SELECT SUM(CASE WHEN tipo='ENTRADA' THEN monto ELSE 0 END) entradas,
      SUM(CASE WHEN tipo='SALIDA' THEN monto ELSE 0 END) salidas FROM caja_movimientos WHERE caja_id=c.id) m
    """;

  private static OffsetDateTime fecha(ResultSet r, String name) throws SQLException {
    var t = r.getTimestamp(name);
    return t == null ? null : t.toLocalDateTime().atOffset(ZoneOffset.UTC);
  }

  private Resumen mapear(ResultSet r, int n) throws SQLException {
    BigDecimal inicial = r.getBigDecimal("inicial"),
        efectivo = r.getBigDecimal("efectivo"),
        entradas = r.getBigDecimal("entradas"),
        salidas = r.getBigDecimal("salidas");
    var cierre = fecha(r, "cierre");
    return new Resumen(
        r.getLong("id"),
        fecha(r, "apertura"),
        r.getString("abierto_nombre"),
        inicial,
        r.getString("nota_apertura"),
        cierre,
        r.getString("cerrado_nombre"),
        r.getBigDecimal("contado"),
        r.getBigDecimal("diferencia"),
        r.getString("nota_cierre"),
        efectivo,
        r.getBigDecimal("tarjeta"),
        r.getBigDecimal("transferencia"),
        r.getLong("cantidad"),
        entradas,
        salidas,
        cierre == null
            ? inicial.add(efectivo).add(entradas).subtract(salidas)
            : r.getBigDecimal("esperado"));
  }

  private Resumen resumen(long id) {
    return sql.query(RESUMEN + " WHERE c.id=?", this::mapear, id).stream()
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Caja no encontrada."));
  }

  public Resumen detalle(long id, String username) {
    administrador(username);
    bloquear();
    return resumen(id);
  }

  public Pagina<Resumen> historial(int pagina, String username) {
    administrador(username);
    ConsultasService.pagina(pagina, 20);
    bloquear();
    long n = sql.queryForObject("SELECT COUNT_BIG(*) FROM cajas", Long.class);
    var lista =
        sql.query(
            RESUMEN + " ORDER BY c.id DESC OFFSET ? ROWS FETCH NEXT 20 ROWS ONLY",
            this::mapear,
            (long) pagina * 20);
    return new Pagina<>(lista, n, pagina, (int) ((n + 19) / 20));
  }

  public Pagina<Registro> movimientos(long id, int pagina, String username) {
    administrador(username);
    ConsultasService.pagina(pagina, 20);
    bloquear();
    resumen(id);
    String union =
        """
      SELECT CONCAT('m-',m.id) id,m.fecha,u.username usuario,m.tipo,m.motivo,
        CASE WHEN m.tipo='SALIDA' THEN -m.monto ELSE m.monto END monto,'EFECTIVO' metodo_pago
      FROM caja_movimientos m JOIN usuarios u ON u.id=m.usuario_id WHERE m.caja_id=?
      UNION ALL
      SELECT CONCAT('v-',v.id),v.fecha,u.username,'VENTA',CONCAT('Venta #',v.id),v.total,v.metodo_pago
      FROM ventas v JOIN usuarios u ON u.id=v.usuario_id WHERE v.caja_id=?
      """;
    long n = sql.queryForObject("SELECT COUNT_BIG(*) FROM (" + union + ") x", Long.class, id, id);
    var lista =
        sql.query(
            "SELECT * FROM ("
                + union
                + ") x ORDER BY fecha DESC,id DESC OFFSET ? ROWS FETCH NEXT 20 ROWS ONLY",
            (r, i) ->
                new Registro(
                    r.getString("id"),
                    fecha(r, "fecha"),
                    r.getString("usuario"),
                    r.getString("tipo"),
                    r.getString("motivo"),
                    r.getBigDecimal("monto"),
                    r.getString("metodo_pago")),
            id,
            id,
            (long) pagina * 20);
    return new Pagina<>(lista, n, pagina, (int) ((n + 19) / 20));
  }
}
