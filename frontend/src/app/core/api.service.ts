import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, timeout } from 'rxjs';
export interface Perfil {id:number;username:string;nombre:string;correo:string|null;telefono:string|null;roles:string[];activo:boolean;fechaCreacion:string|null;fotoPerfil:string|null;fotoPortada:string|null}
export interface PerfilInput {nombre:string;correo:string;telefono:string}
export interface Producto {id:number;sku:string;nombre:string;descripcion:string;precioCompra?:number;precioVenta:number;stock:number;fotoUrl:string;tipo:'PRODUCTO'|'SERVICIO';categoria:string;marca:string;unidad:string;ubicacion:string;stockMinimo:number}
export type ProductoInput = Omit<Producto,'id'|'stock'>;
export interface Movimiento {id:number;productoId:number;tipo:string;cantidad:number;fecha:string;motivo:string;stockAnterior:number;stockPosterior:number}
export type MedioPago='EFECTIVO'|'TARJETA'|'TRANSFERENCIA';
export interface Cobro {lineas:{productoId:number;cantidad:number;observaciones:string}[];metodoPago:MedioPago;recibido:number|null;totalEsperado:number;cliente:string;referenciaPago:string;solicitudId:string}
export interface Ticket {id:number;total:number;fecha:string;vendedor:string;metodoPago:string;recibido:number|null;cambio:number|null;cliente:string|null;referenciaPago:string|null;lineas:{productoId:number;sku:string;nombre:string;cantidad:number;precioUnitario:number;subtotal:number;observaciones:string|null}[]}
export interface HistorialVentas {ventas:{id:number;fecha:string;vendedor:string;total:number;metodoPago:string;cliente:string|null}[];cantidad:number;pagina:number;paginas:number;total:number;efectivo:number;tarjeta:number;transferencia:number;sinRegistro:number}
@Injectable({providedIn:'root'})
export class ApiService {
 private http=inject(HttpClient);
 productos(){return this.http.get<Producto[]>('/api/productos');}
 guardar(p:ProductoInput,id?:number){return id?this.http.put('/api/productos/'+id,p):this.http.post('/api/productos',p);}
 eliminar(id:number){return this.http.delete('/api/productos/'+id);}
 vender(cobro:Cobro){return this.http.post<Ticket>('/api/ventas',cobro).pipe(timeout(20000));}
 historialVentas(fecha:string,pagina=0){return this.http.get<HistorialVentas>('/api/ventas',{params:{fecha,pagina}});}
 ticket(id:number){return this.http.get<Ticket>('/api/ventas/'+id);}
 recuperarVenta(solicitudId:string){return this.http.get<Ticket>('/api/ventas/solicitud/'+solicitudId).pipe(timeout(20000));}
 mover(body:{productoId:number;tipo:string;cantidad:number;motivo:string}){return this.http.post('/api/inventario/movimientos',body);}
 movimientos(){return this.http.get<Movimiento[]>('/api/inventario/movimientos');}
 ganancia(fecha:string){return this.http.get<{ganancia:number}>('/api/reportes/ganancia',{params:{fecha}});}
}
@Injectable({providedIn:'root'})
export class AuthService {
 private http=inject(HttpClient);
 private readonly sessionKey='puntostock.session';
 token=signal<string|null>(null);roles=signal<string[]>([]);
 username=signal('');
 perfil=signal<Perfil|null>(null);
 constructor(){
  for(const persistent of [false,true]){
  try {
   const storage=persistent?localStorage:sessionStorage;
   const saved=JSON.parse(storage.getItem(this.sessionKey)??'null');
   if(saved&&typeof saved.accessToken==='string'&&typeof saved.username==='string'&&
      Array.isArray(saved.roles)&&saved.roles.every((role:unknown)=>typeof role==='string')&&
      typeof saved.expiresAt==='number'&&saved.expiresAt>Date.now()){
    this.token.set(saved.accessToken);this.roles.set(saved.roles);this.username.set(saved.username);
    return;
   }else{storage.removeItem(this.sessionKey);}
  }catch{try{(persistent?localStorage:sessionStorage).removeItem(this.sessionKey);}catch{}}
  }
 }
 iniciarSesion(session:{accessToken:string;roles:string[];expiresIn:number},username:string,rememberMe=false){
  this.logout();
  this.token.set(session.accessToken);this.roles.set(session.roles);this.username.set(username);
  try{(rememberMe?localStorage:sessionStorage).setItem(this.sessionKey,JSON.stringify({...session,username,expiresAt:Date.now()+session.expiresIn*1000}));}catch{}
 }
 login(username:string,password:string,rememberMe=false){return this.http.post<{accessToken:string;roles:string[];expiresIn:number}>('/api/auth/login',{username,password,rememberMe});}
 cargarPerfil(){return this.http.get<Perfil>('/api/auth/me').pipe(tap(p=>this.perfil.set(p)));}
 guardarPerfil(input:PerfilInput){return this.http.put<Perfil>('/api/auth/me',input).pipe(tap(p=>this.perfil.set(p)));}
 guardarFoto(imagen:string,portada=false){return this.http.put<Perfil>('/api/auth/me/'+(portada?'portada':'foto'),{imagen}).pipe(tap(p=>this.perfil.set(p)));}
 admin(){return this.roles().includes('ADMIN');}
 logout(){this.token.set(null);this.roles.set([]);this.username.set('');this.perfil.set(null);for(const persistent of [false,true]){try{(persistent?localStorage:sessionStorage).removeItem(this.sessionKey);}catch{}}}
}
