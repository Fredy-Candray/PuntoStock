import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
export interface Producto {id:number;sku:string;nombre:string;descripcion:string;precioCompra?:number;precioVenta:number;stock:number;fotoUrl:string}
export type ProductoInput = Omit<Producto,'id'|'stock'>;
export interface Movimiento {id:number;productoId:number;tipo:string;cantidad:number;fecha:string;motivo:string;stockAnterior:number;stockPosterior:number}
@Injectable({providedIn:'root'})
export class ApiService {
 private http=inject(HttpClient);
 productos(){return this.http.get<Producto[]>('/api/productos');}
 guardar(p:ProductoInput,id?:number){return id?this.http.put('/api/productos/'+id,p):this.http.post('/api/productos',p);}
 eliminar(id:number){return this.http.delete('/api/productos/'+id);}
 vender(lineas:{productoId:number;cantidad:number}[]){return this.http.post<{id:number;total:number}>('/api/ventas',{lineas});}
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
 constructor(){
  try {
   const saved=JSON.parse(sessionStorage.getItem(this.sessionKey)??'null');
   if(saved&&typeof saved.accessToken==='string'&&typeof saved.username==='string'&&
      Array.isArray(saved.roles)&&saved.roles.every((role:unknown)=>typeof role==='string')&&
      typeof saved.expiresAt==='number'&&saved.expiresAt>Date.now()){
    this.token.set(saved.accessToken);this.roles.set(saved.roles);this.username.set(saved.username);
   }else{sessionStorage.removeItem(this.sessionKey);}
  }catch{this.logout();}
 }
 iniciarSesion(session:{accessToken:string;roles:string[];expiresIn:number},username:string){
  this.token.set(session.accessToken);this.roles.set(session.roles);this.username.set(username);
  try{sessionStorage.setItem(this.sessionKey,JSON.stringify({...session,username,expiresAt:Date.now()+session.expiresIn*1000}));}catch{}
 }
 login(username:string,password:string){return this.http.post<{accessToken:string;roles:string[];expiresIn:number}>('/api/auth/login',{username,password});}
 admin(){return this.roles().includes('ADMIN');}
 logout(){this.token.set(null);this.roles.set([]);this.username.set('');try{sessionStorage.removeItem(this.sessionKey);}catch{}}
}
