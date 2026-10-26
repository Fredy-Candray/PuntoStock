package sv.puntostock.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ProteccionPublica extends OncePerRequestFilter {
  private final LimiteIntentos limites;

  public ProteccionPublica(LimiteIntentos limites) {
    this.limites = limites;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String path = req.getRequestURI();
    if (req.getMethod().equals("POST")
        && java.util.Set.of("/api/auth/login", "/api/auth/recuperacion", "/api/auth/restablecer")
            .contains(path)) {
      try {
        limites.comprobar(path.substring(path.lastIndexOf('/') + 1), req.getRemoteAddr(), null);
      } catch (org.springframework.web.server.ResponseStatusException e) {
        res.setStatus(429);
        res.setHeader("Retry-After", "60");
        res.setContentType("application/json");
        res.getWriter()
            .write(
                "{\"code\":\"RATE_LIMITED\",\"message\":\"Demasiados intentos. Espera un minuto.\",\"campos\":{},\"requestId\":\""
                    + org.slf4j.MDC.get("requestId")
                    + "\"}");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
