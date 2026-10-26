package sv.puntostock.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class Contratos {
  public record ProductoInput(
      @NotBlank @Size(max = 80) String sku,
      @NotBlank @Size(max = 160) String nombre,
      @NotBlank @Size(max = 2000) String descripcion,
      @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal precioCompra,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal precioVenta,
      @Size(max = 1000) String fotoUrl,
      @Pattern(regexp = "PRODUCTO|SERVICIO") String tipo,
      @Size(max = 80) String categoria,
      @Size(max = 80) String marca,
      @Pattern(regexp = "UNIDAD|PAQUETE|CAJA|RESMA|DOCENA|PAGINA|FOTOGRAFIA|TRABAJO") String unidad,
      @Size(max = 80) String ubicacion,
      @Min(0) @Max(1000000) Integer stockMinimo) {
    public ProductoInput(
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        String fotoUrl) {
      this(
          sku,
          nombre,
          descripcion,
          precioCompra,
          precioVenta,
          fotoUrl,
          null,
          null,
          null,
          null,
          null,
          null);
    }
  }

  public record Linea(
      @NotNull @Positive Long productoId,
      @Min(1) @Max(100000) int cantidad,
      @Size(max = 200) String observaciones) {
    public Linea(Long productoId, int cantidad) {
      this(productoId, cantidad, null);
    }
  }

  public record VentaInput(
      @NotEmpty @Size(max = 100) List<@Valid Linea> lineas,
      @Pattern(regexp = "EFECTIVO|TARJETA|TRANSFERENCIA") String metodoPago,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal recibido,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal totalEsperado,
      @Size(max = 160) String cliente,
      @Size(max = 80) String referenciaPago,
      java.util.UUID solicitudId) {
    public VentaInput(List<Linea> lineas) {
      this(lineas, null, null, null, null, null, null);
    }
  }

  public record TicketLinea(
      Long productoId,
      String sku,
      String nombre,
      int cantidad,
      BigDecimal precioUnitario,
      BigDecimal subtotal,
      String observaciones) {}

  public record VentaResultado(
      Long id,
      BigDecimal total,
      java.time.OffsetDateTime fecha,
      String vendedor,
      String metodoPago,
      BigDecimal recibido,
      BigDecimal cambio,
      String cliente,
      String referenciaPago,
      List<TicketLinea> lineas) {}

  public record VentaResumen(
      Long id,
      java.time.OffsetDateTime fecha,
      String vendedor,
      BigDecimal total,
      String metodoPago,
      String cliente) {}

  public record HistorialVentas(
      List<VentaResumen> ventas,
      long cantidad,
      int pagina,
      int paginas,
      BigDecimal total,
      BigDecimal efectivo,
      BigDecimal tarjeta,
      BigDecimal transferencia,
      BigDecimal sinRegistro) {}

  public record MovimientoInput(
      @NotNull @Positive Long productoId,
      @NotBlank String tipo,
      int cantidad,
      @NotBlank @Size(max = 500) String motivo) {}

  public record Login(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Size(max = 72) String password,
      Boolean rememberMe) {
    public Login {
      rememberMe = Boolean.TRUE.equals(rememberMe);
    }
  }

  public record PerfilInput(
      @NotBlank @Size(max = 160) String nombre,
      @NotBlank @Email @Size(max = 254) String correo,
      @Size(max = 30) @Pattern(regexp = "[+0-9 ()-]*") String telefono) {}

  public record FotoInput(@NotNull @Size(max = 1_500_000) String imagen) {}

  public record UsuarioInput(
      @NotBlank @Size(max = 80) String username,
      @Size(max = 72) String password,
      @Pattern(regexp = "ADMIN|VENDEDOR") @NotNull String rol,
      @Size(max = 160) String nombre,
      @Email @Size(max = 254) String correo,
      @Size(max = 30) @Pattern(regexp = "[+0-9 ()-]*") String telefono,
      Boolean activo) {
    public UsuarioInput(String username, String password, String rol) {
      this(username, password, rol, null, null, null, true);
    }
  }
}
