package sv.puntostock.api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import sv.puntostock.service.UsuariosService;
import java.security.Principal;
import java.util.*;
@RestController @RequestMapping("/api/usuarios") @PreAuthorize("hasRole('ADMIN')")
public class UsuariosController {
 private final UsuariosService service;public UsuariosController(UsuariosService service){this.service=service;}
 @GetMapping public List<UsuariosService.Vista> listar(){return service.listar();}
 @GetMapping("/{id}") public UsuariosService.Detalle detalle(@PathVariable Long id){return service.detalle(id);}
 @PostMapping public Map<String,Long> crear(@Valid @RequestBody Contratos.UsuarioInput in,Principal actor){return Map.of("id",service.guardar(null,in,actor.getName()).id());}
 @PutMapping("/{id}") public UsuariosService.Vista editar(@PathVariable Long id,@Valid @RequestBody Contratos.UsuarioInput in,Principal actor){return service.guardar(id,in,actor.getName());}
 @PutMapping("/{id}/activo") public void estado(@PathVariable Long id,@RequestParam boolean valor,Principal actor){service.estado(id,valor,actor.getName());}
 @DeleteMapping("/{id}") public void desactivar(@PathVariable Long id,Principal actor){service.estado(id,false,actor.getName());}
}
