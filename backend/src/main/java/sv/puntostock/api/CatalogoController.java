package sv.puntostock.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.Contratos.ProductoInput;
import sv.puntostock.service.CatalogoService;
import sv.puntostock.service.ConsultasService;

@RestController
@RequestMapping("/api/productos")
public class CatalogoController {
  public record IdRespuesta(Long id) {}

  private final CatalogoService catalogo;
  private final ConsultasService consultas;

  public CatalogoController(CatalogoService catalogo, ConsultasService consultas) {
    this.catalogo = catalogo;
    this.consultas = consultas;
  }

  private boolean admin(Authentication actor) {
    return actor.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
  }

  @GetMapping
  public List<ConsultasService.ProductoVista> productos(Authentication actor) {
    return consultas.catalogo(admin(actor));
  }

  @GetMapping("/pagina")
  public ConsultasService.Pagina<ConsultasService.ProductoVista> pagina(
      @RequestParam(defaultValue = "") String buscar,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) String categoria,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "30") int tamano,
      Authentication actor) {
    return consultas.productos(buscar, tipo, categoria, pagina, tamano, admin(actor));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public IdRespuesta crear(@Valid @RequestBody ProductoInput in) {
    return new IdRespuesta(catalogo.guardar(null, in).id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public IdRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody ProductoInput in) {
    return new IdRespuesta(catalogo.guardar(id, in).id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void eliminar(@PathVariable Long id) {
    catalogo.eliminar(id);
  }
}
