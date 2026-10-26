package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos.*;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;

class InventarioServiceTest {
  ProductoRepository productos = mock(ProductoRepository.class);
  MovimientoRepository movimientos = mock(MovimientoRepository.class);
  UsuarioRepository usuarios = mock(UsuarioRepository.class);
  InventarioService service =
      new InventarioService(productos, movimientos, usuarios, Clock.systemUTC());
  Producto p;

  @BeforeEach
  void setup() {
    p = new Producto();
    p.id = 1L;
    p.sku = "SKU";
    p.stock = 5;
    when(productos.bloquear(1L)).thenReturn(Optional.of(p));
    var u = new Usuario();
    u.id = 1L;
    u.username = "vendedor";
    when(usuarios.findByUsername("vendedor")).thenReturn(Optional.of(u));
  }

  @Test
  void devolucionAlProveedorReduceStock() {
    service.mover(new MovimientoInput(1L, "DEVOLUCION_PROVEEDOR", -2, "Defectuoso"), "vendedor");
    assertEquals(3, p.stock);
  }

  @Test
  void rechazaSalidaConSignoPositivo() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.mover(new MovimientoInput(1L, "MERMA", 2, "Rotura"), "vendedor"));
  }

  @Test
  void inventarioRechazaServicios() {
    p.tipo = "SERVICIO";
    assertThrows(
        ResponseStatusException.class,
        () -> service.mover(new MovimientoInput(1L, "COMPRA", 1, "Entrada"), "vendedor"));
    verifyNoInteractions(movimientos);
  }
}
