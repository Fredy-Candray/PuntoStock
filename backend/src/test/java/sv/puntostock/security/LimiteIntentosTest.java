package sv.puntostock.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

class LimiteIntentosTest {
  @Test
  void limitaCuentaAunqueCambieLaIp() {
    var l = new LimiteIntentos(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    for (int i = 0; i < 10; i++) l.comprobar("login", null, "Ana");
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> l.comprobar("login", null, " ana "));
    l.comprobar("login", null, "otra");
  }

  @Test
  void limitaIpAunqueCambieLaCuenta() {
    var l = new LimiteIntentos(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    for (int i = 0; i < 30; i++) l.comprobar("login", "127.0.0.1", null);
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> l.comprobar("login", "127.0.0.1", null));
  }
}
