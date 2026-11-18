package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.CuentasContratos.*;
import sv.puntostock.service.CuentasService;

@RestController
@RequestMapping("/api/cuentas")
@PreAuthorize("hasRole('ADMIN')")
public class CuentasController {
  private final CuentasService cuentas;

  public CuentasController(CuentasService cuentas) {
    this.cuentas = cuentas;
  }

  @GetMapping
  public Object consultar(
      @RequestParam LocalDate desde,
      @RequestParam LocalDate hasta,
      @RequestParam(defaultValue = "0") int pagina) {
    return cuentas.consultar(desde, hasta, pagina);
  }

  @PostMapping
  public void alta(@Valid @RequestBody Alta in) {
    cuentas.alta(in);
  }

  @PostMapping("/movimientos")
  public void mover(@Valid @RequestBody Operacion in, Principal p) {
    cuentas.mover(in, p.getName());
  }
}
