package sv.puntostock.entity;

import jakarta.persistence.*;
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
