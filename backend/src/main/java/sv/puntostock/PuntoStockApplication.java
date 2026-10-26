package sv.puntostock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class PuntoStockApplication {
  public static void main(String[] args) {
    SpringApplication.run(PuntoStockApplication.class, args);
  }
}
