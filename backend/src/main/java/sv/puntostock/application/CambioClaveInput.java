package sv.puntostock.application;

import jakarta.validation.constraints.*;

public record CambioClaveInput(
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(min = 8, max = 72) String confirmacion) {}
