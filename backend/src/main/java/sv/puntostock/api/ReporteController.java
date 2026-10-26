package sv.puntostock.api;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {
  private final ReporteService servicio;

  public ReporteController(ReporteService servicio) {
    this.servicio = servicio;
  }

  @GetMapping("/ganancia")
  public ReporteService.Ganancia ganancia(@RequestParam LocalDate fecha) {
    return servicio.ganancia(fecha);
  }
}
