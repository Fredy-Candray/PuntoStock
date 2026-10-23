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
  long expiresIn=in.rememberMe()?604800:900;
  var now=Instant.now();var claims=JwtClaimsSet.builder().issuer("puntostock").subject(u.username).claim("authVersion",u.authVersion).issuedAt(now).expiresAt(now.plusSeconds(expiresIn)).build();
  var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
  return Map.of("accessToken",token,"expiresIn",expiresIn,"roles",u.roles.stream().map(r->r.nombre).toList());
 }
}
