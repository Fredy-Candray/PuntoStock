package sv.puntostock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.puntostock.repository.VentaRepository;

@Service
@Transactional(readOnly = true)
public class ReporteService {
  public record Ganancia(LocalDate fecha, BigDecimal ganancia, String alcance) {}

  private final VentaRepository ventas;

  public ReporteService(VentaRepository ventas) {
    this.ventas = ventas;
  }

  public Ganancia ganancia(LocalDate dia) {
    return new Ganancia(
        dia,
        ventas.ganancia(Tiempo.inicio(dia), Tiempo.inicio(dia.plusDays(1))),
        "Ganancia bruta: ventas menos costo histórico; no incluye gastos, impuestos ni reembolsos.");
  }
}
