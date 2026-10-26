package sv.puntostock.api;

import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errores {
  private ErrorRespuesta cuerpo(int status, String mensaje, Map<String, String> campos) {
    return new ErrorRespuesta("HTTP_" + status, mensaje, campos, org.slf4j.MDC.get("requestId"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorRespuesta> negocio(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            cuerpo(
                e.getStatusCode().value(),
                e.getReason() == null ? "No se pudo completar la operación." : e.getReason(),
                Map.of()));
  }

  @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
  public ResponseEntity<ErrorRespuesta> conflicto(Exception e) {
    return ResponseEntity.status(409)
        .body(
            cuerpo(
                409,
                "Conflicto de datos; actualiza y revisa SKU, usuario o stock antes de reintentar.",
                Map.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
    var campos = new LinkedHashMap<String, String>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> campos.putIfAbsent(f.getField(), "Valor inválido"));
    return ResponseEntity.badRequest().body(cuerpo(400, "Revisa los campos indicados.", campos));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorRespuesta> formato(Exception e) {
    return ResponseEntity.badRequest().body(cuerpo(400, "Solicitud inválida.", Map.of()));
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorRespuesta> acceso(Exception e) {
    return ResponseEntity.status(403)
        .body(cuerpo(403, "No tienes permiso para esta operación.", Map.of()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
    org.slf4j.LoggerFactory.getLogger(Errores.class)
        .error("Operación fallida: {}", e.getClass().getSimpleName());
    return ResponseEntity.internalServerError()
        .body(
            cuerpo(
                500,
                "No se pudo completar la operación. Informa el identificador al administrador.",
                Map.of()));
  }
}
