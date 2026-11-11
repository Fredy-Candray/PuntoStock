import sv.puntostock.local.LocalServer;

public class LocalServerTest {
  public static void main(String[] args) {
    String[] ok={"http://192.168.31.155:14201/","10.0.0.1:14201","172.16.0.1:14201","172.31.255.254:14201","127.0.0.1:14200","192.168.1.2"};
    String[] no={"http://example.com:14201/","http://8.8.8.8:14201/","http://172.32.0.1:14201/","https://192.168.1.2:14201/","http://192.168.1.2:0/","http://192.168.1.2:65536/","http://user@192.168.1.2:14201/","http://192.168.1.2:14201/login","http://192.168.1.2:14201/?a=1","http://192.168.1.2:14201/#x","http://192.168.001.2:14201/","http://999.168.1.2:14201/"};
    for(String url:ok) if(LocalServer.normalize(url)==null) throw new AssertionError("Rechazo indebido: "+url);
    for(String url:no) if(LocalServer.normalize(url)!=null) throw new AssertionError("Origen no permitido: "+url);
    System.out.println("APROBADO: 18 casos de direcciones privadas y bloqueo de destinos externos.");
  }
}
