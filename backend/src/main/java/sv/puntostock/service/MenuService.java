package sv.puntostock.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sv.puntostock.repository.*;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.http.HttpStatus.*;

@Service
public class MenuService {
 public record Opcion(String id,String etiqueta,String ruta,String icono,int orden,boolean activo,List<String> roles){}
 public record Seccion(String id,String titulo,int orden,boolean activo,List<Opcion> opciones){}
 public record Configuracion(List<Seccion> secciones){}
 public record Ruta(String ruta,String etiqueta,List<String> roles){}
 public record Respuesta(long version,List<Seccion> secciones,List<String> roles,List<Ruta> rutas,List<String> iconos){}
 public record Guardar(Long version,List<Seccion> secciones){}
 public static final List<String> ROLES=List.of("ADMIN","VENDEDOR");
 public static final List<String> ICONOS=List.of("grid","cart","receipt","box","history","users","settings","user","chart","calendar","shield");
 public static final List<Ruta> RUTAS=List.of(new Ruta("/dashboard","Resumen",List.of("ADMIN")),new Ruta("/pos","Punto de venta",ROLES),new Ruta("/ventas","Ventas",ROLES),new Ruta("/productos","Productos",List.of("ADMIN")),new Ruta("/inventario","Inventario",List.of("ADMIN")),new Ruta("/usuarios","Usuarios",List.of("ADMIN")),new Ruta("/administrar-menu","Administrar menú",List.of("ADMIN")),new Ruta("/perfil","Mi perfil",ROLES));
 private final MenuConfiguracionRepository repository;private final UsuarioRepository usuarios;private final ObjectMapper mapper;
 public MenuService(MenuConfiguracionRepository repository,UsuarioRepository usuarios,ObjectMapper mapper){this.repository=repository;this.usuarios=usuarios;this.mapper=mapper;}
 private sv.puntostock.entity.MenuConfiguracion registro(){return repository.findById(1).orElseThrow(()->new ResponseStatusException(SERVICE_UNAVAILABLE,"Aplica la migración 006 del menú."));}
 private Configuracion leer(String json){return mapper.readValue(json,Configuracion.class);}
 @Transactional(readOnly=true) public Respuesta configuracion(){var r=registro();return new Respuesta(r.version,leer(r.contenido).secciones(),ROLES,RUTAS,ICONOS);}
 @Transactional public Respuesta guardar(Guardar in){
  validar(in.secciones());var r=registro();
  if(in.version()==null||in.version()!=r.version)throw new ResponseStatusException(CONFLICT,"El menú cambió en otra sesión. Vuelve a cargar antes de guardar.");
  r.contenido=mapper.writeValueAsString(new Configuracion(in.secciones()));repository.saveAndFlush(r);return configuracion();
 }
 @Transactional(readOnly=true) public List<Seccion> menu(String username){
  var u=usuarios.findByUsername(username).filter(x->x.activo).orElseThrow(()->new ResponseStatusException(UNAUTHORIZED));
  var roles=u.roles.stream().map(x->x.nombre).toList();
  return leer(registro().contenido).secciones().stream().filter(Seccion::activo).sorted(Comparator.comparingInt(Seccion::orden)).map(s->new Seccion(s.id(),s.titulo(),s.orden(),s.activo(),s.opciones().stream().filter(Opcion::activo).filter(o->o.roles().stream().anyMatch(roles::contains)).sorted(Comparator.comparingInt(Opcion::orden)).toList())).filter(s->!s.opciones().isEmpty()).toList();
 }
 public static void validar(List<Seccion> secciones){
  if(secciones==null||secciones.isEmpty()||secciones.size()>20)throw invalido("Agrega entre 1 y 20 secciones.");
  Set<String> rutas=new HashSet<>(),ids=new HashSet<>();boolean acceso=false;int total=0;
  for(var s:secciones){
   if(s==null)throw invalido("Sección inválida.");texto(s.id(),80);texto(s.titulo(),80);if(!ids.add(s.id())||s.orden()<1||s.orden()>1000||s.opciones()==null)throw invalido("Revisa el orden y las secciones del menú.");
   for(var o:s.opciones()){
    if(o==null||++total>50)throw invalido("El menú admite hasta 50 opciones.");texto(o.id(),80);texto(o.etiqueta(),80);
    var ruta=RUTAS.stream().filter(r->r.ruta().equals(o.ruta())).findFirst().orElseThrow(()->invalido("Selecciona una pantalla existente de PuntoStock."));
    if(!ids.add(o.id())||!rutas.add(o.ruta())||!ICONOS.contains(o.icono())||o.orden()<1||o.orden()>1000||o.roles()==null||new HashSet<>(o.roles()).size()!=o.roles().size()||!ruta.roles().containsAll(o.roles()))throw invalido("Revisa rutas, roles, íconos y orden. Los permisos de la API se conservan.");
    if(s.activo()&&o.activo()&&o.ruta().equals("/administrar-menu")&&o.roles().contains("ADMIN"))acceso=true;
   }
  }
  if(!acceso)throw invalido("Administrar menú debe permanecer activo y visible para Administrador.");
 }
 private static void texto(String texto,int max){if(texto==null||texto.isBlank()||texto.length()>max)throw invalido("Completa los títulos y nombres del menú.");}
 private static ResponseStatusException invalido(String mensaje){return new ResponseStatusException(BAD_REQUEST,mensaje);}
}
