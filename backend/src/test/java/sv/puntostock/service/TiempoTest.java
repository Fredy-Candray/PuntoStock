package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

class TiempoTest {
  @Test
  void cambioDeDiaDelNegocioNoUsaMedianocheUtc() {
    var antes = Clock.fixed(Instant.parse("2026-10-26T05:59:59Z"), ZoneOffset.UTC);
    var despues = Clock.fixed(Instant.parse("2026-10-26T06:00:00Z"), ZoneOffset.UTC);
    assertEquals(LocalDate.of(2026, 10, 25), LocalDate.now(antes.withZone(Tiempo.ZONA)));
    assertEquals(LocalDate.of(2026, 10, 26), LocalDate.now(despues.withZone(Tiempo.ZONA)));
    assertEquals(LocalDateTime.of(2026, 10, 26, 6, 0), Tiempo.inicio(LocalDate.of(2026, 10, 26)));
  }
}
