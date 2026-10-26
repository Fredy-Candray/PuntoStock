package sv.puntostock.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sv.puntostock.entity.Usuario;
import sv.puntostock.repository.*;

@Component
public class BootstrapAdmin implements CommandLineRunner {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PasswordEncoder encoder;

  @Value("${app.bootstrap-user}")
  private String username;

  @Value("${app.bootstrap-password}")
  private String password;

  public BootstrapAdmin(UsuarioRepository u, RolRepository r, PasswordEncoder e) {
    usuarios = u;
    roles = r;
    encoder = e;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (usuarios.count() > 0 || username.isBlank()) return;
    int length = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    if (password.length() < 12 || length > 72)
      throw new IllegalArgumentException("Clave inicial: mínimo 12 caracteres y máximo 72 bytes");
    Usuario u = new Usuario();
    u.username = username;
    u.passwordHash = encoder.encode(password);
    u.roles.add(roles.findByNombre("ADMIN").orElseThrow());
    usuarios.save(u);
  }
}
