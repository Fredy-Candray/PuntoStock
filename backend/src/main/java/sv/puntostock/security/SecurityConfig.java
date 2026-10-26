package sv.puntostock.security;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import sv.puntostock.repository.UsuarioRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private static void error(jakarta.servlet.http.HttpServletResponse res, int status)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType("application/json");
    String id = org.slf4j.MDC.get("requestId");
    res.getWriter()
        .write(
            "{\"code\":\"HTTP_"
                + status
                + "\",\"message\":\""
                + (status == 401
                    ? "Inicia sesión nuevamente."
                    : "No tienes permiso para esta operación.")
                + "\",\"campos\":{},\"requestId\":\""
                + id
                + "\"}");
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecretKeySpec jwtKey(@Value("${app.jwt-secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32)
      throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes");
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder encoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder decoder(SecretKeySpec key) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("puntostock"));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, UsuarioRepository usuarios, Sesiones sesiones)
      throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> error(res, 401))
                    .accessDeniedHandler((req, res, ex) -> error(res, 403)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/login",
                        "/api/auth/recuperacion",
                        "/api/auth/restablecer",
                        "/api/auth/renovar",
                        "/api/auth/salir",
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint((req, res, ex) -> error(res, 401))
                    .accessDeniedHandler((req, res, ex) -> error(res, 403))
                    .jwt(
                        j ->
                            j.jwtAuthenticationConverter(
                                jwt -> {
                                  var u =
                                      usuarios
                                          .acceso(jwt.getSubject())
                                          .filter(UsuarioRepository.Acceso::getActivo)
                                          .orElseThrow(
                                              () ->
                                                  new org.springframework.security.oauth2.core
                                                      .OAuth2AuthenticationException(
                                                      "invalid_token"));
                                  Number tokenVersion = jwt.getClaim("authVersion");
                                  if ((tokenVersion == null ? 0 : tokenVersion.longValue())
                                      != u.getAuthVersion())
                                    throw new org.springframework.security.oauth2.core
                                        .OAuth2AuthenticationException("invalid_token");
                                  if (!sesiones.vigente(
                                      jwt.getClaimAsString("sid"), u.getId(), u.getAuthVersion()))
                                    throw new org.springframework.security.oauth2.core
                                        .OAuth2AuthenticationException("invalid_token");
                                  var authorities =
                                      usuarios.rolesAcceso(u.getId()).stream()
                                          .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                          .toList();
                                  return new JwtAuthenticationToken(
                                      jwt, authorities, u.getUsername());
                                })))
        .build();
  }
}
