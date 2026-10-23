package sv.puntostock.entity;
import jakarta.persistence.*;
@Entity @Table(name="menu_configuracion")
public class MenuConfiguracion {
 @Id public Integer id;
 @org.hibernate.annotations.Nationalized @Column(nullable=false,columnDefinition="nvarchar(max)") public String contenido;
 @Version public long version;
}
