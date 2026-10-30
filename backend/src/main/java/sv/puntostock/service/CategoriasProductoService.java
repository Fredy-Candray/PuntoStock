package sv.puntostock.service;

import static org.springframework.http.HttpStatus.*;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.CategoriaProductoContratos.*;
import sv.puntostock.entity.CategoriaProducto;
import sv.puntostock.repository.CategoriaProductoRepository;

@Service
@Transactional
public class CategoriasProductoService {
  private final CategoriaProductoRepository categorias;

  public CategoriasProductoService(CategoriaProductoRepository categorias) {
    this.categorias = categorias;
  }

  @Transactional(readOnly = true)
  public List<Vista> listar() {
    return categorias.findAllByOrderByNombreAsc().stream().map(this::vista).toList();
  }

  private Vista vista(CategoriaProducto c) {
    return new Vista(c.id, c.nombre, c.activo, c.version);
  }

  public Vista guardar(Long id, Entrada in) {
    String nombre = in.nombre().strip();
    if (nombre.isBlank())
      throw new ResponseStatusException(BAD_REQUEST, "Escribe el nombre de la categoría.");
    CategoriaProducto c =
        id == null
            ? new CategoriaProducto()
            : categorias
                .bloquear(id)
                .orElseThrow(
                    () -> new ResponseStatusException(NOT_FOUND, "Categoría no disponible."));
    if (id != null && (in.version() == null || in.version() != c.version))
      throw new ResponseStatusException(
          CONFLICT, "La categoría cambió. Actualiza y vuelve a intentarlo.");
    if (categorias.existsByNombreIgnoreCaseAndIdNot(nombre, id == null ? -1L : id))
      throw new ResponseStatusException(CONFLICT, "Ya existe una categoría con ese nombre.");
    c.nombre = nombre;
    c.activo = in.activo();
    return vista(categorias.saveAndFlush(c));
  }

  public String paraProducto(String nombre, String anterior) {
    String buscado = nombre == null || nombre.isBlank() ? "General" : nombre.strip();
    var c =
        categorias
            .bloquearNombre(buscado)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        BAD_REQUEST, "Selecciona una categoría registrada."));
    if (!c.activo && !c.nombre.equals(anterior))
      throw new ResponseStatusException(CONFLICT, "La categoría está inactiva. Selecciona otra.");
    return c.nombre;
  }
}
