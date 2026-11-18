package sv.puntostock.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import sv.puntostock.service.Tipos.MedioPago;

public final class PedidoContratos {
  private PedidoContratos() {}

  public record Crear(
      @NotBlank @Size(max = 160) String cliente,
      @NotBlank @Size(max = 500) String descripcion,
      @NotNull @Size(max = 1000) String servicios,
      @Min(1) @Max(1000000) int cantidad,
      @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal precioUnitario,
      @NotNull LocalDate entrega,
      @NotNull @Valid Abonar anticipo) {}

  public record Abonar(
      @NotNull UUID solicitudId,
      @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal monto,
      @NotNull MedioPago metodoPago) {}

  public record Editar(
      @NotBlank @Size(max = 160) String cliente,
      @NotBlank @Size(max = 500) String descripcion,
      @NotNull @Size(max = 1000) String servicios,
      @Min(1) @Max(1000000) int cantidad,
      @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal precioUnitario,
      @NotNull LocalDate entrega) {}

  public record Consumir(
      @NotNull UUID solicitudId, @Min(1) long productoId, @Min(1) int cantidad) {}

  public enum Estado {
    PENDIENTE,
    LISTO,
    ENTREGADO,
    CANCELADO
  }

  public record CambiarEstado(@NotNull Estado estado, Long ultimoAbono, Long ultimoConsumo) {}

  public record Resumen(
      long id,
      String cliente,
      String descripcion,
      String servicios,
      int cantidad,
      BigDecimal precioUnitario,
      BigDecimal total,
      BigDecimal saldo,
      LocalDate entrega,
      Estado estado) {}

  public record Abono(
      long id, OffsetDateTime fecha, BigDecimal monto, BigDecimal saldo, MedioPago metodoPago) {}

  public record Consumo(
      long id,
      String nombre,
      String unidad,
      int cantidad,
      OffsetDateTime fecha,
      BigDecimal costoUnitario,
      BigDecimal costoTotal) {}

  public record Detalle(
      Resumen pedido, List<Abono> abonos, List<Consumo> materiales, BigDecimal costoMateriales) {}
}
