package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.Contratos.MovimientoInput;
import sv.puntostock.service.ConsultasService;
import sv.puntostock.service.InventarioService;

@RestController
@RequestMapping("/api/inventario/movimientos")
@PreAuthorize("hasRole('ADMIN')")
public class InventarioController {
  private final InventarioService inventario;
  private final ConsultasService consultas;

  public InventarioController(InventarioService inventario, ConsultasService consultas) {
    this.inventario = inventario;
    this.consultas = consultas;
  }

  @GetMapping
  public List<ConsultasService.MovimientoVista> movimientos() {
    return consultas.ultimosMovimientos();
  }

  @PostMapping
  public void mover(@Valid @RequestBody MovimientoInput in, Principal p) {
    inventario.mover(in, p.getName());
  }

  @GetMapping("/pagina")
  public ConsultasService.Pagina<ConsultasService.MovimientoVista> pagina(
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @RequestParam(required = false) Long productoId,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "30") int tamano) {
    return consultas.movimientos(desde, hasta, productoId, pagina, tamano);
  }
}
