package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.PedidoContratos.*;
import sv.puntostock.service.ConsultasService.Pagina;
import sv.puntostock.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
public class PedidoController {
  private final PedidoService pedidos;

  public PedidoController(PedidoService pedidos) {
    this.pedidos = pedidos;
  }

  @GetMapping
  public Pagina<Resumen> listar(@RequestParam(defaultValue = "0") int pagina) {
    return pedidos.listar(pagina);
  }

  @GetMapping("/{id}")
  public Detalle detalle(@PathVariable long id) {
    return pedidos.detalle(id);
  }

  @PostMapping
  public Detalle crear(@Valid @RequestBody Crear in, Principal p) {
    return pedidos.crear(in, p.getName());
  }

  @PostMapping("/{id}/abonos")
  public Detalle abonar(@PathVariable long id, @Valid @RequestBody Abonar in, Principal p) {
    return pedidos.abonar(id, in, p.getName());
  }

  @PostMapping("/{id}/materiales")
  public Detalle consumir(@PathVariable long id, @Valid @RequestBody Consumir in, Principal p) {
    return pedidos.consumir(id, in, p.getName());
  }

  @PutMapping("/{id}/estado")
  public Detalle estado(@PathVariable long id, @Valid @RequestBody CambiarEstado in) {
    return pedidos.estado(id, in);
  }

  @PutMapping("/{id}")
  public Detalle editar(@PathVariable long id, @Valid @RequestBody Editar in) {
    return pedidos.editar(id, in);
  }
}
