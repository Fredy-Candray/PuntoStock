package sv.puntostock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {
  @Test
  void controladoresUsanServiciosYLosServiciosNoDependenDeContratosHttp() throws IOException {
    Path root = Path.of("src/main/java/sv/puntostock");
    try (var files = Files.list(root.resolve("api"))) {
      for (Path f :
          files.filter(p -> p.getFileName().toString().endsWith("Controller.java")).toList())
        assertFalse(
            Pattern.compile("(?:import|new)\\s+sv\\.puntostock\\.repository\\.")
                .matcher(Files.readString(f))
                .find(),
            "Acceso a persistencia en " + f.getFileName());
    }
    try (var files = Files.list(root.resolve("service"))) {
      for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList())
        assertFalse(
            Pattern.compile("import\\s+sv\\.puntostock\\.api\\.")
                .matcher(Files.readString(f))
                .find(),
            "Contrato HTTP en " + f.getFileName());
    }
  }
}
