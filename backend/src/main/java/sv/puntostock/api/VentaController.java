package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.Contratos.*;
import sv.puntostock.service.Tiempo;
import sv.puntostock.service.VentaService;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {
  private final VentaService ventas;
  private final Clock clock;

  public VentaController(VentaService ventas, Clock clock) {
    this.ventas = ventas;
    this.clock = clock;
  }

  @PostMapping
  public VentaResultado vender(@Valid @RequestBody VentaInput in, Principal p) {
    return ventas.vender(in, p.getName());
  }

  @GetMapping
  public HistorialVentas ventas(
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(defaultValue = "0") int pagina,
      Principal p) {
    return ventas.historial(
        fecha == null ? LocalDate.now(clock.withZone(Tiempo.ZONA)) : fecha, pagina, p.getName());
  }

  @GetMapping("/{id}")
  public VentaResultado ticket(@PathVariable Long id, Principal p) {
    return ventas.comprobante(id, p.getName());
  }

  @GetMapping("/solicitud/{id}")
  public VentaResultado solicitud(@PathVariable UUID id, Principal p) {
    return ventas.solicitud(id, p.getName());
  }
}
