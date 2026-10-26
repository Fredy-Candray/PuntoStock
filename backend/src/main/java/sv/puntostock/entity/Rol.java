package sv.puntostock.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "roles")
public class Rol {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 20)
  public String nombre;
}
