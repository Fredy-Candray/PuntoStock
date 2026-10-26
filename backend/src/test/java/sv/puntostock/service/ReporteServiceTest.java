package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import sv.puntostock.application.Contratos.*;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;

class ReporteServiceTest {
  VentaRepository ventas = mock(VentaRepository.class);
  ReporteService service = new ReporteService(ventas);

  @Test
  void reporteUsaIntervaloUtcDelDiaLocal() {
    var desde = LocalDateTime.of(2026, 10, 15, 6, 0);
    var hasta = desde.plusDays(1);
    when(ventas.ganancia(desde, hasta)).thenReturn(BigDecimal.TEN);
    assertEquals(BigDecimal.TEN, service.ganancia(LocalDate.of(2026, 10, 15)).ganancia());
    verify(ventas).ganancia(desde, hasta);
  }
}
