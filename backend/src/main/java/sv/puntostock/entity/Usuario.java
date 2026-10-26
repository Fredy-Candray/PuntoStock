package sv.puntostock.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @org.hibernate.annotations.Nationalized
  @Column(nullable = false, unique = true, length = 80)
  public String username;

  @Column(name = "password_hash", nullable = false, length = 100)
  public String passwordHash;

  public boolean activo = true;

  @Column(name = "auth_version", nullable = false)
  public long authVersion = 0;

  @Column(name = "recuperacion_hash", length = 64)
  public String recuperacionHash;

  @Column(name = "recuperacion_expira")
  public LocalDateTime recuperacionExpira;

  @Column(name = "recuperacion_solicitada")
  public LocalDateTime recuperacionSolicitada;

  @org.hibernate.annotations.Nationalized
  @Column(length = 160)
  public String nombre;

  @org.hibernate.annotations.Nationalized
  @Column(length = 254)
  public String correo;

  @org.hibernate.annotations.Nationalized
  @Column(length = 30)
  public String telefono;

  @Column(name = "foto_perfil", columnDefinition = "varchar(max)")
  public String fotoPerfil;

  @Column(name = "foto_portada", columnDefinition = "varchar(max)")
  public String fotoPortada;

  @Column(name = "fecha_creacion")
  public LocalDateTime fechaCreacion;

  @PrePersist
  void registrarFecha() {
    if (fechaCreacion == null) fechaCreacion = LocalDateTime.now(java.time.Clock.systemUTC());
  }

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "usuario_roles",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "rol_id"))
  public Set<Rol> roles = new HashSet<>();
}
