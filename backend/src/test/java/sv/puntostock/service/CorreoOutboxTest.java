package sv.puntostock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import sv.puntostock.entity.Usuario;

class CorreoOutboxTest {
  @Test
  @SuppressWarnings("unchecked")
  void encolarCifraDestinoYTokenSinContactarSmtp() {
    var sql = mock(JdbcTemplate.class);
    ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    var outbox =
        new CorreoOutbox(
            sql,
            provider,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            "from@example.com",
            true,
            "solo-test-secret-32-bytes-minimo");
    var u = new Usuario();
    u.id = 1L;
    u.correo = "privado@example.com";
    u.recuperacionHash = "hash";
    outbox.encolar(u, "https://example.com/login#token=secreto");
    verifyNoInteractions(provider);
    var cap = org.mockito.ArgumentCaptor.forClass(Object.class);
    verify(sql)
        .update(
            contains("insert into correo_pendiente"),
            eq(1L),
            eq("hash"),
            cap.capture(),
            eq(900L),
            eq(0L));
    assertFalse(cap.getValue().toString().contains("secreto"));
    assertFalse(cap.getValue().toString().contains("privado"));
  }
}
