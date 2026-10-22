package sv.puntostock.security;
import sv.puntostock.repository.UsuarioRepository;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecretKeySpec jwtKey(@Value("${app.jwt-secret}") String secret){
  byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);if(bytes.length<32)throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes");return new SecretKeySpec(bytes,"HmacSHA256");
 }
 @Bean JwtEncoder encoder(SecretKeySpec key){return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));}
 @Bean JwtDecoder decoder(SecretKeySpec key){
  NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("puntostock"));return decoder;
 }
 @Bean SecurityFilterChain security(HttpSecurity http,UsuarioRepository usuarios)throws Exception{
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/api/auth/recuperacion","/api/auth/restablecer").permitAll().anyRequest().authenticated())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwt->{
    var u=usuarios.findByUsername(jwt.getSubject()).filter(x->x.activo).orElseThrow(()->new org.springframework.security.oauth2.core.OAuth2AuthenticationException("invalid_token"));
    Number tokenVersion=jwt.getClaim("authVersion");
    if((tokenVersion==null?0:tokenVersion.longValue())!=u.authVersion)throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("invalid_token");
    var authorities=u.roles.stream().map(r->new SimpleGrantedAuthority("ROLE_"+r.nombre)).toList();
    return new JwtAuthenticationToken(jwt,authorities,u.username);
   }))).build();
 }
}
