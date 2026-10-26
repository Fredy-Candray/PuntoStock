package sv.puntostock.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class Correlacion extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String id = UUID.randomUUID().toString();
    org.slf4j.MDC.put("requestId", id);
    res.setHeader("X-Request-ID", id);
    try {
      chain.doFilter(req, res);
    } finally {
      org.slf4j.MDC.remove("requestId");
    }
  }
}
