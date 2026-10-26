package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.Contratos;
import sv.puntostock.service.PerfilService;

@RestController
@RequestMapping("/api/auth/me")
public class PerfilController {
  private final PerfilService servicio;

  public PerfilController(PerfilService servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  public PerfilService.Perfil obtener(Principal p) {
    return servicio.obtener(p.getName());
  }

  @PutMapping
  public PerfilService.Perfil actualizar(
      @Valid @RequestBody Contratos.PerfilInput in, Principal p) {
    return servicio.actualizar(in, p.getName());
  }

  @PutMapping("/foto")
  public PerfilService.Perfil foto(@Valid @RequestBody Contratos.FotoInput in, Principal p) {
    return servicio.foto(in, p.getName());
  }

  @PutMapping("/portada")
  public PerfilService.Perfil portada(@Valid @RequestBody Contratos.FotoInput in, Principal p) {
    return servicio.portada(in, p.getName());
  }
}
