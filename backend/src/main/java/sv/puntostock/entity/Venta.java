package sv.puntostock.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "ventas")
public class Venta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, columnDefinition = "datetime2(6)")
  public LocalDateTime fecha;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  @Column(nullable = false, precision = 19, scale = 2)
  public BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 2)
  public BigDecimal ganancia = BigDecimal.ZERO;

  @Column(name = "metodo_pago", nullable = false, length = 20)
  public String metodoPago = "EFECTIVO";

  @Column(precision = 19, scale = 2)
  public BigDecimal recibido;

  @Column(precision = 19, scale = 2)
  public BigDecimal cambio;

  @org.hibernate.annotations.Nationalized
  @Column(length = 160)
  public String cliente;

  @org.hibernate.annotations.Nationalized
  @Column(name = "referencia_pago", length = 80)
  public String referenciaPago;

  @Column(name = "solicitud_id")
  public UUID solicitudId;

  @Column(name = "firma_solicitud", length = 64)
  public String firmaSolicitud;

  @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
  public List<DetalleVenta> detalles = new ArrayList<>();
}
