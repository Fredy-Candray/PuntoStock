package sv.puntostock.service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.UsuarioRepository;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecuperacionServiceTest {
 final UsuarioRepository usuarios=mock(UsuarioRepository.class);
 final PasswordEncoder passwords=mock(PasswordEncoder.class);
 final JavaMailSender sender=mock(JavaMailSender.class);
 @SuppressWarnings("unchecked") final ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
 final Usuario u=new Usuario();
 RecuperacionService service(){when(provider.getIfAvailable()).thenReturn(sender);return new RecuperacionService(usuarios,passwords,provider,true,"test@example.com","https://example.com");}
 RecuperacionServiceTest(){u.username="ana";u.correo="ana@example.com";u.passwordHash="anterior";when(usuarios.bloquear("ana")).thenReturn(Optional.of(u));}
 @Test void enviaTokenAleatorioPeroGuardaSoloHashYLimitaReenvios(){
  var s=service();s.solicitar("ana","ana@example.com");
  var message=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(message.capture());
  String token=message.getValue().getText().split("#recuperar=")[1].split("&")[0];
  assertEquals(43,token.length());assertEquals(RecuperacionService.hash(token),u.recuperacionHash);
  assertTrue(u.recuperacionExpira.isAfter(LocalDateTime.now(Clock.systemUTC()).plusMinutes(14)));
  s.solicitar("ana","ana@example.com");verify(sender,times(1)).send(any(SimpleMailMessage.class));
 }
 @Test void correoIncorrectoNoEnviaNiModificaCuenta(){service().solicitar("ana","otra@example.com");verifyNoInteractions(sender);assertNull(u.recuperacionHash);}
 @Test void restablecerInvalidaTokenYSesionesPrevias(){
  u.recuperacionHash=RecuperacionService.hash("token");u.recuperacionExpira=LocalDateTime.now(Clock.systemUTC()).plusMinutes(1);
  when(passwords.encode("nueva-password")).thenReturn("hash-nuevo");var s=service();s.restablecer("ana","token","nueva-password");
  assertEquals("hash-nuevo",u.passwordHash);assertEquals(1,u.authVersion);assertNull(u.recuperacionHash);
  assertThrows(ResponseStatusException.class,()->s.restablecer("ana","token","otra-password"));
 }
 @Test void tokenVencidoOIncorrectoNoCambiaClave(){
  u.recuperacionHash=RecuperacionService.hash("correcto");u.recuperacionExpira=LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);var s=service();
  assertThrows(ResponseStatusException.class,()->s.restablecer("ana","correcto","nueva-password"));
  u.recuperacionExpira=LocalDateTime.now(Clock.systemUTC()).plusMinutes(1);
  assertThrows(ResponseStatusException.class,()->s.restablecer("ana","incorrecto","nueva-password"));verifyNoInteractions(passwords);
 }
 @Test void desactivadoNoPuedeRecuperar(){u.activo=false;var s=service();s.solicitar("ana","ana@example.com");verifyNoInteractions(sender);assertThrows(ResponseStatusException.class,()->s.restablecer("ana","token","nueva-password"));}
 @Test void fallaSmtpDevuelveErrorDeDisponibilidad(){doThrow(new MailSendException("fallo")).when(sender).send(any(SimpleMailMessage.class));assertEquals(503,assertThrows(ResponseStatusException.class,()->service().solicitar("ana","ana@example.com")).getStatusCode().value());}
}
