package sv.puntostock.api;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.application.Contratos;
import sv.puntostock.security.Sesiones;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final String COOKIE = "puntostock.refresh";
  private final Sesiones sesiones;
  private final boolean secure;

  public AuthController(Sesiones sesiones, @Value("${app.session.secure:true}") boolean secure) {
    this.sesiones = sesiones;
    this.secure = secure;
  }

  @PostMapping("/login")
  public Sesiones.Acceso login(
      @Valid @RequestBody Contratos.Login in, HttpServletRequest req, HttpServletResponse res) {
    verificarNavegador(req);
    var emitida = sesiones.ingresar(in.username(), in.password(), in.rememberMe());
    sesiones.salir(cookie(req));
    guardarCookie(res, emitida);
    return emitida.acceso();
  }

  @PostMapping("/renovar")
  public Sesiones.Acceso renovar(HttpServletRequest req, HttpServletResponse res) {
    verificarNavegador(req);
    var emitida = sesiones.renovar(cookie(req));
    guardarCookie(res, emitida);
    return emitida.acceso();
  }

  @PostMapping("/salir")
  public void salir(HttpServletRequest req, HttpServletResponse res) {
    verificarNavegador(req);
    sesiones.salir(cookie(req));
    res.addHeader(
        "Set-Cookie",
        ResponseCookie.from(COOKIE, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(0)
            .build()
            .toString());
  }

  private void guardarCookie(HttpServletResponse res, Sesiones.Emitida e) {
    var b =
        ResponseCookie.from(COOKIE, e.renovacion())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/api/auth");
    if (e.recordar()) b.maxAge(604800);
    res.addHeader("Set-Cookie", b.build().toString());
  }

  // Cabecera no enviable mediante formularios de otro origen; CORS permanece deshabilitado.
  private void verificarNavegador(HttpServletRequest req) {
    if (!"1".equals(req.getHeader("X-PuntoStock")))
      throw new ResponseStatusException(FORBIDDEN, "Solicitud de sesión no permitida.");
  }

  private String cookie(HttpServletRequest req) {
    if (req.getCookies() != null)
      for (var c : req.getCookies()) if (c.getName().equals(COOKIE)) return c.getValue();
    return null;
  }
}
