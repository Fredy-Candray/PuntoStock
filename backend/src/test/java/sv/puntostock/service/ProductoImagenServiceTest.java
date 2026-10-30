package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.ProductoImagen;
import sv.puntostock.repository.*;

class ProductoImagenServiceTest {
  ProductoImagenRepository repo = mock(ProductoImagenRepository.class);
  ProductoRepository productos = mock(ProductoRepository.class);
  ProductoImagenService service = new ProductoImagenService(repo, productos);

  static String png() throws Exception {
    var salida = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", salida);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
  }

  @Test
  void guardaRasterYDevuelveRutaProtegidaSinArchivoLocal() throws Exception {
    String data = png();
    String url = service.guardar(1L, data);
    assertTrue(url.startsWith("/api/productos/1/imagen?v="));
    var c = ArgumentCaptor.forClass(ProductoImagen.class);
    verify(repo).save(c.capture());
    var foto = c.getValue();
    assertEquals("image/png", foto.tipo);
    assertEquals(1L, foto.productoId);
    assertNotNull(foto.revision);
    assertArrayEquals(
        Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1)), foto.contenido);
  }

  @Test
  void rechazaContenidoFalsoYMimetypeIncorrecto() throws Exception {
    assertThrows(
        ResponseStatusException.class, () -> service.guardar(1L, "data:image/png;base64,aG9sYQ=="));
    String falsa = png().replace("image/png", "image/jpeg");
    assertThrows(ResponseStatusException.class, () -> service.guardar(1L, falsa));
    assertThrows(
        ResponseStatusException.class,
        () -> service.guardar(1L, "data:image/svg+xml;base64,aG9sYQ=="));
    verify(repo, never()).save(any());
  }

  @Test
  void noEntregaImagenDeProductoInactivo() {
    assertThrows(ResponseStatusException.class, () -> service.consultar(1L));
    verifyNoInteractions(repo);
  }
}
