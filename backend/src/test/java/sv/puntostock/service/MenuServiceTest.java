package sv.puntostock.service;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;
import tools.jackson.databind.ObjectMapper;
class MenuServiceTest {
 MenuService.Opcion opcion(String id,String ruta,List<String> roles){return new MenuService.Opcion(id,"Opción",ruta,"grid",1,true,roles);}
 List<MenuService.Seccion> menu(List<MenuService.Opcion> opciones){return List.of(new MenuService.Seccion("seccion","Sistema",1,true,opciones));}
 MenuService.Opcion admin(){return opcion("admin","/administrar-menu",List.of("ADMIN"));}
 @Test void aceptaMenuCompatible(){assertDoesNotThrow(()->MenuService.validar(menu(List.of(admin(),opcion("pos","/pos",MenuService.ROLES)))));}
 @Test void impideEliminarAccesoAdministrador(){assertThrows(ResponseStatusException.class,()->MenuService.validar(menu(List.of(opcion("pos","/pos",MenuService.ROLES)))));}
 @Test void impideOcultarLaSeccionDeAdministracion(){assertThrows(ResponseStatusException.class,()->MenuService.validar(List.of(new MenuService.Seccion("s","Sistema",1,false,List.of(admin())))));}
 @Test void impideAsignarPantallasAdministrativasAlVendedor(){assertThrows(ResponseStatusException.class,()->MenuService.validar(menu(List.of(admin(),opcion("usuarios","/usuarios",MenuService.ROLES)))));}
 @Test void rechazaRutasExternasYDesconocidas(){for(String ruta:List.of("https://example.com","//example.com","/desconocida","/../usuarios"))assertThrows(ResponseStatusException.class,()->MenuService.validar(menu(List.of(admin(),opcion("otra",ruta,MenuService.ROLES)))));}
 @Test void rechazaDuplicados(){
  var duplicado=opcion("repetido","/administrar-menu",List.of("ADMIN"));
  assertThrows(ResponseStatusException.class,()->MenuService.validar(menu(List.of(admin(),duplicado))));
 }
 @Test void rechazaOrdenInvalido(){assertThrows(ResponseStatusException.class,()->MenuService.validar(List.of(new MenuService.Seccion("s","Sistema",0,true,List.of(admin())))));}
 @Test void configuracionInicialDeLaMigracionSeLeeYFiltraPorRol()throws Exception{
  String sql=java.nio.file.Files.readString(java.nio.file.Path.of("../database/006_menu_administrable.sql"));
  var matcher=java.util.regex.Pattern.compile("VALUES\\(1,N'(.*)',0\\);",java.util.regex.Pattern.DOTALL).matcher(sql);assertTrue(matcher.find());
  var config=new MenuConfiguracion();config.id=1;config.contenido=matcher.group(1);
  var repository=mock(MenuConfiguracionRepository.class);var usuarios=mock(UsuarioRepository.class);when(repository.findById(1)).thenReturn(Optional.of(config));
  var u=new Usuario();u.username="vendedor";var rol=new Rol();rol.nombre="VENDEDOR";u.roles.add(rol);when(usuarios.findByUsername("vendedor")).thenReturn(Optional.of(u));
  var service=new MenuService(repository,usuarios,new ObjectMapper());
  assertDoesNotThrow(()->MenuService.validar(service.configuracion().secciones()));
  assertEquals(List.of("/pos","/ventas"),service.menu("vendedor").stream().flatMap(s->s.opciones().stream()).map(MenuService.Opcion::ruta).toList());
 }
 @Test void noSobrescribeUnaConfiguracionConVersionObsoleta(){
  var r=new MenuConfiguracion();r.id=1;r.version=2;r.contenido="anterior";
  var repository=mock(MenuConfiguracionRepository.class);when(repository.findById(1)).thenReturn(Optional.of(r));
  var service=new MenuService(repository,mock(UsuarioRepository.class),new ObjectMapper());
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.guardar(new MenuService.Guardar(1L,menu(List.of(admin()))))).getStatusCode().value());
  assertEquals("anterior",r.contenido);verify(repository,never()).saveAndFlush(any());
 }
}
