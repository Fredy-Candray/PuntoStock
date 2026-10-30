package sv.puntostock.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "producto_imagenes")
public class ProductoImagen {
  @Id
  @Column(name = "producto_id")
  public Long productoId;

  @Column(nullable = false, length = 1048576, columnDefinition = "varbinary(max)")
  public byte[] contenido;

  @Column(nullable = false, length = 20)
  public String tipo;

  @Column(nullable = false)
  public UUID revision;
}
