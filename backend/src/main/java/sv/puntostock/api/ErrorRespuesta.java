package sv.puntostock.api;

import java.util.Map;

public record ErrorRespuesta(
    String code, String message, Map<String, String> campos, String requestId) {}
