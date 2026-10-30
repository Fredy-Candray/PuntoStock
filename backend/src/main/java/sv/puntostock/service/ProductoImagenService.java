package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.ProductoImagen;
import sv.puntostock.repository.*;

@Service
@Transactional(readOnly = true)
public class ProductoImagenService {
  private final ProductoImagenRepository imagenes;
  private final ProductoRepository productos;

  public ProductoImagenService(ProductoImagenRepository imagenes, ProductoRepository productos) {
    this.imagenes = imagenes;
    this.productos = productos;
  }

  public record Contenido(byte[] bytes, String tipo) {}

  public Contenido consultar(Long producto) {
    if (!productos.existsByIdAndActivoTrue(producto)) throw new ResponseStatusException(NOT_FOUND);
    var i = imagenes.findById(producto).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    return new Contenido(i.contenido, i.tipo);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public String guardar(Long producto, String data) {
    String validada = FotoPerfilValidator.validar(data);
    if (validada == null)
      throw new ResponseStatusException(BAD_REQUEST, "Selecciona una imagen válida.");
    var i = new ProductoImagen();
    i.productoId = producto;
    i.tipo = validada.startsWith("data:image/png;") ? "image/png" : "image/jpeg";
    i.contenido = Base64.getDecoder().decode(validada.substring(validada.indexOf(',') + 1));
    i.revision = UUID.randomUUID();
    imagenes.save(i);
    return "/api/productos/" + producto + "/imagen?v=" + i.revision;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void quitar(Long producto) {
    imagenes.deleteById(producto);
  }
}
