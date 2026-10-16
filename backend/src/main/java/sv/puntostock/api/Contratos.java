package sv.puntostock.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public final class Contratos {
 public record ProductoInput(@NotBlank @Size(max=80) String sku,@NotBlank @Size(max=160) String nombre,
  @NotBlank @Size(max=2000) String descripcion,@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal precioCompra,
  @NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal precioVenta,@NotBlank @Size(max=1000) String fotoUrl) {}
 public record Linea(@NotNull @Positive Long productoId,@Min(1) int cantidad) {}
 public record VentaInput(@NotEmpty @Size(max=100) List<@Valid Linea> lineas) {}
 public record VentaResultado(Long id,BigDecimal total) {}
 public record MovimientoInput(@NotNull @Positive Long productoId,@NotBlank String tipo,int cantidad,@NotBlank @Size(max=500) String motivo) {}
 public record Login(@NotBlank String username,@NotBlank String password,boolean rememberMe) {}
 public record UsuarioInput(@NotBlank @Size(max=80) String username,@NotBlank @Size(min=12,max=72) String password,
  @Pattern(regexp="ADMIN|VENDEDOR") @NotNull String rol) {}
}
