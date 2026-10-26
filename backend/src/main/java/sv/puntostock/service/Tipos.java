package sv.puntostock.service;

/** Nombres estables que coinciden con las restricciones SQL y el contrato público. */
public final class Tipos {
  private Tipos() {}

  public enum MedioPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA
  }

  public enum TipoProducto {
    PRODUCTO,
    SERVICIO
  }

  public enum TipoMovimiento {
    COMPRA,
    DEVOLUCION_CLIENTE,
    DEVOLUCION_PROVEEDOR,
    MERMA,
    AJUSTE,
    VENTA
  }
}
