package sv.puntostock.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos;
import sv.puntostock.security.Sesiones;

class AuthControllerTest {
  final Sesiones sesiones = mock(Sesiones.class);
  final AuthController controller = new AuthController(sesiones, true);

  MockHttpServletRequest request() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-PuntoStock", "1");
    return req;
  }

  @Test
  void accesoCortoYRenovacionProtegida() {
    var req = request();
    var res = new MockHttpServletResponse();
    var acceso = new Sesiones.Acceso("jwt", 900, List.of("ADMIN"), "test");
    when(sesiones.ingresar("test", "test-password", true))
        .thenReturn(new Sesiones.Emitida(acceso, "opaque", true));
    assertEquals(
        900,
        controller.login(new Contratos.Login("test", "test-password", true), req, res).expiresIn());
    String cookie = res.getHeader("Set-Cookie");
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("Secure"));
    assertTrue(cookie.contains("SameSite=Strict"));
    assertTrue(cookie.contains("Max-Age=604800"));
  }

  @Test
  void sesionSinRecordarNoPersisteCookie() {
    var res = new MockHttpServletResponse();
    when(sesiones.ingresar("test", "test-password", false))
        .thenReturn(
            new Sesiones.Emitida(
                new Sesiones.Acceso("jwt", 900, List.of(), "test"), "opaque", false));
    controller.login(new Contratos.Login("test", "test-password", false), request(), res);
    assertFalse(res.getHeader("Set-Cookie").contains("Max-Age"));
  }

  @Test
  void loginSinRememberMeMantieneCompatibilidad() {
    var in =
        new tools.jackson.databind.ObjectMapper()
            .readValue(
                "{\"username\":\"test\",\"password\":\"test-password\"}", Contratos.Login.class);
    assertFalse(in.rememberMe());
  }

  @Test
  void rememberMeNuloEquivaleASesionNormal() {
    var in =
        new tools.jackson.databind.ObjectMapper()
            .readValue(
                "{\"username\":\"test\",\"password\":\"test-password\",\"rememberMe\":null}",
                Contratos.Login.class);
    assertFalse(in.rememberMe());
  }

  @Test
  void cookiesNoPermitenCsrfSinCabecera() {
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    controller.renovar(new MockHttpServletRequest(), new MockHttpServletResponse()))
            .getStatusCode()
            .value());
    verifyNoInteractions(sesiones);
  }

  @Test
  void salirRevocaYBorraCookie() {
    var req = request();
    req.setCookies(new jakarta.servlet.http.Cookie("puntostock.refresh", "opaque"));
    var res = new MockHttpServletResponse();
    controller.salir(req, res);
    verify(sesiones).salir("opaque");
    assertTrue(res.getHeader("Set-Cookie").contains("Max-Age=0"));
  }
}
