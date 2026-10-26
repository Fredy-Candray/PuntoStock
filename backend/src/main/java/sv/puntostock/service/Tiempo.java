package sv.puntostock.service;

import java.time.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Tiempo {
  public static final ZoneId ZONA = ZoneId.of("America/El_Salvador");

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  public static LocalDateTime inicio(LocalDate dia) {
    return LocalDateTime.ofInstant(dia.atStartOfDay(ZONA).toInstant(), ZoneOffset.UTC);
  }
}
