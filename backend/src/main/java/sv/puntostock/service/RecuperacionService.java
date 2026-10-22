package sv.puntostock.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class RecuperacionService {
 private final UsuarioRepository usuarios;
 private final PasswordEncoder passwords;
 private final ObjectProvider<JavaMailSender> mail;
 private final boolean enabled;
 private final String from,baseUrl;
 public RecuperacionService(UsuarioRepository usuarios,PasswordEncoder passwords,ObjectProvider<JavaMailSender> mail,
  @Value("${app.recovery.enabled:false}") boolean enabled,@Value("${app.recovery.from:}") String from,
  @Value("${app.recovery.base-url:http://127.0.0.1:14200}") String baseUrl){
  this.usuarios=usuarios;this.passwords=passwords;this.mail=mail;this.enabled=enabled;this.from=from;this.baseUrl=baseUrl;
 }
 static String hash(String token){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 @Transactional public void solicitar(String username,String correo){
  var sender=mail.getIfAvailable();
  if(!enabled||sender==null||from.isBlank())throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Recuperación por correo aún no configurada");
  var usuario=usuarios.bloquear(username.trim()).filter(u->u.activo&&u.correo!=null&&u.correo.equalsIgnoreCase(correo.trim()));
  if(usuario.isEmpty())return;
  var u=usuario.get();var now=LocalDateTime.now(Clock.systemUTC());
  if(u.recuperacionSolicitada!=null&&u.recuperacionSolicitada.plusMinutes(2).isAfter(now))return;
  byte[] random=new byte[32];new SecureRandom().nextBytes(random);
  String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
  u.recuperacionHash=hash(token);u.recuperacionExpira=now.plusMinutes(15);u.recuperacionSolicitada=now;
  var message=new SimpleMailMessage();message.setFrom(from);message.setTo(u.correo);
  message.setSubject("Restablece tu contraseña de PuntoStock");
  String link=baseUrl.replaceAll("/+$","")+"/login#recuperar="+token+"&usuario="+java.net.URLEncoder.encode(u.username,StandardCharsets.UTF_8);
  message.setText("Solicitaste restablecer tu contraseña de PuntoStock.\n\nAbre este enlace, válido por 15 minutos y de un solo uso:\n"+link+"\n\nSi no lo solicitaste, ignora este correo. Tu contraseña no ha cambiado.");
  try{sender.send(message);}catch(org.springframework.mail.MailException e){throw new ResponseStatusException(SERVICE_UNAVAILABLE,"No se pudo enviar el correo. Inténtalo más tarde.");}
 }
 @Transactional public void restablecer(String username,String token,String password){
  if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(BAD_REQUEST,"La contraseña excede 72 bytes");
  var u=usuarios.bloquear(username.trim()).filter(x->x.activo).orElseThrow(RecuperacionService::invalido);
  if(u.recuperacionHash==null||u.recuperacionExpira==null||!u.recuperacionExpira.isAfter(LocalDateTime.now(Clock.systemUTC()))||
   !MessageDigest.isEqual(u.recuperacionHash.getBytes(StandardCharsets.UTF_8),hash(token).getBytes(StandardCharsets.UTF_8)))throw invalido();
  u.passwordHash=passwords.encode(password);u.authVersion++;u.recuperacionHash=null;u.recuperacionExpira=null;
 }
 private static ResponseStatusException invalido(){return new ResponseStatusException(BAD_REQUEST,"El enlace es inválido o venció. Solicita uno nuevo.");}
}
