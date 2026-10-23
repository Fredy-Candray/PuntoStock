package sv.puntostock.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import sv.puntostock.service.MenuService;
import java.security.Principal;
import java.util.List;
@RestController @RequestMapping("/api")
public class MenuController {
 private final MenuService service;public MenuController(MenuService service){this.service=service;}
 @GetMapping("/auth/me/menu") public List<MenuService.Seccion> menu(Principal actor){return service.menu(actor.getName());}
 @GetMapping("/menu-admin") @PreAuthorize("hasRole('ADMIN')") public MenuService.Respuesta configuracion(){return service.configuracion();}
 @PutMapping("/menu-admin") @PreAuthorize("hasRole('ADMIN')") public MenuService.Respuesta guardar(@RequestBody MenuService.Guardar in){return service.guardar(in);}
}
