package sv.puntostock.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
@Entity @Table(name="ventas")
public class Venta {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,columnDefinition="datetime2(6)") public LocalDateTime fecha;
 @ManyToOne(optional=false) @JoinColumn(name="usuario_id") public Usuario usuario;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal total=BigDecimal.ZERO;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal ganancia=BigDecimal.ZERO;
 @OneToMany(mappedBy="venta",cascade=CascadeType.ALL) public List<DetalleVenta> detalles=new ArrayList<>();
}
