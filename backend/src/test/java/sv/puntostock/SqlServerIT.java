package sv.puntostock;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import sv.puntostock.application.Contratos.*;
import sv.puntostock.service.VentaService;

/** Solo una base descartable expresamente proporcionada; jamás usa PuntoStock. */
@EnabledIfEnvironmentVariable(
    named = "SQL_TEST_URL",
    matches = ".*databaseName=PuntoStock_qa_[a-f0-9]+;.*")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqlServerIT {
  static {
    if (System.getProperty("os.name").startsWith("Windows"))
      System.setProperty("jdk.net.unixdomain.tmpdir", "../.cache/local/no-unix-sockets");
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add(
        "spring.jpa.properties.hibernate.session_factory.statement_inspector",
        () -> "sv.puntostock.SqlCapture");
    r.add("spring.datasource.url", () -> System.getenv("SQL_TEST_URL"));
    r.add("spring.datasource.username", () -> System.getenv("SQL_TEST_USER"));
    r.add("spring.datasource.password", () -> System.getenv("SQL_TEST_PASSWORD"));
    r.add("app.jwt-secret", () -> "solo-integracion-aislada-32-bytes-minimo");
    r.add("app.bootstrap-user", () -> "");
    r.add("app.bootstrap-password", () -> "");
    r.add("app.recovery.enabled", () -> "false");
    r.add("app.session.secure", () -> false);
  }

  @Autowired JdbcTemplate sql;
  @Autowired VentaService operaciones;
  @Autowired PasswordEncoder passwords;
  @Autowired Flyway flyway;
  @Autowired sv.puntostock.service.CajaService caja;
  @LocalServerPort int port;
  long admin, vendedor, otro;
  String clave = "qa-integration-password";

  @BeforeAll
  void datos() {
    admin = usuario("qa.admin", "ADMIN");
    vendedor = usuario("qa.vendor", "VENDEDOR");
    otro = usuario("qa.other", "VENDEDOR");
  }

  @BeforeEach
  void cajaParaLasPruebasDeVenta() {
    if (!caja.estado().abierta())
      caja.abrir(
          new sv.puntostock.application.CajaContratos.Apertura(
              BigDecimal.ZERO, "Prueba aislada", UUID.randomUUID()),
          "qa.admin");
  }

  long usuario(String nombre, String rol) {
    sql.update(
        "insert into usuarios(username,password_hash,activo) values(?,?,1)",
        nombre,
        passwords.encode(clave));
    long id = sql.queryForObject("select id from usuarios where username=?", Long.class, nombre);
    sql.update(
        "insert into usuario_roles(usuario_id,rol_id) select ?,id from roles where nombre=?",
        id,
        rol);
    return id;
  }

  long producto(int stock) {
    String sku = "qa-" + UUID.randomUUID();
    sql.update(
        "insert into productos(sku,nombre,descripcion,precio_compra,precio_venta,stock,foto_url) values(?,?,?,2.5,4,?,?)",
        sku,
        "Cuaderno",
        "Prueba SQL",
        stock,
        "/assets/producto.svg");
    return sql.queryForObject("select id from productos where sku=?", Long.class, sku);
  }

  VentaInput cobro(long producto, UUID id, int cantidad) {
    return new VentaInput(
        List.of(new Linea(producto, cantidad)),
        "EFECTIVO",
        BigDecimal.valueOf(4L * cantidad),
        BigDecimal.valueOf(4L * cantidad),
        "",
        "",
        id);
  }

  @Test
  void ultimaUnidadConcurrenteSoloUnaVenta() throws Exception {
    long p = producto(1);
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var resultados = new ArrayList<Future<Boolean>>();
      for (String u : List.of("qa.vendor", "qa.other"))
        resultados.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    operaciones.vender(cobro(p, UUID.randomUUID(), 1), u);
                    return true;
                  } catch (org.springframework.web.server.ResponseStatusException e) {
                    assertEquals(409, e.getStatusCode().value());
                    return false;
                  }
                }));
      start.countDown();
      int exitos = 0;
      for (var f : resultados) if (f.get(20, TimeUnit.SECONDS)) exitos++;
      assertEquals(1, exitos);
      assertEquals(
          0, sql.queryForObject("select stock from productos where id=?", Integer.class, p));
      assertEquals(
          1,
          sql.queryForObject(
              "select count(*) from detalle_venta where producto_id=?", Integer.class, p));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void idempotenciaConservaUnaVentaYRechazaContenidoDistinto() {
    long p = producto(5);
    UUID id = UUID.randomUUID();
    var a = operaciones.vender(cobro(p, id, 1), "qa.vendor");
    var b = operaciones.vender(cobro(p, id, 1), "qa.vendor");
    assertEquals(a.id(), b.id());
    assertEquals(4, sql.queryForObject("select stock from productos where id=?", Integer.class, p));
    assertEquals(
        409,
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> operaciones.vender(cobro(p, id, 2), "qa.vendor"))
            .getStatusCode()
            .value());
  }

  @Test
  void falloMovimientoRevierteVentaDetalleYStock() {
    long p = producto(5);
    UUID id = UUID.randomUUID();
    sql.execute(
        "CREATE TRIGGER qa_fallo ON movimientos_inventario AFTER INSERT AS BEGIN IF EXISTS(SELECT 1 FROM inserted WHERE producto_id="
            + p
            + ") THROW 51001,'fallo de prueba aislado',1; END");
    try {
      assertThrows(Exception.class, () -> operaciones.vender(cobro(p, id, 1), "qa.vendor"));
      assertEquals(
          5, sql.queryForObject("select stock from productos where id=?", Integer.class, p));
      assertEquals(
          0,
          sql.queryForObject(
              "select count(*) from ventas where solicitud_id=?", Integer.class, id.toString()));
      assertEquals(
          0,
          sql.queryForObject(
              "select count(*) from detalle_venta where producto_id=?", Integer.class, p));
    } finally {
      sql.execute("DROP TRIGGER qa_fallo");
    }
  }

  @Test
  void snapshotYPrecioRequierenReconfirmacion() {
    long p = producto(5);
    var v = operaciones.vender(cobro(p, UUID.randomUUID(), 1), "qa.vendor");
    sql.update("update productos set nombre='Otro',precio_venta=8,precio_compra=7 where id=?", p);
    assertEquals(
        new BigDecimal("4.00"),
        operaciones.comprobante(v.id(), "qa.vendor").lineas().getFirst().precioUnitario());
    assertEquals(
        "Cuaderno", operaciones.comprobante(v.id(), "qa.vendor").lineas().getFirst().nombre());
    assertEquals(
        409,
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> operaciones.vender(cobro(p, UUID.randomUUID(), 1), "qa.vendor"))
            .getStatusCode()
            .value());
  }

  @Test
  void autenticacionNoCargaImagenes() throws Exception {
    var l = login("qa.admin");
    SqlCapture.consultas.clear();
    assertEquals(200, get(l, "/api/productos").statusCode());
    assertTrue(SqlCapture.consultas.stream().anyMatch(s -> s.contains("auth_version")));
    assertTrue(
        SqlCapture.consultas.stream()
            .noneMatch(s -> s.contains("foto_perfil") || s.contains("foto_portada")));
  }

  record Login(HttpClient client, String token) {}

  @Autowired
  @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
  org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mappings;

  @Test
  void rutasYPermisosCoincidenConElContratoAnterior() throws Exception {
    var esperadas = new TreeSet<String>();
    try (var input = getClass().getResourceAsStream("/contracts/rutas.json")) {
      assertNotNull(input);
      for (var row : new tools.jackson.databind.ObjectMapper().readTree(input))
        esperadas.add(
            row.get(0).asString() + "\t" + row.get(1).asString() + "\t" + row.get(2).asString());
    }
    var actuales = new TreeSet<String>();
    mappings
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              var auth =
                  org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                      handler.getMethod(),
                      org.springframework.security.access.prepost.PreAuthorize.class);
              if (auth == null)
                auth =
                    org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                        handler.getBeanType(),
                        org.springframework.security.access.prepost.PreAuthorize.class);
              String permiso = auth == null ? "security-global" : auth.value();
              for (var path : info.getPatternValues())
                if (path.startsWith("/api/") && !path.startsWith("/api/caja"))
                  for (var method : info.getMethodsCondition().getMethods())
                    actuales.add(method.name() + "\t" + path + "\t" + permiso);
            });
    assertEquals(32, esperadas.size());
    assertEquals(esperadas, actuales);
  }

  HttpResponse<String> enviar(Login login, String method, String path, String body)
      throws Exception {
    return login.client.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + login.token)
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void flujoHttpDeCatalogoInventarioVentaYReporteMantieneContratos() throws Exception {
    var l = login("qa.admin");
    var mapper = new tools.jackson.databind.ObjectMapper();
    String sku = "qa-http-" + UUID.randomUUID();
    String alta =
        "{\"sku\":\""
            + sku
            + "\",\"nombre\":\"Cuaderno HTTP\",\"descripcion\":\"Prueba de contrato\",\"precioCompra\":2.5,\"precioVenta\":4,\"fotoUrl\":\"/assets/producto.svg\"}";
    var creado = enviar(l, "POST", "/api/productos", alta);
    assertEquals(200, creado.statusCode(), creado.body());
    var respuesta = mapper.readTree(creado.body());
    assertEquals(1, respuesta.size());
    long id = Long.parseLong(respuesta.get("id").toString());
    assertEquals(
        0, sql.queryForObject("select stock from productos where id=?", Integer.class, id));
    var movimiento =
        enviar(
            l,
            "POST",
            "/api/inventario/movimientos",
            "{\"productoId\":"
                + id
                + ",\"tipo\":\"COMPRA\",\"cantidad\":2,\"motivo\":\"Entrada HTTP\"}");
    assertEquals(200, movimiento.statusCode(), movimiento.body());
    var venta =
        enviar(
            l,
            "POST",
            "/api/ventas",
            "{\"lineas\":[{\"productoId\":"
                + id
                + ",\"cantidad\":1}],\"metodoPago\":\"EFECTIVO\",\"recibido\":4,\"totalEsperado\":4,\"solicitudId\":\""
                + UUID.randomUUID()
                + "\"}");
    assertEquals(200, venta.statusCode(), venta.body());
    var ticket = mapper.readTree(venta.body());
    long ventaId = Long.parseLong(ticket.get("id").toString());
    assertEquals(
        0, new BigDecimal(ticket.get("total").toString()).compareTo(new BigDecimal("4.00")));
    assertTrue(ticket.has("lineas"));
    assertEquals(
        1, sql.queryForObject("select stock from productos where id=?", Integer.class, id));
    var actualizado =
        enviar(
            l,
            "PUT",
            "/api/productos/" + id,
            alta.replace("\"precioVenta\":4", "\"precioVenta\":5"));
    assertEquals(200, actualizado.statusCode(), actualizado.body());
    var historico = mapper.readTree(get(l, "/api/ventas/" + ventaId).body());
    assertEquals(
        0,
        new BigDecimal(historico.get("lineas").get(0).get("precioUnitario").toString())
            .compareTo(new BigDecimal("4.00")));
    String fecha = java.time.LocalDate.now(sv.puntostock.service.Tiempo.ZONA).toString();
    var historial = get(l, "/api/ventas?fecha=" + fecha);
    assertEquals(200, historial.statusCode(), historial.body());
    assertTrue(historial.body().contains("efectivo"));
    var reporte = get(l, "/api/reportes/ganancia?fecha=" + fecha);
    assertEquals(200, reporte.statusCode(), reporte.body());
    assertTrue(reporte.body().contains("ganancia"));
    assertEquals(200, enviar(l, "DELETE", "/api/productos/" + id, "").statusCode());
    assertEquals(
        1,
        sql.queryForObject(
            "select count(*) from productos where id=? and activo=0", Integer.class, id));
  }

  @Test
  void perfilHttpConservaCredencialesYCancelaRecuperacionAnterior() throws Exception {
    var l = login("qa.vendor");
    var antes = get(l, "/api/auth/me");
    assertEquals(200, antes.statusCode(), antes.body());
    String hash =
        sql.queryForObject("select password_hash from usuarios where id=?", String.class, vendedor);
    sql.update("update usuarios set recuperacion_hash='hash-perfil-qa' where id=?", vendedor);
    var r =
        enviar(
            l,
            "PUT",
            "/api/auth/me",
            "{\"nombre\":\"Ana Perfil\",\"correo\":\"NUEVO@EXAMPLE.TEST\",\"telefono\":\"7777-7777\"}");
    assertEquals(200, r.statusCode(), r.body());
    assertTrue(r.body().contains("nuevo@example.test"));
    assertFalse(r.body().contains("passwordHash"));
    assertNull(
        sql.queryForObject(
            "select recuperacion_hash from usuarios where id=?", String.class, vendedor));
    assertEquals(
        hash,
        sql.queryForObject(
            "select password_hash from usuarios where id=?", String.class, vendedor));
    var foto =
        enviar(
            l, "PUT", "/api/auth/me/foto", "{\"imagen\":\"data:image/svg+xml;base64,PHN2Zz4=\"}");
    assertEquals(400, foto.statusCode());
  }

  @Autowired org.springframework.transaction.PlatformTransactionManager transacciones;

  @Test
  @SuppressWarnings("unchecked")
  void outboxConfirmaAntesDeEnviarYCancelaSolicitudesReemplazadas() {
    long id = usuario("qa.mail", "VENDEDOR");
    var u = new sv.puntostock.entity.Usuario();
    u.id = id;
    u.correo = "qa@example.test";
    u.recuperacionHash = "hash-qa";
    org.springframework.beans.factory.ObjectProvider<
            org.springframework.mail.javamail.JavaMailSender>
        provider = org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
    var sender = org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
    org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(sender);
    org.mockito.Mockito.when(provider.getObject()).thenReturn(sender);
    var outbox =
        new sv.puntostock.service.CorreoOutbox(
            sql,
            provider,
            java.time.Clock.systemUTC(),
            "from@example.test",
            true,
            "solo-integracion-aislada-32-bytes-minimo");
    var tx = new org.springframework.transaction.support.TransactionTemplate(transacciones);
    tx.executeWithoutResult(
        status -> {
          sql.update("update usuarios set recuperacion_hash=? where id=?", u.recuperacionHash, id);
          outbox.encolar(u, "https://example.test/login#recuperar=qa");
          status.setRollbackOnly();
        });
    assertEquals(
        0,
        sql.queryForObject(
            "select count(*) from correo_pendiente where usuario_id=?", Integer.class, id));
    org.mockito.Mockito.verifyNoInteractions(sender);
    tx.executeWithoutResult(
        status -> {
          sql.update("update usuarios set recuperacion_hash=? where id=?", u.recuperacionHash, id);
          outbox.encolar(u, "https://example.test/login#recuperar=qa");
        });
    String cifrado =
        sql.queryForObject(
            "select contenido_cifrado from correo_pendiente where usuario_id=?", String.class, id);
    assertFalse(cifrado.contains("recuperar"));
    assertFalse(cifrado.contains(u.correo));
    org.mockito.Mockito.doAnswer(
            inv -> {
              assertFalse(
                  org.springframework.transaction.support.TransactionSynchronizationManager
                      .isActualTransactionActive());
              return null;
            })
        .when(sender)
        .send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    outbox.enviarPendientes();
    assertEquals(
        "ENVIADO",
        sql.queryForObject(
            "select estado from correo_pendiente where usuario_id=?", String.class, id));
    assertNull(
        sql.queryForObject(
            "select contenido_cifrado from correo_pendiente where usuario_id=?", String.class, id));
    u.recuperacionHash = "hash-reemplazado";
    outbox.encolar(u, "https://example.test/login#recuperar=viejo");
    sql.update("update usuarios set recuperacion_hash=null where id=?", id);
    outbox.enviarPendientes();
    assertEquals(
        1,
        sql.queryForObject(
            "select count(*) from correo_pendiente where usuario_id=? and estado='CANCELADO'",
            Integer.class,
            id));
    org.mockito.Mockito.verify(sender, org.mockito.Mockito.times(1))
        .send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
  }

  @Test
  void listasAdministrativasPaginanYValidanEntrada() throws Exception {
    var l = login("qa.admin");
    for (String path :
        List.of(
            "/api/usuarios/pagina?pagina=0&tamano=1",
            "/api/productos/pagina?pagina=0&tamano=1",
            "/api/inventario/movimientos/pagina?pagina=0&tamano=1")) {
      var r = get(l, path);
      assertEquals(200, r.statusCode(), r.body());
      assertTrue(r.body().contains("contenido"));
      assertTrue(r.body().contains("cantidad"));
      assertFalse(r.body().contains("passwordHash"));
    }
    var invalida = get(l, "/api/productos/pagina?tamano=0");
    assertEquals(400, invalida.statusCode());
    assertTrue(invalida.body().contains("requestId"));
  }

  Login login(String usuario) throws Exception {
    var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    var client = HttpClient.newBuilder().cookieHandler(cookies).build();
    var response =
        client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-PuntoStock", "1")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\""
                            + usuario
                            + "\",\"password\":\""
                            + clave
                            + "\",\"rememberMe\":true}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), response.body());
    String token =
        new tools.jackson.databind.ObjectMapper()
            .readTree(response.body())
            .get("accessToken")
            .asString();
    assertTrue(response.headers().firstValue("set-cookie").orElse("").contains("HttpOnly"));
    return new Login(client, token);
  }

  HttpResponse<String> get(Login l, String path) throws Exception {
    return l.client.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Authorization", "Bearer " + l.token)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void rbacCostosRevocacionYCabeceras() throws Exception {
    var l = login("qa.vendor");
    var catalogo = get(l, "/api/productos");
    assertEquals(200, catalogo.statusCode());
    assertFalse(catalogo.body().contains("precioCompra"));
    assertEquals(403, get(l, "/api/usuarios").statusCode());
    assertTrue(catalogo.headers().firstValue("X-Content-Type-Options").isPresent());
    assertTrue(catalogo.headers().firstValue("X-Request-ID").isPresent());
    sql.update("update usuarios set auth_version=auth_version+1 where id=?", vendedor);
    assertEquals(401, get(l, "/api/productos").statusCode());
  }

  @Test
  void cerrarSesionRevocaElJwtYRenovacionExigeCabecera() throws Exception {
    var l = login("qa.other");
    var url = "http://127.0.0.1:" + port + "/api/auth/renovar";
    var sinCabecera =
        l.client.send(
            HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(403, sinCabecera.statusCode());
    var refresh =
        l.client.send(
            HttpRequest.newBuilder(URI.create(url))
                .header("X-PuntoStock", "1")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, refresh.statusCode());
    var out =
        l.client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/salir"))
                .header("X-PuntoStock", "1")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, out.statusCode());
    assertEquals(401, get(l, "/api/productos").statusCode());
  }

  @Test
  void migracionesDetectanChecksumModificado() {
    flyway.validate();
    Integer checksum =
        sql.queryForObject(
            "select checksum from flyway_schema_history where version='7'", Integer.class);
    try {
      sql.update("update flyway_schema_history set checksum=checksum+1 where version='7'");
      assertThrows(org.flywaydb.core.api.FlywayException.class, () -> flyway.validate());
    } finally {
      sql.update("update flyway_schema_history set checksum=? where version='7'", checksum);
    }
    flyway.validate();
  }

  @Test
  @EnabledIfEnvironmentVariable(
      named = "SQL_BASELINE_URL",
      matches = ".*databaseName=PuntoStock_qa_[a-f0-9]+;.*")
  void baselineExistenteConservaDatosYAplicaLaNuevaVersion() {
    String url = System.getenv("SQL_BASELINE_URL"),
        user = System.getenv("SQL_TEST_USER"),
        password = System.getenv("SQL_TEST_PASSWORD");
    var anterior = Flyway.configure().dataSource(url, user, password).target("6").load();
    anterior.migrate();
    var db =
        new JdbcTemplate(
            new org.springframework.jdbc.datasource.DriverManagerDataSource(url, user, password));
    db.update(
        "insert into productos(sku,nombre,descripcion,precio_compra,precio_venta,stock,foto_url) values('BASELINE','Conservar','Prueba',2,4,7,'/assets/producto.svg')");
    // Solo en la segunda base descartable: simula una instalación histórica sin ejecutor de
    // migraciones.
    db.execute("DROP TABLE flyway_schema_history");
    var actual = Flyway.configure().dataSource(url, user, password).baselineVersion("6").load();
    actual.baseline();
    Flyway.configure().dataSource(url, user, password).target("7").load().migrate();
    db.update(
        "INSERT INTO usuarios(username,password_hash,activo) VALUES('historico-caja',?,1)",
        passwords.encode(clave));
    db.update(
        "INSERT INTO ventas(usuario_id,fecha,total,ganancia) SELECT id,SYSUTCDATETIME(),12,3 FROM usuarios WHERE username='historico-caja'");
    db.update(
        "UPDATE menu_configuracion SET contenido=REPLACE(contenido,N'Espacio de trabajo',N'Mi tienda') WHERE id=1");
    actual.migrate();
    actual.validate();
    assertEquals(
        7, db.queryForObject("select stock from productos where sku='BASELINE'", Integer.class));
    assertEquals(
        2,
        db.queryForObject(
            "select count(*) from sys.tables where name in ('sesiones','correo_pendiente')",
            Integer.class));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT COUNT(*) FROM ventas WHERE total=12 AND ganancia=3 AND caja_id IS NULL",
            Integer.class));
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM cajas", Integer.class));
    String menu =
        db.queryForObject("SELECT contenido FROM menu_configuracion WHERE id=1", String.class);
    assertTrue(menu.contains("Mi tienda"));
    assertTrue(menu.contains("/caja"));
    assertEquals(
        8,
        db.queryForObject(
            "SELECT MAX(TRY_CONVERT(int,version)) FROM flyway_schema_history WHERE success=1",
            Integer.class));
  }

  private sv.puntostock.application.CajaContratos.Resumen nuevaCaja(String inicial) {
    var estado = caja.estado();
    if (estado.abierta()) {
      var c = caja.detalle(estado.id(), "qa.admin");
      caja.cerrar(
          c.id(),
          new sv.puntostock.application.CajaContratos.Cierre(
              c.esperado(), "Preparar prueba", UUID.randomUUID(), c.esperado(), c.ventas()),
          "qa.admin");
    }
    return caja.abrir(
        new sv.puntostock.application.CajaContratos.Apertura(
            new BigDecimal(inicial), "Nueva prueba", UUID.randomUUID()),
        "qa.admin");
  }

  @Test
  void cajaConciliaEfectivoNetoSinSumarTarjetaNiTransferencia() {
    var c = nuevaCaja("20.00");
    long p = producto(10);
    var solicitud = UUID.randomUUID();
    var efectivo =
        new VentaInput(
            List.of(new Linea(p, 1)),
            "EFECTIVO",
            new BigDecimal("10.00"),
            new BigDecimal("4.00"),
            "",
            "",
            solicitud);
    var venta = operaciones.vender(efectivo, "qa.vendor");
    operaciones.vender(efectivo, "qa.vendor");
    for (String medio : List.of("TARJETA", "TRANSFERENCIA"))
      operaciones.vender(
          new VentaInput(
              List.of(new Linea(p, 1)),
              medio,
              null,
              new BigDecimal("4.00"),
              "",
              "",
              UUID.randomUUID()),
          "qa.other");
    var entrada =
        new sv.puntostock.application.CajaContratos.Movimiento(
            "ENTRADA", new BigDecimal("5.00"), "Aporte", UUID.randomUUID());
    caja.mover(c.id(), entrada, "qa.admin");
    caja.mover(c.id(), entrada, "qa.admin");
    caja.mover(
        c.id(),
        new sv.puntostock.application.CajaContratos.Movimiento(
            "SALIDA", new BigDecimal("3.00"), "Transporte", UUID.randomUUID()),
        "qa.admin");
    var r = caja.detalle(c.id(), "qa.admin");
    assertEquals(new BigDecimal("26.00"), r.esperado());
    assertEquals(3, r.ventas());
    assertEquals(new BigDecimal("4.00"), r.efectivo());
    assertEquals(new BigDecimal("4.00"), r.tarjeta());
    assertEquals(new BigDecimal("4.00"), r.transferencia());
    assertEquals(
        c.id(),
        sql.queryForObject("SELECT caja_id FROM ventas WHERE id=?", Long.class, venta.id()));
    var cierre =
        new sv.puntostock.application.CajaContratos.Cierre(
            new BigDecimal("25.00"), "Falta un dólar", UUID.randomUUID(), r.esperado(), r.ventas());
    var cerrado = caja.cerrar(c.id(), cierre, "qa.admin");
    assertEquals(new BigDecimal("-1.00"), cerrado.diferencia());
    assertFalse(caja.estado().abierta());
    assertEquals(cerrado, caja.cerrar(c.id(), cierre, "qa.admin"));
    var recuperada = operaciones.vender(efectivo, "qa.vendor");
    assertEquals(venta.id(), recuperada.id());
    assertEquals(venta.total(), recuperada.total());
    assertEquals(venta.cambio(), recuperada.cambio());
    assertEquals(venta.lineas(), recuperada.lineas());
    assertEquals(5, caja.movimientos(c.id(), 0, "qa.admin").cantidad());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> operaciones.vender(cobro(p, UUID.randomUUID(), 1), "qa.vendor"));
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () ->
            caja.mover(
                c.id(),
                new sv.puntostock.application.CajaContratos.Movimiento(
                    "ENTRADA", BigDecimal.ONE, "Tarde", UUID.randomUUID()),
                "qa.admin"));
  }

  @Test
  void cajaRechazaCierreDesactualizadoRetiroExcesivoYReutilizacionDeSolicitud() {
    var c = nuevaCaja("10.00");
    long p = producto(3);
    var op =
        new sv.puntostock.application.CajaContratos.Movimiento(
            "SALIDA", new BigDecimal("11.00"), "Excesivo", UUID.randomUUID());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> caja.mover(c.id(), op, "qa.admin"));
    assertEquals(new BigDecimal("10.00"), caja.detalle(c.id(), "qa.admin").esperado());
    operaciones.vender(cobro(p, UUID.randomUUID(), 1), "qa.vendor");
    var cierre =
        new sv.puntostock.application.CajaContratos.Cierre(
            new BigDecimal("10.00"), "", UUID.randomUUID(), c.esperado(), c.ventas());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> caja.cerrar(c.id(), cierre, "qa.admin"));
    assertTrue(caja.estado().abierta());
    var entrada =
        new sv.puntostock.application.CajaContratos.Movimiento(
            "ENTRADA", BigDecimal.ONE, "Aporte", UUID.randomUUID());
    caja.mover(c.id(), entrada, "qa.admin");
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () ->
            caja.mover(
                c.id(),
                new sv.puntostock.application.CajaContratos.Movimiento(
                    "ENTRADA", new BigDecimal("2.00"), "Aporte", entrada.solicitudId()),
                "qa.admin"));
    var r = caja.detalle(c.id(), "qa.admin");
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () ->
            caja.cerrar(
                c.id(),
                new sv.puntostock.application.CajaContratos.Cierre(
                    BigDecimal.ZERO, "", UUID.randomUUID(), r.esperado(), r.ventas()),
                "qa.admin"));
    assertTrue(caja.estado().abierta());
  }

  @Test
  void aperturaConcurrenteSoloPermiteUnaCajaAbierta() throws Exception {
    var c = nuevaCaja("0.00");
    caja.cerrar(
        c.id(),
        new sv.puntostock.application.CajaContratos.Cierre(
            BigDecimal.ZERO, "", UUID.randomUUID(), c.esperado(), c.ventas()),
        "qa.admin");
    var pool = Executors.newFixedThreadPool(2);
    var inicio = new CountDownLatch(1);
    try {
      var tareas = new ArrayList<Future<Boolean>>();
      for (int n = 0; n < 2; n++)
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  try {
                    caja.abrir(
                        new sv.puntostock.application.CajaContratos.Apertura(
                            BigDecimal.ZERO, "", UUID.randomUUID()),
                        "qa.admin");
                    return true;
                  } catch (org.springframework.web.server.ResponseStatusException ex) {
                    assertEquals(409, ex.getStatusCode().value());
                    return false;
                  }
                }));
      inicio.countDown();
      int exitos = 0;
      for (var f : tareas) if (f.get(20, TimeUnit.SECONDS)) exitos++;
      assertEquals(1, exitos);
      assertEquals(
          1, sql.queryForObject("SELECT COUNT(*) FROM cajas WHERE cierre IS NULL", Integer.class));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void ventaConcurrenteConCierreNoQuedaFueraDelSaldo() throws Exception {
    var c = nuevaCaja("0.00");
    long p = producto(2);
    var pool = Executors.newFixedThreadPool(2);
    var inicio = new CountDownLatch(1);
    try {
      var venta =
          pool.submit(
              () -> {
                inicio.await();
                try {
                  operaciones.vender(cobro(p, UUID.randomUUID(), 1), "qa.vendor");
                  return true;
                } catch (org.springframework.web.server.ResponseStatusException ex) {
                  assertEquals(409, ex.getStatusCode().value());
                  return false;
                }
              });
      var cierre =
          pool.submit(
              () -> {
                inicio.await();
                try {
                  caja.cerrar(
                      c.id(),
                      new sv.puntostock.application.CajaContratos.Cierre(
                          BigDecimal.ZERO, "", UUID.randomUUID(), c.esperado(), c.ventas()),
                      "qa.admin");
                  return true;
                } catch (org.springframework.web.server.ResponseStatusException ex) {
                  assertEquals(409, ex.getStatusCode().value());
                  return false;
                }
              });
      inicio.countDown();
      boolean vendida = venta.get(20, TimeUnit.SECONDS), cerrada = cierre.get(20, TimeUnit.SECONDS);
      assertNotEquals(vendida, cerrada);
      var r = caja.detalle(c.id(), "qa.admin");
      assertEquals(vendida ? new BigDecimal("4.00") : new BigDecimal("0.00"), r.esperado());
      assertEquals(vendida ? 1 : 0, r.ventas());
      assertEquals(cerrada, r.cierre() != null);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void cajaHttpValidaPermisosImportesYReintentos() throws Exception {
    var c = nuevaCaja("5.00");
    var vendedorLogin = login("qa.vendor");
    assertEquals(200, get(vendedorLogin, "/api/caja/estado").statusCode());
    assertEquals(403, get(vendedorLogin, "/api/caja").statusCode());
    assertEquals(
        403,
        enviar(
                vendedorLogin,
                "POST",
                "/api/caja/abrir",
                "{\"inicial\":0,\"nota\":\"\",\"solicitudId\":\"" + UUID.randomUUID() + "\"}")
            .statusCode());
    var l = login("qa.admin");
    String ruta = "/api/caja/" + c.id() + "/movimientos";
    for (String importe : List.of("0", "-1", "1.001"))
      assertEquals(
          400,
          enviar(
                  l,
                  "POST",
                  ruta,
                  "{\"tipo\":\"ENTRADA\",\"monto\":"
                      + importe
                      + ",\"motivo\":\"Prueba\",\"solicitudId\":\""
                      + UUID.randomUUID()
                      + "\"}")
              .statusCode());
    assertEquals(
        400,
        enviar(
                l,
                "POST",
                ruta,
                "{\"tipo\":\"ENTRADA\",\"monto\":1,\"motivo\":\" \",\"solicitudId\":\""
                    + UUID.randomUUID()
                    + "\"}")
            .statusCode());
    var apertura =
        new sv.puntostock.application.CajaContratos.Apertura(
            BigDecimal.ONE, "Reintento", UUID.randomUUID());
    caja.cerrar(
        c.id(),
        new sv.puntostock.application.CajaContratos.Cierre(
            c.esperado(), "", UUID.randomUUID(), c.esperado(), c.ventas()),
        "qa.admin");
    var a = caja.abrir(apertura, "qa.admin");
    assertEquals(a, caja.abrir(apertura, "qa.admin"));
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () ->
            caja.abrir(
                new sv.puntostock.application.CajaContratos.Apertura(
                    BigDecimal.TEN, "Reintento", apertura.solicitudId()),
                "qa.admin"));
    assertEquals(400, get(l, "/api/caja?pagina=-1").statusCode());
    assertEquals(404, get(l, "/api/caja/99999999").statusCode());
  }
}
