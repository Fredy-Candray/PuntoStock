package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FotoPerfilValidatorTest {
  @Test
  void admiteImagenRealYPermiteQuitarla() throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
    String image = "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    assertEquals(image, FotoPerfilValidator.validar(image));
    assertNull(FotoPerfilValidator.validar(""));
  }

  @Test
  void rechazaContenidoFalsoYTiposActivos() {
    assertThrows(
        ResponseStatusException.class,
        () -> FotoPerfilValidator.validar("data:image/png;base64,YWJj"));
    assertThrows(
        ResponseStatusException.class,
        () -> FotoPerfilValidator.validar("data:image/svg+xml;base64,PHN2Zz4="));
    assertThrows(
        ResponseStatusException.class,
        () -> FotoPerfilValidator.validar("https://example.test/foto.png"));
  }

  @Test
  void rechazaImagenExcesivaSinDecodificar() {
    assertThrows(
        ResponseStatusException.class,
        () -> FotoPerfilValidator.validar("data:image/jpeg;base64," + "A".repeat(1_500_000)));
  }
}
