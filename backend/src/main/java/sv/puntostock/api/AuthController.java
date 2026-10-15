package sv.puntostock.api;
import sv.puntostock.api.Contratos.*;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api")
public class AuthController {
 private final UsuarioRepository usuarios;private final RolRepository roles;private final PasswordEncoder passwords;private final JwtEncoder encoder;
 public AuthController(UsuarioRepository u,RolRepository r,PasswordEncoder p,JwtEncoder e){usuarios=u;roles=r;passwords=p;encoder=e;}
 @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody Login in){
  var u=usuarios.findByUsername(in.username()).filter(x->x.activo).orElseThrow(()->new ResponseStatusException(UNAUTHORIZED,"Credenciales inválidas"));
  if(!passwords.matches(in.password(),u.passwordHash))throw new ResponseStatusException(UNAUTHORIZED,"Credenciales inválidas");
  var now=Instant.now();var claims=JwtClaimsSet.builder().issuer("puntostock").subject(u.username).issuedAt(now).expiresAt(now.plusSeconds(900)).build();
  var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
  return Map.of("accessToken",token,"expiresIn",900,"roles",u.roles.stream().map(r->r.nombre).toList());
 }
 @GetMapping("/usuarios") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> listar(){return usuarios.findAll().stream().map(u->Map.<String,Object>of("id",u.id,"username",u.username,"activo",u.activo,"roles",u.roles.stream().map(r->r.nombre).toList())).toList();}
 @PostMapping("/usuarios") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Map<String,Long> crear(@Valid @RequestBody UsuarioInput in){
  if(usuarios.findByUsername(in.username()).isPresent())throw new ResponseStatusException(CONFLICT,"Usuario existente");
  if(in.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(BAD_REQUEST,"Contraseña excede 72 bytes");
  var u=new Usuario();u.username=in.username();u.passwordHash=passwords.encode(in.password());u.roles.add(roles.findByNombre(in.rol()).orElseThrow());return Map.of("id",usuarios.save(u).id);
 }
 @PutMapping("/usuarios/{id}/activo") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public void activo(@PathVariable Long id,@RequestParam boolean valor,java.security.Principal actor){
  var u=usuarios.findById(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND));
  if(u.username.equals(actor.getName())&&!valor)throw new ResponseStatusException(CONFLICT,"No puede desactivar su propia cuenta");u.activo=valor;
 }
}
