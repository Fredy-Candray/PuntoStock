package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CodigoProductoTest {
  @Test
  void acentosEspaciosYSecuenciaNoGeneranCaracteresEspeciales() {
    assertEquals(
        "ART-UTI-LAP-000048",
        CodigoProducto.crear("PRODUCTO", "Útiles escolares", " Lápiz #2 ", 48));
    assertEquals(
        "SRV-IMP-IMP-000049",
        CodigoProducto.crear("SERVICIO", "Impresiones", "Impresión a color", 49));
  }

  @Test
  void secuenciaCreceYNoSeTrunca() {
    assertEquals("ART-GEN-ART-1000000", CodigoProducto.crear("PRODUCTO", "", "", 1000000));
    assertNotEquals(
        CodigoProducto.crear("PRODUCTO", "General", "Cuaderno", 1),
        CodigoProducto.crear("PRODUCTO", "General", "Cuaderno", 2));
  }
}
