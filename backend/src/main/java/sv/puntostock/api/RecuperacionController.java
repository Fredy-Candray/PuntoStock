package sv.puntostock.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import sv.puntostock.service.RecuperacionService;

@RestController
@RequestMapping("/api/auth")
public class RecuperacionController {
  private final RecuperacionService service;

  public RecuperacionController(RecuperacionService service) {
    this.service = service;
  }

  public record Solicitud(
      @NotBlank @Size(max = 80) String username, @NotBlank @Email @Size(max = 254) String correo) {}

  public record Restablecer(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  @PostMapping("/recuperacion")
  public Map<String, String> solicitar(@Valid @RequestBody Solicitud in) {
    service.solicitar(in.username(), in.correo());
    return Map.of(
        "mensaje",
        "Si los datos corresponden a una cuenta activa, recibirás un enlace en tu correo. Revisa también Spam. Si ya lo solicitaste, espera dos minutos antes de repetir.");
  }

  @PostMapping("/restablecer")
  public Map<String, String> restablecer(@Valid @RequestBody Restablecer in) {
    service.restablecer(in.username(), in.token(), in.password());
    return Map.of("mensaje", "Contraseña actualizada. Inicia sesión con tu nueva contraseña.");
  }
}
