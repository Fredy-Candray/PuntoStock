package sv.puntostock.service;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;
import sv.puntostock.api.Contratos.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OperacionServiceTest {
 ProductoRepository productos=mock(ProductoRepository.class);
 VentaRepository ventas=mock(VentaRepository.class);
 MovimientoRepository movimientos=mock(MovimientoRepository.class);
 UsuarioRepository usuarios=mock(UsuarioRepository.class);
 OperacionService service=new OperacionService(productos,ventas,movimientos,usuarios);
 Producto p;
 @BeforeEach void setup(){
  p=new Producto();p.id=1L;p.sku="SKU";p.stock=5;p.precioCompra=new BigDecimal("2.50");p.precioVenta=new BigDecimal("4.00");
  when(productos.bloquear(1L)).thenReturn(Optional.of(p));
  Usuario u=new Usuario();u.id=1L;u.username="vendedor";when(usuarios.findByUsername("vendedor")).thenReturn(Optional.of(u));
  when(ventas.save(any())).thenAnswer(inv->{Venta v=inv.getArgument(0);v.id=10L;return v;});
 }
 @Test void ventaConservaPreciosHistoricosYGeneraSalida(){
  VentaResultado resultado=service.vender(new VentaInput(List.of(new Linea(1L,2))),"vendedor");
  assertEquals(10L,resultado.id());assertEquals(new BigDecimal("8.00"),resultado.total());
  var captor=ArgumentCaptor.forClass(Venta.class);verify(ventas).save(captor.capture());Venta v=captor.getValue();
  assertEquals(new BigDecimal("8.00"),v.total);assertEquals(new BigDecimal("3.00"),v.ganancia);assertEquals(3,p.stock);
  p.precioCompra=new BigDecimal("3.00");assertEquals(new BigDecimal("2.50"),v.detalles.get(0).costoUnitario);
  var mc=ArgumentCaptor.forClass(Movimiento.class);verify(movimientos).save(mc.capture());assertEquals(-2,mc.getValue().cantidad);assertSame(v,mc.getValue().venta);
 }
 @Test void stockInsuficienteNoRegistraVenta(){
  assertThrows(ResponseStatusException.class,()->service.vender(new VentaInput(List.of(new Linea(1L,6))),"vendedor"));
  verify(ventas,never()).save(any());verify(movimientos,never()).save(any());assertEquals(5,p.stock);
 }
 @Test void rechazaLineasDuplicadas(){
  assertThrows(ResponseStatusException.class,()->service.vender(new VentaInput(List.of(new Linea(1L,1),new Linea(1L,1))),"vendedor"));verify(ventas,never()).save(any());
 }
 @Test void devolucionAlProveedorReduceStock(){service.mover(new MovimientoInput(1L,"DEVOLUCION_PROVEEDOR",-2,"Defectuoso"),"vendedor");assertEquals(3,p.stock);}
 @Test void rechazaSalidaConSignoPositivo(){assertThrows(ResponseStatusException.class,()->service.mover(new MovimientoInput(1L,"MERMA",2,"Rotura"),"vendedor"));}
 @Test void reporteUsaIntervaloUtcDelDiaLocal(){
  var desde=LocalDateTime.of(2026,10,15,6,0);var hasta=desde.plusDays(1);
  when(ventas.ganancia(desde,hasta)).thenReturn(BigDecimal.TEN);
  assertEquals(BigDecimal.TEN,service.ganancia(LocalDate.of(2026,10,15)));verify(ventas).ganancia(desde,hasta);
 }
}
