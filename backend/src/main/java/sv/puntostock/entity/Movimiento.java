package sv.puntostock.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "movimientos_inventario")
public class Movimiento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "producto_id")
  public Producto producto;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  @ManyToOne
  @JoinColumn(name = "venta_id")
  public Venta venta;

  @Column(name = "pedido_id")
  public Long pedidoId;

  @Column(name = "solicitud_consumo")
  public UUID solicitudConsumo;

  @org.hibernate.annotations.Nationalized
  @Column(name = "nombre_insumo", length = 160)
  public String nombreInsumo;

  @org.hibernate.annotations.Nationalized
  @Column(name = "unidad_insumo", length = 80)
  public String unidadInsumo;

  @Column(name = "costo_unitario_insumo", precision = 19, scale = 2)
  public BigDecimal costoUnitarioInsumo;

  @Column(columnDefinition = "datetime2(6)")
  public LocalDateTime fecha;

  @Column(length = 40)
  public String tipo;

  public int cantidad;

  @Column(name = "stock_anterior")
  public int stockAnterior;

  @Column(name = "stock_posterior")
  public int stockPosterior;

  @org.hibernate.annotations.Nationalized
  @Column(length = 500)
  public String motivo;
}
