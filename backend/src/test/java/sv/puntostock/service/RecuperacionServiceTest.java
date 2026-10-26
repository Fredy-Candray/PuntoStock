package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.UsuarioRepository;

class RecuperacionServiceTest {
  final UsuarioRepository usuarios = mock(UsuarioRepository.class);
  final PasswordEncoder passwords = mock(PasswordEncoder.class);
  final CorreoOutbox correo = mock(CorreoOutbox.class);
  final Clock clock = Clock.fixed(Instant.parse("2026-10-26T06:00:00Z"), ZoneOffset.UTC);
  final Usuario u = new Usuario();

  RecuperacionService service() {
    when(correo.disponible()).thenReturn(true);
    return new RecuperacionService(
        usuarios,
        passwords,
        correo,
        clock,
        new sv.puntostock.security.LimiteIntentos(clock),
        "https://example.com");
  }

  RecuperacionServiceTest() {
    u.username = "ana";
    u.correo = "ana@example.com";
    u.passwordHash = "anterior";
    when(usuarios.bloquear("ana")).thenReturn(Optional.of(u));
  }

  @Test
  void enviaTokenAleatorioPeroGuardaSoloHashYLimitaReenvios() {
    var s = service();
    s.solicitar("ana", "ana@example.com");
    var message = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(correo).encolar(eq(u), message.capture());
    String token = message.getValue().split("#recuperar=")[1].split("&")[0];
    assertEquals(43, token.length());
    assertEquals(RecuperacionService.hash(token), u.recuperacionHash);
    assertTrue(u.recuperacionExpira.isAfter(LocalDateTime.now(clock).plusMinutes(14)));
    s.solicitar("ana", "ana@example.com");
    verify(correo, times(1)).encolar(eq(u), anyString());
  }

  @Test
  void correoIncorrectoNoEnviaNiModificaCuenta() {
    service().solicitar("ana", "otra@example.com");
    verify(correo, never()).encolar(any(), anyString());
    assertNull(u.recuperacionHash);
  }

  @Test
  void restablecerInvalidaTokenYSesionesPrevias() {
    u.recuperacionHash = RecuperacionService.hash("token");
    u.recuperacionExpira = LocalDateTime.now(clock).plusMinutes(1);
    when(passwords.encode("nueva-password")).thenReturn("hash-nuevo");
    var s = service();
    s.restablecer("ana", "token", "nueva-password");
    assertEquals("hash-nuevo", u.passwordHash);
    assertEquals(1, u.authVersion);
    assertNull(u.recuperacionHash);
    assertThrows(
        ResponseStatusException.class, () -> s.restablecer("ana", "token", "otra-password"));
  }

  @Test
  void tokenVencidoOIncorrectoNoCambiaClave() {
    u.recuperacionHash = RecuperacionService.hash("correcto");
    u.recuperacionExpira = LocalDateTime.now(clock).minusSeconds(1);
    var s = service();
    assertThrows(
        ResponseStatusException.class, () -> s.restablecer("ana", "correcto", "nueva-password"));
    u.recuperacionExpira = LocalDateTime.now(clock).plusMinutes(1);
    assertThrows(
        ResponseStatusException.class, () -> s.restablecer("ana", "incorrecto", "nueva-password"));
    verifyNoInteractions(passwords);
  }

  @Test
  void desactivadoNoPuedeRecuperar() {
    u.activo = false;
    var s = service();
    s.solicitar("ana", "ana@example.com");
    verify(correo, never()).encolar(any(), anyString());
    assertThrows(
        ResponseStatusException.class, () -> s.restablecer("ana", "token", "nueva-password"));
  }

  @Test
  void correoNoConfiguradoDevuelveDisponibilidad() {
    var s = service();
    when(correo.disponible()).thenReturn(false);
    assertEquals(
        503,
        assertThrows(ResponseStatusException.class, () -> s.solicitar("ana", "ana@example.com"))
            .getStatusCode()
            .value());
    verify(correo, never()).encolar(any(), anyString());
  }
}
