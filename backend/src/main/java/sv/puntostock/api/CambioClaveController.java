package sv.puntostock.api;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.application.CambioClaveInput;
import sv.puntostock.service.UsuariosService;

@RestController
public class CambioClaveController {
  private final UsuariosService usuarios;

  public CambioClaveController(UsuariosService usuarios) {
    this.usuarios = usuarios;
  }

  @PostMapping("/api/auth/cambiar-clave-inicial")
  public void cambiar(@Valid @RequestBody CambioClaveInput in, Principal actor) {
    usuarios.cambiarClaveInicial(actor.getName(), in.password(), in.confirmacion());
  }
}
