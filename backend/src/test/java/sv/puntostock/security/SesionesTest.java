package sv.puntostock.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import sv.puntostock.repository.UsuarioRepository;

class SesionesTest {
  final JdbcTemplate sql = mock(JdbcTemplate.class);
  final UsuarioRepository usuarios = mock(UsuarioRepository.class);
  final PasswordEncoder claves = mock(PasswordEncoder.class);
  final JwtEncoder jwt = mock(JwtEncoder.class);
  final Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  Sesiones service() {
    when(claves.encode(anyString())).thenReturn("dummy");
    return new Sesiones(sql, usuarios, claves, jwt, clock, new LimiteIntentos(clock));
  }

  @Test
  void usuarioInexistenteTambienComparaHash() {
    var s = service();
    when(usuarios.acceso("nadie")).thenReturn(Optional.empty());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> s.ingresar("nadie", "clave", false));
    verify(claves).matches("clave", "dummy");
    verifyNoInteractions(jwt);
  }

  @Test
  void sinSesionPersistidaNoAceptaJwt() {
    assertFalse(service().vigente(null, 1, 0));
    verifyNoInteractions(sql);
  }

  @Test
  void renovarTokenMalformadoNoConsultaBase() {
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service().renovar("invalido"));
    verifyNoInteractions(sql);
  }
}
