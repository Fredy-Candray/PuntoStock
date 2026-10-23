package sv.puntostock.service;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import sv.puntostock.api.Contratos.UsuarioInput;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UsuariosServiceTest {
 final UsuarioRepository usuarios=mock(UsuarioRepository.class);final RolRepository roles=mock(RolRepository.class);final PasswordEncoder passwords=mock(PasswordEncoder.class);
 final UsuariosService service=new UsuariosService(usuarios,roles,passwords);final Usuario admin=new Usuario(),vendedor=new Usuario();final Rol rolAdmin=new Rol(),rolVendedor=new Rol();
 UsuariosServiceTest(){rolAdmin.nombre="ADMIN";rolVendedor.nombre="VENDEDOR";admin.id=1L;admin.username="admin";admin.roles.add(rolAdmin);vendedor.id=2L;vendedor.username="vendedor";vendedor.passwordHash="anterior";vendedor.roles.add(rolVendedor);
  when(roles.bloquearAdministracion()).thenReturn(Optional.of(rolAdmin));when(usuarios.bloquear("admin")).thenReturn(Optional.of(admin));when(usuarios.findById(1L)).thenReturn(Optional.of(admin));when(usuarios.findById(2L)).thenReturn(Optional.of(vendedor));when(usuarios.findByUsername("vendedor")).thenReturn(Optional.of(vendedor));when(usuarios.findByUsername("admin")).thenReturn(Optional.of(admin));when(roles.findByNombre("ADMIN")).thenReturn(Optional.of(rolAdmin));when(roles.findByNombre("VENDEDOR")).thenReturn(Optional.of(rolVendedor));when(usuarios.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));}
 @Test void impideAutodesactivacion(){assertThrows(ResponseStatusException.class,()->service.estado(1L,false,"admin"));assertTrue(admin.activo);}
 @Test void impideQuitarRolPropio(){assertThrows(ResponseStatusException.class,()->service.guardar(1L,new UsuarioInput("admin","","VENDEDOR"),"admin"));assertTrue(admin.roles.contains(rolAdmin));}
 @Test void cambiarRolInvalidaSesiones(){service.guardar(2L,new UsuarioInput("vendedor","","ADMIN"),"admin");assertEquals(1,vendedor.authVersion);assertTrue(vendedor.roles.contains(rolAdmin));assertEquals("anterior",vendedor.passwordHash);}
 @Test void editarPerfilNoCambiaClaveNiVersion(){service.guardar(2L,new UsuarioInput("vendedor","","VENDEDOR","Nombre","v@example.com",null,true),"admin");assertEquals(0,vendedor.authVersion);assertEquals("anterior",vendedor.passwordHash);}
 @Test void cambiarClaveInvalidaEnlacesYSesiones(){vendedor.recuperacionHash="hash";when(passwords.encode("nueva-clave-123")).thenReturn("nueva-hash");service.guardar(2L,new UsuarioInput("vendedor","nueva-clave-123","VENDEDOR"),"admin");assertEquals(1,vendedor.authVersion);assertEquals("nueva-hash",vendedor.passwordHash);assertNull(vendedor.recuperacionHash);}
 @Test void desactivarYReactivarNoRestauraSesionesViejas(){service.estado(2L,false,"admin");assertFalse(vendedor.activo);service.estado(2L,true,"admin");assertTrue(vendedor.activo);assertEquals(2,vendedor.authVersion);}
 @Test void exigeActorActivoInclusoTrasEsperarBloqueo(){admin.activo=false;assertThrows(ResponseStatusException.class,()->service.estado(2L,false,"admin"));assertTrue(vendedor.activo);}
 @Test void rechazaClavesCortasYMayoresA72Bytes(){assertThrows(ResponseStatusException.class,()->UsuariosService.validarClave("corta",true));assertThrows(ResponseStatusException.class,()->UsuariosService.validarClave("á".repeat(40),false));assertDoesNotThrow(()->UsuariosService.validarClave("",false));}
}
