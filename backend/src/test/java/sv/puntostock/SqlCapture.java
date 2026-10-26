package sv.puntostock;

public class SqlCapture implements org.hibernate.resource.jdbc.spi.StatementInspector {
  public static final java.util.Queue<String> consultas =
      new java.util.concurrent.ConcurrentLinkedQueue<>();

  @Override
  public String inspect(String sql) {
    consultas.add(sql);
    return sql;
  }
}
