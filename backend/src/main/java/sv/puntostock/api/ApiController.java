package sv.puntostock.api;
import sv.puntostock.api.Contratos.*;
import sv.puntostock.service.OperacionService;
import sv.puntostock.repository.*;
import sv.puntostock.entity.Producto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api")
public class ApiController {
 private final OperacionService servicio;private final ProductoRepository productos;private final MovimientoRepository movimientos;
 public ApiController(OperacionService s,ProductoRepository p,MovimientoRepository m){servicio=s;productos=p;movimientos=m;}
 @GetMapping("/productos") public List<Map<String,Object>> productos(Principal actor){
  boolean admin=servicio.actor(actor.getName()).roles.stream().anyMatch(r->r.nombre.equals("ADMIN"));
  return productos.findByActivoTrueOrderByNombreAsc().stream().map(p->{Map<String,Object> dto=new LinkedHashMap<>();
   dto.put("id",p.id);dto.put("sku",p.sku);dto.put("nombre",p.nombre);dto.put("descripcion",p.descripcion);dto.put("precioVenta",p.precioVenta);dto.put("stock",p.stock);dto.put("fotoUrl",p.fotoUrl);
   dto.put("tipo",p.tipo);dto.put("categoria",p.categoria);dto.put("marca",p.marca);dto.put("unidad",p.unidad);dto.put("ubicacion",p.ubicacion);dto.put("stockMinimo",p.stockMinimo);
   if(admin)dto.put("precioCompra",p.precioCompra);return dto;}).toList();
 }
 @PostMapping("/productos") @PreAuthorize("hasRole('ADMIN')") public Map<String,Long> crear(@Valid @RequestBody ProductoInput in){return Map.of("id",servicio.guardar(null,in).id);}
 @PutMapping("/productos/{id}") @PreAuthorize("hasRole('ADMIN')") public Map<String,Long> actualizar(@PathVariable Long id,@Valid @RequestBody ProductoInput in){return Map.of("id",servicio.guardar(id,in).id);}
 @DeleteMapping("/productos/{id}") @PreAuthorize("hasRole('ADMIN')") public void eliminar(@PathVariable Long id){servicio.eliminar(id);}
 @PostMapping("/ventas") public VentaResultado vender(@Valid @RequestBody VentaInput in,Principal p){return servicio.vender(in,p.getName());}
 @GetMapping("/ventas") public HistorialVentas ventas(@RequestParam(required=false) LocalDate fecha,@RequestParam(defaultValue="0") int pagina,Principal p){return servicio.historial(fecha==null?LocalDate.now(java.time.ZoneId.of("America/El_Salvador")):fecha,pagina,p.getName());}
 @GetMapping("/ventas/{id}") public VentaResultado ticket(@PathVariable Long id,Principal p){return servicio.comprobante(id,p.getName());}
 @GetMapping("/ventas/solicitud/{id}") public VentaResultado solicitud(@PathVariable java.util.UUID id,Principal p){return servicio.solicitud(id,p.getName());}
 @PostMapping("/inventario/movimientos") @PreAuthorize("hasRole('ADMIN')") public void mover(@Valid @RequestBody MovimientoInput in,Principal p){servicio.mover(in,p.getName());}
 @GetMapping("/inventario/movimientos") @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true)
 public List<Map<String,Object>> movimientos(){return movimientos.findTop200ByOrderByFechaDesc().stream().map(m->Map.<String,Object>of("id",m.id,"productoId",m.producto.id,"tipo",m.tipo,"cantidad",m.cantidad,"fecha",m.fecha,"motivo",m.motivo,"stockAnterior",m.stockAnterior,"stockPosterior",m.stockPosterior)).toList();}
 @GetMapping("/reportes/ganancia") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> ganancia(@RequestParam LocalDate fecha){return Map.of("fecha",fecha,"ganancia",servicio.ganancia(fecha));}
}
