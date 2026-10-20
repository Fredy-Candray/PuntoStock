package sv.puntostock.api;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.UsuarioRepository;
import sv.puntostock.service.FotoPerfilValidator;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController @RequestMapping("/api/auth/me")
public class PerfilController {
 private final UsuarioRepository usuarios;
 public PerfilController(UsuarioRepository usuarios){this.usuarios=usuarios;}
 public record Perfil(Long id,String username,String nombre,String correo,String telefono,
  List<String> roles,boolean activo,OffsetDateTime fechaCreacion,String fotoPerfil,String fotoPortada) {}
 private Usuario actual(Principal principal){
  return usuarios.findByUsername(principal.getName()).filter(u->u.activo)
   .orElseThrow(()->new ResponseStatusException(UNAUTHORIZED));
 }
 private Perfil respuesta(Usuario u){return new Perfil(u.id,u.username,u.nombre==null?u.username:u.nombre,u.correo,u.telefono,
  u.roles.stream().map(r->r.nombre).sorted().toList(),u.activo,
  u.fechaCreacion==null?null:u.fechaCreacion.atOffset(ZoneOffset.UTC),u.fotoPerfil,u.fotoPortada);}
 @GetMapping @Transactional(readOnly=true)
 public Perfil obtener(Principal principal){return respuesta(actual(principal));}
 @PutMapping @Transactional
 public Perfil actualizar(@Valid @RequestBody Contratos.PerfilInput in,Principal principal){
  var u=actual(principal);u.nombre=in.nombre().trim();u.correo=in.correo().trim();
  u.telefono=in.telefono()==null||in.telefono().isBlank()?null:in.telefono().trim();
  return respuesta(u);
 }
 @PutMapping("/foto") @Transactional
 public Perfil foto(@Valid @RequestBody Contratos.FotoInput in,Principal principal){
  var u=actual(principal);u.fotoPerfil=FotoPerfilValidator.validar(in.imagen());return respuesta(u);
 }
 @PutMapping("/portada") @Transactional
 public Perfil portada(@Valid @RequestBody Contratos.FotoInput in,Principal principal){
  var u=actual(principal);u.fotoPortada=FotoPerfilValidator.validar(in.imagen());return respuesta(u);
 }
}
