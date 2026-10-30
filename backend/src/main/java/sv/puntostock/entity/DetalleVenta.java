package sv.puntostock.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "venta_id")
  public Venta venta;

  @ManyToOne(optional = false)
  @JoinColumn(name = "producto_id")
  public Producto producto;

  public int cantidad;

  @Column(nullable = false, length = 10)
  public String presentacion = "UNIDAD";

  @Column(name = "unidades_por_presentacion", nullable = false)
  public int unidadesPorPresentacion = 1;

  @org.hibernate.annotations.Nationalized
  @Column(name = "nombre_producto", length = 160)
  public String nombreProducto;

  @org.hibernate.annotations.Nationalized
  @Column(name = "sku_producto", length = 80)
  public String skuProducto;

  @org.hibernate.annotations.Nationalized
  @Column(length = 200)
  public String observaciones;

  @Column(name = "costo_unitario", precision = 19, scale = 2)
  public BigDecimal costoUnitario;

  @Column(name = "precio_unitario", precision = 19, scale = 2)
  public BigDecimal precioUnitario;
}
