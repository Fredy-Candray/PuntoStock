package sv.puntostock.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias_productos")
public class CategoriaProducto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @org.hibernate.annotations.Nationalized
  @Column(nullable = false, length = 80, unique = true)
  public String nombre;

  @Column(nullable = false)
  public boolean activo = true;

  @Version public long version;
}
