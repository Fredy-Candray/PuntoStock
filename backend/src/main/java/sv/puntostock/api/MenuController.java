package sv.puntostock.api;

import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.service.MenuService;

@RestController
@RequestMapping("/api")
public class MenuController {
  private final MenuService service;

  public MenuController(MenuService service) {
    this.service = service;
  }

  @GetMapping("/auth/me/menu")
  public List<MenuService.Seccion> menu(Principal actor) {
    return service.menu(actor.getName());
  }

  @GetMapping("/menu-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public MenuService.Respuesta configuracion() {
    return service.configuracion();
  }

  @PutMapping("/menu-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public MenuService.Respuesta guardar(@RequestBody MenuService.Guardar in) {
    return service.guardar(in);
  }
}
