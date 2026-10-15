package sv.puntostock.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.*;
import java.util.Map;
@RestControllerAdvice
public class Errores {
 @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class})
 public ResponseEntity<Map<String,String>> conflicto(Exception e){return ResponseEntity.status(409).body(Map.of("message","Conflicto de datos; actualice y revise SKU, usuario o stock antes de reintentar"));}
}
