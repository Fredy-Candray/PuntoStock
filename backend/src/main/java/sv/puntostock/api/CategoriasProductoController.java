package sv.puntostock.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.CategoriaProductoContratos.*;
import sv.puntostock.service.CategoriasProductoService;

@RestController
@RequestMapping("/api/productos/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriasProductoController {
  private final CategoriasProductoService service;

  public CategoriasProductoController(CategoriasProductoService service) {
    this.service = service;
  }

  @GetMapping
  public List<Vista> listar() {
    return service.listar();
  }

  @PostMapping
  public Vista crear(@Valid @RequestBody Entrada in) {
    return service.guardar(null, in);
  }

  @PutMapping("/{id}")
  public Vista actualizar(@PathVariable Long id, @Valid @RequestBody Entrada in) {
    return service.guardar(id, in);
  }
}
