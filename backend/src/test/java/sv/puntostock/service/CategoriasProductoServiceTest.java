package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.CategoriaProductoContratos.Entrada;
import sv.puntostock.entity.CategoriaProducto;
import sv.puntostock.repository.CategoriaProductoRepository;

class CategoriasProductoServiceTest {
  CategoriaProductoRepository repo = mock(CategoriaProductoRepository.class);
  CategoriasProductoService service = new CategoriasProductoService(repo);

  @Test
  void crearLimpiaNombreYGuarda() {
    when(repo.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              CategoriaProducto c = inv.getArgument(0);
              c.id = 2L;
              return c;
            });
    var c = service.guardar(null, new Entrada("  Regalos  ", true, null));
    assertEquals("Regalos", c.nombre());
    assertTrue(c.activo());
    assertEquals(2L, c.id());
  }

  @Test
  void duplicadosYVersionAntiguaNoSeGuardan() {
    when(repo.existsByNombreIgnoreCaseAndIdNot("Regalos", -1L)).thenReturn(true);
    assertThrows(
        ResponseStatusException.class,
        () -> service.guardar(null, new Entrada("Regalos", true, null)));
    CategoriaProducto c = new CategoriaProducto();
    c.version = 3;
    when(repo.bloquear(2L)).thenReturn(Optional.of(c));
    assertThrows(
        ResponseStatusException.class, () -> service.guardar(2L, new Entrada("Otro", true, 2L)));
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void inactivaSoloPermiteConservarAsignacionExistente() {
    CategoriaProducto c = new CategoriaProducto();
    c.nombre = "Antigua";
    c.activo = false;
    when(repo.bloquearNombre("Antigua")).thenReturn(Optional.of(c));
    assertEquals("Antigua", service.paraProducto("Antigua", "Antigua"));
    assertThrows(ResponseStatusException.class, () -> service.paraProducto("Antigua", null));
    assertThrows(ResponseStatusException.class, () -> service.paraProducto("Antigua", "Otra"));
    assertThrows(ResponseStatusException.class, () -> service.paraProducto("No registrada", null));
  }
}
