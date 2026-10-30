package sv.puntostock.application;

import jakarta.validation.constraints.*;

public final class CategoriaProductoContratos {
  private CategoriaProductoContratos() {}

  public record Entrada(
      @NotBlank @Size(max = 80) String nombre, @NotNull Boolean activo, @Min(0) Long version) {}

  public record Vista(Long id, String nombre, boolean activo, long version) {}
}
