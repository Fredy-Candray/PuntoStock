package sv.puntostock.service;

import java.text.Normalizer;
import java.util.Locale;

/** Código estable: tipo, categoría y nombre al crear, seguido del identificador de ingreso. */
public final class CodigoProducto {
  private CodigoProducto() {}

  private static String prefijo(String texto, String defecto) {
    String normalizado =
        Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "");
    return normalizado.isEmpty()
        ? defecto
        : normalizado.substring(0, Math.min(3, normalizado.length()));
  }

  public static String crear(String tipo, String categoria, String nombre, long id) {
    return ("SERVICIO".equals(tipo) ? "SRV" : "ART")
        + "-"
        + prefijo(categoria, "GEN")
        + "-"
        + prefijo(nombre, "ART")
        + "-"
        + String.format(Locale.ROOT, "%06d", id);
  }
}
