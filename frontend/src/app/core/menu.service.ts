import { Injectable,inject,signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
export interface OpcionMenu{id:string;etiqueta:string;ruta:string;icono:string;orden:number;activo:boolean;roles:string[]}
export interface SeccionMenu{id:string;titulo:string;orden:number;activo:boolean;opciones:OpcionMenu[]}
export interface ConfiguracionMenu{version:number;secciones:SeccionMenu[];roles:string[];rutas:{ruta:string;etiqueta:string;roles:string[]}[];iconos:string[]}
@Injectable({providedIn:'root'})
export class MenuService {
 private http=inject(HttpClient);secciones=signal<SeccionMenu[]>([]);error=signal(false);
 cargar(){return this.http.get<SeccionMenu[]>('/api/auth/me/menu').pipe(tap({next:s=>{this.secciones.set(s);this.error.set(false);},error:()=>{this.secciones.set([]);this.error.set(true);}}));}
 limpiar(){this.secciones.set([]);this.error.set(false);}
 inicio(){return this.secciones()[0]?.opciones[0]?.ruta||'/perfil';}
 configuracion(){return this.http.get<ConfiguracionMenu>('/api/menu-admin');}
 guardar(config:ConfiguracionMenu){return this.http.put<ConfiguracionMenu>('/api/menu-admin',{version:config.version,secciones:config.secciones});}
}
