package sv.puntostock.service;

import java.util.Locale;
import java.util.Objects;
import sv.puntostock.entity.Usuario;

/** Política común para el correo y la revocación de recuperación. */
public final class Identidad {
  private Identidad() {}

  public static String normalizarCorreo(String correo) {
    return correo == null || correo.isBlank() ? null : correo.trim().toLowerCase(Locale.ROOT);
  }

  public static void cambiarCorreo(Usuario usuario, String correo) {
    String nuevo = normalizarCorreo(correo);
    if (!Objects.equals(usuario.correo, nuevo)) {
      usuario.recuperacionHash = null;
      usuario.recuperacionExpira = null;
      usuario.recuperacionSolicitada = null;
    }
    usuario.correo = nuevo;
  }
}
