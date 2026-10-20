package sv.puntostock.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.*;
import sv.puntostock.repository.UsuarioRepository;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerfilControllerTest {
 private final UsuarioRepository usuarios=mock(UsuarioRepository.class);
 private final PerfilController controller=new PerfilController(usuarios);
 private Usuario usuario(){
  var u=new Usuario();u.id=5L;u.username="vendedor";u.passwordHash="no-exponer";
  var r=new Rol();r.nombre="VENDEDOR";u.roles.add(r);
  when(usuarios.findByUsername("vendedor")).thenReturn(Optional.of(u));return u;
 }
 @Test void consultaSoloLaCuentaAutenticadaSinHash(){
  usuario();var p=controller.obtener(()->"vendedor");assertEquals(5L,p.id());assertEquals("vendedor",p.nombre());
  assertEquals(java.util.List.of("VENDEDOR"),p.roles());
  assertTrue(java.util.Arrays.stream(p.getClass().getRecordComponents()).noneMatch(c->c.getName().toLowerCase().contains("password")));
  verify(usuarios).findByUsername("vendedor");
 }
 @Test void editarDatosNoCambiaCredencialesNiRoles(){
  var u=usuario();var p=controller.actualizar(new Contratos.PerfilInput("  Ana Pérez "," ana@example.test "," 7777-7777 "),()->"vendedor");
  assertEquals("Ana Pérez",p.nombre());assertEquals("ana@example.test",p.correo());assertEquals("7777-7777",p.telefono());
  assertEquals("vendedor",u.username);assertEquals("no-exponer",u.passwordHash);assertEquals("VENDEDOR",u.roles.iterator().next().nombre);
 }
 @Test void rechazaCuentaInactiva(){var u=usuario();u.activo=false;assertThrows(ResponseStatusException.class,()->controller.obtener(()->"vendedor"));}
 @Test void fotoInvalidaNoModificaLaGuardada(){var u=usuario();u.fotoPerfil="foto-anterior";assertThrows(ResponseStatusException.class,()->controller.foto(new Contratos.FotoInput("data:image/svg+xml;base64,PHN2Zz4="),()->"vendedor"));assertEquals("foto-anterior",u.fotoPerfil);}
}
