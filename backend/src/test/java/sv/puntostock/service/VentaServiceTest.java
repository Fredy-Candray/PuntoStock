package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos.*;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;

class VentaServiceTest {
  ProductoRepository productos = mock(ProductoRepository.class);
  VentaRepository ventas = mock(VentaRepository.class);
  MovimientoRepository movimientos = mock(MovimientoRepository.class);
  UsuarioRepository usuarios = mock(UsuarioRepository.class);
  VentaService service =
      new VentaService(
          productos,
          ventas,
          usuarios,
          new InventarioService(productos, movimientos, usuarios, Clock.systemUTC()),
          Clock.systemUTC());
  Producto p;

  @BeforeEach
  void setup() {
    p = new Producto();
    p.id = 1L;
    p.sku = "SKU";
    p.stock = 5;
    p.precioCompra = new BigDecimal("2.50");
    p.precioVenta = new BigDecimal("4.00");
    when(productos.bloquear(1L)).thenReturn(Optional.of(p));
    Usuario u = new Usuario();
    u.id = 1L;
    u.username = "vendedor";
    when(usuarios.findByUsername("vendedor")).thenReturn(Optional.of(u));
    when(usuarios.bloquear("vendedor")).thenReturn(Optional.of(u));
    when(ventas.save(any()))
        .thenAnswer(
            inv -> {
              Venta v = inv.getArgument(0);
              v.id = 10L;
              return v;
            });
  }

  @Test
  void ventaConservaPreciosHistoricosYGeneraSalida() {
    VentaResultado resultado =
        service.vender(new VentaInput(List.of(new Linea(1L, 2))), "vendedor");
    assertEquals(10L, resultado.id());
    assertEquals(new BigDecimal("8.00"), resultado.total());
    var captor = ArgumentCaptor.forClass(Venta.class);
    verify(ventas).save(captor.capture());
    Venta v = captor.getValue();
    assertEquals(new BigDecimal("8.00"), v.total);
    assertEquals(new BigDecimal("3.00"), v.ganancia);
    assertEquals(3, p.stock);
    p.precioCompra = new BigDecimal("3.00");
    assertEquals(new BigDecimal("2.50"), v.detalles.get(0).costoUnitario);
    var mc = ArgumentCaptor.forClass(Movimiento.class);
    verify(movimientos).save(mc.capture());
    assertEquals(-2, mc.getValue().cantidad);
    assertSame(v, mc.getValue().venta);
  }

