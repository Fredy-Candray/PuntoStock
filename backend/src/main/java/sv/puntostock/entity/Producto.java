package sv.puntostock.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
@Entity @Table(name="productos")
public class Producto {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @org.hibernate.annotations.Nationalized @Column(nullable=false,length=80,unique=true) public String sku;
 @org.hibernate.annotations.Nationalized @Column(nullable=false,length=160) public String nombre;
 @org.hibernate.annotations.Nationalized @Column(nullable=false,length=2000) public String descripcion;
 @Column(name="precio_compra",nullable=false,precision=19,scale=2) public BigDecimal precioCompra;
 @Column(name="precio_venta",nullable=false,precision=19,scale=2) public BigDecimal precioVenta;
 @Column(nullable=false) public int stock;
 @org.hibernate.annotations.Nationalized @Column(name="foto_url",nullable=false,length=1000) public String fotoUrl;
 @Column(nullable=false) public boolean activo=true;
 @Version public long version;
}
