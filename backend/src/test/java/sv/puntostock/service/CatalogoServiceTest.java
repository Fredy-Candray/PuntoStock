package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos.ProductoInput;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;

class CatalogoServiceTest {
  ProductoRepository productos = mock(ProductoRepository.class);
  MovimientoRepository movimientos = mock(MovimientoRepository.class);
  UsuarioRepository usuarios = mock(UsuarioRepository.class);
  CategoriasProductoService categorias = mock(CategoriasProductoService.class);
  ProductoImagenService imagenes = mock(ProductoImagenService.class);
  CatalogoService service =
      new CatalogoService(
          productos,
          new InventarioService(productos, movimientos, usuarios, Clock.systemUTC()),
          categorias,
          imagenes);

  ProductoInput input(String tipo, Integer inicial) {
    return new ProductoInput(
        "NUEVO",
        "Cuaderno",
        "Cuaderno escolar",
        BigDecimal.ONE,
        BigDecimal.TEN,
        null,
        tipo,
        null,
        null,
        null,
        null,
        5,
        inicial);
  }

  @Test
  void crearConExistenciasRegistraSaldoYAutor() {
    Usuario admin = new Usuario();
    admin.activo = true;
    admin.username = "admin";
    when(usuarios.findByUsername("admin")).thenReturn(Optional.of(admin));
    Producto p = service.guardar(null, input("PRODUCTO", 20), "admin");
    assertEquals(20, p.stock);
    var captor = ArgumentCaptor.forClass(Movimiento.class);
    verify(movimientos).save(captor.capture());
    Movimiento m = captor.getValue();
    assertSame(p, m.producto);
    assertSame(admin, m.usuario);
    assertEquals(0, m.stockAnterior);
    assertEquals(20, m.stockPosterior);
    assertEquals(20, m.cantidad);
    assertEquals("AJUSTE", m.tipo);
    assertNull(m.venta);
  }

  @Test
  void ceroYOmitidoNoGeneranMovimiento() {
    assertEquals(0, service.guardar(null, input("PRODUCTO", 0), "admin").stock);
    assertEquals(0, service.guardar(null, input("PRODUCTO", null), "admin").stock);
    assertEquals(0, service.guardar(null, input("SERVICIO", 0), "admin").stock);
    verifyNoInteractions(movimientos, usuarios);
  }

  @Test
  void rechazaStockEnServiciosEdicionYFueraDeRango() {
    assertThrows(
        ResponseStatusException.class, () -> service.guardar(null, input("SERVICIO", 1), "admin"));
    assertThrows(
        ResponseStatusException.class, () -> service.guardar(1L, input("PRODUCTO", 20), "admin"));
    assertThrows(
        ResponseStatusException.class, () -> service.guardar(null, input("PRODUCTO", -1), "admin"));
    assertThrows(
        ResponseStatusException.class,
        () -> service.guardar(null, input("PRODUCTO", 1000001), "admin"));
    verifyNoInteractions(productos, movimientos);
  }

  @Test
  void editarConservaExistenciasSinCrearEntrada() {
    Producto existente = new Producto();
    existente.stock = 12;
    when(productos.bloquear(1L)).thenReturn(Optional.of(existente));
    assertEquals(12, service.guardar(1L, input("PRODUCTO", null), "admin").stock);
    verifyNoInteractions(movimientos, usuarios);
  }

  ProductoInput automatico(String sku) {
    return new ProductoInput(
        sku,
        "Cuaderno",
        "Escolar",
        BigDecimal.ONE,
        BigDecimal.TEN,
        null,
        "PRODUCTO",
        "Papelería",
        null,
        null,
        null,
        5,
        0);
  }

  @Test
  void compraPorCajaCalculaCostoYExistenciasEnServidor() {
    Usuario u = new Usuario();
    u.activo = true;
    when(usuarios.findByUsername("admin")).thenReturn(Optional.of(u));
    var caja =
        new sv.puntostock.application.Contratos.CajaProductoInput(
            12, new BigDecimal("6.00"), new BigDecimal("8.00"), 2);
    var in =
        new ProductoInput(
            "CAJA",
            "Lápiz",
            "Escolar",
            new BigDecimal("999.00"),
            BigDecimal.ONE,
            null,
            "PRODUCTO",
            "Papelería",
            null,
            "UNIDAD",
            null,
            5,
            3,
            caja);
    Producto p = service.guardar(null, in, "admin");
    assertEquals(27, p.stock);
    assertEquals(new BigDecimal("0.50"), p.precioCompra);
    assertEquals(12, p.unidadesPorCaja);
    assertEquals(new BigDecimal("6.00"), p.precioCompraCaja);
    var c = ArgumentCaptor.forClass(Movimiento.class);
    verify(movimientos).save(c.capture());
    assertEquals(27, c.getValue().cantidad);
  }

  @Test
  void cajasInicialesNoSeAceptanAlEditarNiConUnidadIncorrecta() {
    var caja =
        new sv.puntostock.application.Contratos.CajaProductoInput(
            12, new BigDecimal("6.00"), null, 2);
    Producto p = new Producto();
    p.stock = 10;
    when(productos.bloquear(1L)).thenReturn(Optional.of(p));
    var in =
        new ProductoInput(
            "CAJA",
            "Lápiz",
            "Escolar",
            BigDecimal.ONE,
            BigDecimal.ONE,
            null,
            "PRODUCTO",
            "General",
            null,
            "UNIDAD",
            null,
            5,
            0,
            caja);
    assertThrows(ResponseStatusException.class, () -> service.guardar(1L, in, "admin"));
    var servicio =
        new ProductoInput(
            "CAJA",
            "Lápiz",
            "Escolar",
            BigDecimal.ONE,
            BigDecimal.ONE,
            null,
            "SERVICIO",
            "General",
            null,
            "PAGINA",
            null,
            5,
            0,
            caja);
    assertThrows(ResponseStatusException.class, () -> service.guardar(null, servicio, "admin"));
    verify(productos, never()).save(any());
  }

  @Test
  void codigoAutomaticoUsaIdYEvitaColisionConCodigoManual() {
    when(categorias.paraProducto("Papelería", null)).thenReturn("Papelería");
    when(productos.save(any()))
        .thenAnswer(
            inv -> {
              Producto p = inv.getArgument(0);
              p.id = 48L;
              return p;
            });
    when(productos.existsBySku("ART-PAP-CUA-000048")).thenReturn(true);
    assertEquals("ART-PAP-CUA-000048-2", service.guardar(null, automatico(""), "admin").sku);
  }

  @Test
  void manualSeConservaYEdicionNoGeneraOtroCodigo() {
    Producto p = service.guardar(null, automatico("  7501234567890  "), "admin");
    assertEquals("7501234567890", p.sku);
    verify(productos, never()).existsBySku(any());
    assertThrows(ResponseStatusException.class, () -> service.guardar(1L, automatico(""), "admin"));
  }
}
