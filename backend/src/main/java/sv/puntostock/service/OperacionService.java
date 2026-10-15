package sv.puntostock.service;
import sv.puntostock.entity.*;
import sv.puntostock.repository.*;
import sv.puntostock.api.Contratos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service @Transactional
public class OperacionService {
 private final ProductoRepository productos; private final VentaRepository ventas;
 private final MovimientoRepository movimientos; private final UsuarioRepository usuarios;
 public OperacionService(ProductoRepository p,VentaRepository v,MovimientoRepository m,UsuarioRepository u){productos=p;ventas=v;movimientos=m;usuarios=u;}
 public Usuario actor(String username){return usuarios.findByUsername(username).filter(u->u.activo).orElseThrow(()->new ResponseStatusException(UNAUTHORIZED));}
 private Producto bloquear(Long id){return productos.bloquear(id).filter(p->p.activo).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Producto no disponible"));}
 public Producto guardar(Long id,ProductoInput in){
  Producto p=id==null?new Producto():bloquear(id);
  p.sku=in.sku();p.nombre=in.nombre();p.descripcion=in.descripcion();p.precioCompra=in.precioCompra();p.precioVenta=in.precioVenta();p.fotoUrl=in.fotoUrl();
  return productos.save(p);
 }
 public void eliminar(Long id){bloquear(id).activo=false;}
 private void registrar(Producto p,Usuario u,String tipo,int cantidad,String motivo,Venta venta){
  long nuevo=(long)p.stock+cantidad;
  if(nuevo<0 || nuevo>Integer.MAX_VALUE) throw new ResponseStatusException(CONFLICT,"Stock insuficiente o fuera de rango: "+p.sku);
  Movimiento m=new Movimiento();m.producto=p;m.usuario=u;m.tipo=tipo;m.cantidad=cantidad;m.motivo=motivo;m.venta=venta;
  m.fecha=LocalDateTime.now(ZoneOffset.UTC);m.stockAnterior=p.stock;m.stockPosterior=(int)nuevo;p.stock=(int)nuevo;movimientos.save(m);
 }
 public VentaResultado vender(VentaInput in,String username){
  Usuario u=actor(username);Venta v=new Venta();v.usuario=u;v.fecha=LocalDateTime.now(ZoneOffset.UTC);
  Set<Long> ids=new HashSet<>();
  for(Linea l:in.lineas()) if(!ids.add(l.productoId())) throw new ResponseStatusException(BAD_REQUEST,"Producto repetido");
  // Orden de bloqueo consistente para reducir deadlocks entre ventas.
  var lineas=in.lineas().stream().sorted(Comparator.comparing(Linea::productoId)).toList();
  Map<Long,Producto> bloqueados=new HashMap<>();
  for(Linea l:lineas) bloqueados.put(l.productoId(),bloquear(l.productoId()));
  for(Linea l:lineas){
   Producto p=bloqueados.get(l.productoId());
   if(p.stock<l.cantidad()) throw new ResponseStatusException(CONFLICT,"Stock insuficiente: "+p.sku);
   DetalleVenta d=new DetalleVenta();d.venta=v;d.producto=p;d.cantidad=l.cantidad();d.costoUnitario=p.precioCompra;d.precioUnitario=p.precioVenta;v.detalles.add(d);
   BigDecimal cantidad=BigDecimal.valueOf(l.cantidad());v.total=v.total.add(p.precioVenta.multiply(cantidad));
   v.ganancia=v.ganancia.add(p.precioVenta.subtract(p.precioCompra).multiply(cantidad));
  }
  ventas.save(v);
  for(Linea l:lineas) registrar(bloqueados.get(l.productoId()),u,"VENTA",-l.cantidad(),"Venta #"+v.id,v);
  return new VentaResultado(v.id,v.total);
 }
 public void mover(MovimientoInput in,String username){
  boolean entrada=Set.of("COMPRA","DEVOLUCION_CLIENTE").contains(in.tipo());
  boolean salida=Set.of("MERMA","DEVOLUCION_PROVEEDOR").contains(in.tipo());
  if(in.cantidad()==0 || !(entrada||salida||in.tipo().equals("AJUSTE")) || (entrada&&in.cantidad()<0) || (salida&&in.cantidad()>0))
   throw new ResponseStatusException(BAD_REQUEST,"Tipo o signo de cantidad inválido");
  registrar(bloquear(in.productoId()),actor(username),in.tipo(),in.cantidad(),in.motivo(),null);
 }
 @Transactional(readOnly=true)
 public BigDecimal ganancia(LocalDate dia){
  ZoneId zona=ZoneId.of("America/El_Salvador");
  var desde=LocalDateTime.ofInstant(dia.atStartOfDay(zona).toInstant(),ZoneOffset.UTC);
  var hasta=LocalDateTime.ofInstant(dia.plusDays(1).atStartOfDay(zona).toInstant(),ZoneOffset.UTC);
  return ventas.ganancia(desde,hasta);
 }
}
