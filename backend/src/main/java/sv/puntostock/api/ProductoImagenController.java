package sv.puntostock.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.service.ProductoImagenService;

@RestController
@RequestMapping("/api/productos")
public class ProductoImagenController {
  private final ProductoImagenService imagenes;

  public ProductoImagenController(ProductoImagenService imagenes) {
    this.imagenes = imagenes;
  }

  @GetMapping("/{id}/imagen")
  public ResponseEntity<byte[]> consultar(@PathVariable Long id) {
    var i = imagenes.consultar(id);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(i.tipo()))
        .body(i.bytes());
  }
}
