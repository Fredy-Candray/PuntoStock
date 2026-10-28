package sv.puntostock.application;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CajaContratos {
  private CajaContratos() {}

  public record Apertura(
      @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal inicial,
      @NotNull @Size(max = 500) String nota,
      @NotNull UUID solicitudId) {}

  public record Movimiento(
      @NotNull @Pattern(regexp = "ENTRADA|SALIDA") String tipo,
      @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal monto,
      @NotBlank @Size(max = 500) String motivo,
      @NotNull UUID solicitudId) {}

  public record Cierre(
      @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal contado,
      @NotNull @Size(max = 500) String nota,
      @NotNull UUID solicitudId,
      @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal esperado,
      @Min(0) long ventas) {}

  public record Estado(boolean abierta, Long id) {}

  public record Resumen(
      long id,
      OffsetDateTime apertura,
      String abiertoPor,
      BigDecimal inicial,
      String notaApertura,
      OffsetDateTime cierre,
      String cerradoPor,
      BigDecimal contado,
      BigDecimal diferencia,
      String notaCierre,
      BigDecimal efectivo,
      BigDecimal tarjeta,
      BigDecimal transferencia,
      long ventas,
      BigDecimal entradas,
      BigDecimal salidas,
      BigDecimal esperado) {}

  public record Registro(
      String id,
      OffsetDateTime fecha,
      String usuario,
      String tipo,
      String motivo,
      BigDecimal monto,
      String metodoPago) {}
}