  @Test
  void stockInsuficienteNoRegistraVenta() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.vender(new VentaInput(List.of(new Linea(1L, 6))), "vendedor"));
    verify(ventas, never()).save(any());
    verify(movimientos, never()).save(any());
    assertEquals(5, p.stock);
  }

  @Test
  void rechazaLineasDuplicadas() {
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.vender(
                new VentaInput(List.of(new Linea(1L, 1), new Linea(1L, 1))), "vendedor"));
    verify(ventas, never()).save(any());
  }

  private VentaInput cobro(String metodo, String recibido, String esperado, UUID id) {
    return new VentaInput(
        List.of(new Linea(1L, 2, "Trabajo de prueba")),
        metodo,
        recibido == null ? null : new BigDecimal(recibido),
        esperado == null ? null : new BigDecimal(esperado),
        "Cliente de prueba",
        "REF-01",
        id);
  }

  @Test
  void efectivoCalculaCambioYConservaDetalle() {
    var r = service.vender(cobro("EFECTIVO", "10.00", "8.00", UUID.randomUUID()), "vendedor");
    assertEquals(new BigDecimal("2.00"), r.cambio());
    assertEquals("EFECTIVO", r.metodoPago());
    assertEquals("SKU", r.lineas().get(0).sku());
    assertEquals("Trabajo de prueba", r.lineas().get(0).observaciones());
  }

  @Test
  void efectivoInsuficienteNoDescuentaNiGuarda() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.vender(cobro("EFECTIVO", "7.99", "8.00", UUID.randomUUID()), "vendedor"));
    assertEquals(5, p.stock);
    verify(ventas, never()).save(any());
    verifyNoInteractions(movimientos);
  }

  @Test
  void precioCambiadoRequiereNuevaConfirmacion() {
    var e =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.vender(cobro("EFECTIVO", "10.00", "7.00", UUID.randomUUID()), "vendedor"));
    assertEquals(409, e.getStatusCode().value());
    assertEquals(5, p.stock);
    verify(ventas, never()).save(any());
  }

  @Test
  void tarjetaNoGeneraCambio() {
    var r = service.vender(cobro("TARJETA", null, "8.00", UUID.randomUUID()), "vendedor");
    assertEquals(new BigDecimal("8.00"), r.recibido());
    assertEquals(0, r.cambio().signum());
  }

  @Test
  void pagoNoEfectivoRechazaSobrepago() {
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.vender(cobro("TRANSFERENCIA", "10.00", "8.00", UUID.randomUUID()), "vendedor"));
    verify(ventas, never()).save(any());
  }

  @Test
  void servicioSeVendeSinStockYSinMovimiento() {
    p.tipo = "SERVICIO";
    p.stock = 0;
    p.precioVenta = new BigDecimal("0.20");
    p.precioCompra = new BigDecimal("0.05");
    var r = service.vender(new VentaInput(List.of(new Linea(1L, 100))), "vendedor");
    assertEquals(new BigDecimal("20.00"), r.total());
    assertEquals(0, p.stock);
    verifyNoInteractions(movimientos);
    var c = ArgumentCaptor.forClass(Venta.class);
    verify(ventas).save(c.capture());
    assertEquals(new BigDecimal("15.00"), c.getValue().ganancia);
  }

  @Test
  void reintentarMismaSolicitudDevuelveMismaVenta() {
    UUID id = UUID.randomUUID();
    var input = cobro("EFECTIVO", "10.00", "8.00", id);
    var first = service.vender(input, "vendedor");
    var c = ArgumentCaptor.forClass(Venta.class);
    verify(ventas).save(c.capture());
    when(ventas.findByUsuarioIdAndSolicitudId(1L, id)).thenReturn(Optional.of(c.getValue()));
    var second = service.vender(input, "vendedor");
    assertEquals(first, second);
    assertEquals(3, p.stock);
    verify(ventas, times(1)).save(any());
    verify(movimientos, times(1)).save(any());
  }

  @Test
  void noPermiteReutilizarSolicitudConOtroContenido() {
    UUID id = UUID.randomUUID();
    service.vender(cobro("EFECTIVO", "10.00", "8.00", id), "vendedor");
    var c = ArgumentCaptor.forClass(Venta.class);
    verify(ventas).save(c.capture());
    when(ventas.findByUsuarioIdAndSolicitudId(1L, id)).thenReturn(Optional.of(c.getValue()));
    assertThrows(
        ResponseStatusException.class,
        () -> service.vender(cobro("EFECTIVO", "20.00", "8.00", id), "vendedor"));
    assertEquals(3, p.stock);
    verify(ventas, times(1)).save(any());
  }

  @Test
  void ticketConservaNombreYCodigoAunqueCambieCatalogo() {
    p.nombre = "Cuaderno";
    service.vender(new VentaInput(List.of(new Linea(1L, 1))), "vendedor");
    var c = ArgumentCaptor.forClass(Venta.class);
    verify(ventas).save(c.capture());
    when(ventas.findById(10L)).thenReturn(Optional.of(c.getValue()));
    p.nombre = "Nuevo nombre";
    p.sku = "OTRO";
    var r = service.comprobante(10L, "vendedor");
    assertEquals("Cuaderno", r.lineas().get(0).nombre());
    assertEquals("SKU", r.lineas().get(0).sku());
  }

  @Test
  void rechazaCantidadInvalidaAntesDeGuardar() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.vender(new VentaInput(List.of(new Linea(1L, -1))), "vendedor"));
    verify(ventas, never()).save(any());
    assertEquals(5, p.stock);
  }
}
