package sv.puntostock.security;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ventanas acotadas en memoria; detrás del proxy se usa la IP del socket, nunca cabeceras no
 * confiables.
 */
@Component
public class LimiteIntentos {
  private record Ventana(long inicio, int cantidad) {}

  private final Map<String, Ventana> ventanas = new HashMap<>();
  private final Clock clock;

  public LimiteIntentos(Clock clock) {
    this.clock = clock;
  }

  public synchronized void comprobar(String operacion, String ip, String cuenta) {
    long ahora = clock.millis();
    ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicio >= 60_000);
    if (ventanas.size() > 20_000)
      throw new ResponseStatusException(
          TOO_MANY_REQUESTS, "Demasiados intentos. Espera un minuto.");
    if (ip != null)
      consumir(operacion + ":ip:" + ip, operacion.startsWith("login") ? 30 : 20, ahora);
    if (cuenta != null)
      consumir(
          operacion + ":cuenta:" + cuenta.trim().toLowerCase(Locale.ROOT),
          operacion.startsWith("login") ? 10 : 5,
          ahora);
  }

  private void consumir(String clave, int maximo, long ahora) {
    var v = ventanas.getOrDefault(clave, new Ventana(ahora, 0));
    if (v.cantidad >= maximo)
      throw new ResponseStatusException(
          TOO_MANY_REQUESTS, "Demasiados intentos. Espera un minuto.");
    ventanas.put(clave, new Ventana(v.inicio, v.cantidad + 1));
  }
}
