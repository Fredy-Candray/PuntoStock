package sv.puntostock.local;

import java.net.URI;

/** Solo direcciones IPv4 privadas literales; no resuelve nombres ni admite servidores públicos. */
public final class LocalServer {
  private LocalServer() {}
  public static String normalize(String value) {
    try {
      String text = value.trim();
      if (!text.contains("://")) text = "http://" + text;
      URI uri = new URI(text);
      if (!"http".equals(uri.getScheme()) || uri.getUserInfo() != null
          || uri.getQuery() != null || uri.getFragment() != null
          || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) return null;
      String host = uri.getHost();
      if (host == null) return null;
      String[] parts = host.split("\\.");
      if (parts.length != 4) return null;
      int[] bytes = new int[4];
      for (int i=0; i<4; i++) {
        if (!parts[i].matches("0|[1-9][0-9]{0,2}")) return null;
        bytes[i] = Integer.parseInt(parts[i]);
        if (bytes[i] > 255) return null;
      }
      if (!(host.equals("127.0.0.1") || bytes[0] == 10
          || (bytes[0] == 172 && bytes[1] >= 16 && bytes[1] <= 31)
          || (bytes[0] == 192 && bytes[1] == 168))) return null;
      int port = uri.getPort() == -1 ? 14201 : uri.getPort();
      if (port < 1 || port > 65535) return null;
      return "http://" + host + ":" + port + "/";
    } catch (Exception e) { return null; }
  }
}
