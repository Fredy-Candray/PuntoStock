package sv.puntostock.application;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CuentasContratos {
  private CuentasContratos() {}

  public enum Ambito {
    PERSONAL,
    NEGOCIO
  }

  public enum Tipo {
    APORTE,
    RETIRO,
    TRASPASO,
    INGRESO,
    GASTO,
    COMPRA,
    PAGO_COMPRA
  }

  public record Alta(
      @NotBlank @Size(max = 100) String nombre,
      @NotNull Ambito ambito,
      @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal inicial,
      @NotNull UUID solicitud) {}

  public record Operacion(
      @NotNull Tipo tipo,
      @NotNull UUID solicitud,
      @NotBlank @Size(max = 250) String concepto,
      @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal monto,
      @Positive Long origen,
      @Positive Long destino,
      @Positive Long cajaId,
      @Positive Long productoId,
      @Positive Integer cantidad,
      @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal pagadoInicial,
      @Positive Long compraId) {}

  public record Cuenta(long id, String nombre, Ambito ambito, BigDecimal saldo) {}

  public record Movimiento(
      long id,
      OffsetDateTime fecha,
      Tipo tipo,
      String concepto,
      BigDecimal monto,
      String origen,
      String destino,
      Long cajaId,
      Integer cantidad,
      BigDecimal pagadoInicial,
      Long compraId) {}

  public record Deuda(long id, String proveedor, BigDecimal pendiente) {}

  public record Cobro(long id, String cliente, BigDecimal pendiente) {}

  public record Resultado(
      BigDecimal ventas,
      BigDecimal gananciaBruta,
      BigDecimal otrosIngresos,
      BigDecimal gastos,
      BigDecimal pedidosEntregados,
      BigDecimal costoPedidos,
      long pedidosSinCosto,
      BigDecimal resultadoEstimado,
      BigDecimal compras,
      BigDecimal inventario,
      long entregadosSinFecha,
      BigDecimal totalPorPagar,
      BigDecimal totalPorCobrar) {}
}
