package sv.puntostock.api;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {
 private final UsuarioRepository usuarios=mock(UsuarioRepository.class);
 private final PasswordEncoder passwords=mock(PasswordEncoder.class);
 private final JwtEncoder encoder=mock(JwtEncoder.class);
 private final AuthController controller=new AuthController(usuarios,mock(RolRepository.class),passwords,encoder);

 private void preparar(){
  var usuario=new Usuario();usuario.username="test";usuario.activo=true;usuario.passwordHash="hash";
  when(usuarios.findByUsername("test")).thenReturn(Optional.of(usuario));
  when(passwords.matches("test-password","hash")).thenReturn(true);
  when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("test-token").header("alg","HS256")
   .subject("test").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build());
 }
 private void comprobarDuracion(boolean recordar,long segundos){
  preparar();
  var result=controller.login(new Contratos.Login("test","test-password",recordar));
  assertEquals(segundos,result.get("expiresIn"));
  var captured=ArgumentCaptor.forClass(JwtEncoderParameters.class);verify(encoder).encode(captured.capture());
  var claims=captured.getValue().getClaims();
  assertEquals(segundos,Duration.between(claims.getIssuedAt(),claims.getExpiresAt()).getSeconds());
 }
 @Test void sesionNormalDuraQuinceMinutos(){comprobarDuracion(false,900);}
 @Test void sesionRecordadaDuraSieteDias(){comprobarDuracion(true,604800);}
 @Test void recordarNoPermiteIngresarSinCredencialesValidas(){
  preparar();when(passwords.matches("test-password","hash")).thenReturn(false);
  assertThrows(ResponseStatusException.class,()->controller.login(new Contratos.Login("test","test-password",true)));
  verifyNoInteractions(encoder);
 }
}
