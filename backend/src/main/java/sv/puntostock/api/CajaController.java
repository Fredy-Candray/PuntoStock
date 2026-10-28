package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.CajaContratos.*;
import sv.puntostock.service.CajaService;
import sv.puntostock.service.ConsultasService.Pagina;

@RestController
@RequestMapping("/api/caja")
public class CajaController {
  private final CajaService caja;

  public CajaController(CajaService caja) {
    this.caja = caja;
  }

  @GetMapping("/estado")
  public Estado estado() {
    return caja.estado();
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Pagina<Resumen> historial(@RequestParam(defaultValue = "0") int pagina, Principal p) {
    return caja.historial(pagina, p.getName());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Resumen detalle(@PathVariable long id, Principal p) {
    return caja.detalle(id, p.getName());
  }

  @GetMapping("/{id}/movimientos")
  @PreAuthorize("hasRole('ADMIN')")
  public Pagina<Registro> movimientos(
      @PathVariable long id, @RequestParam(defaultValue = "0") int pagina, Principal p) {
    return caja.movimientos(id, pagina, p.getName());
  }

  @PostMapping("/abrir")
  @PreAuthorize("hasRole('ADMIN')")
  public Resumen abrir(@Valid @RequestBody Apertura in, Principal p) {
    return caja.abrir(in, p.getName());
  }

  @PostMapping("/{id}/movimientos")
  @PreAuthorize("hasRole('ADMIN')")
  public Resumen mover(@PathVariable long id, @Valid @RequestBody Movimiento in, Principal p) {
    return caja.mover(id, in, p.getName());
  }

  @PostMapping("/{id}/cerrar")
  @PreAuthorize("hasRole('ADMIN')")
  public Resumen cerrar(@PathVariable long id, @Valid @RequestBody Cierre in, Principal p) {
    return caja.cerrar(id, in, p.getName());
  }
}
