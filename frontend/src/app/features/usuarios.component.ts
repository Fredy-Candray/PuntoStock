import { Component,HostListener,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpClient,HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { A11yModule } from '@angular/cdk/a11y';
import { AuthService } from '../core/api.service';
import { IconComponent } from '../core/icon.component';
interface Usuario {id:number;username:string;nombre:string;correo:string|null;telefono:string|null;activo:boolean;roles:string[];fechaCreacion:string|null}
@Component({standalone:true,imports:[FormsModule,DatePipe,A11yModule,IconComponent],templateUrl:'./usuarios.component.html'})
export class UsuariosComponent {
 http=inject(HttpClient);auth=inject(AuthService);router=inject(Router);
 usuarios=signal<Usuario[]>([]);loading=signal(true);busy=signal(false);error=signal('');mensaje=signal('');editor=signal(false);
 detalle=signal<{usuario:Usuario;fotoPerfil:string|null}|null>(null);confirmar=signal<Usuario|null>(null);id:number|null=null;
 buscar='';rol='';estado='ACTIVO';visible=false;modelo=this.vacio();
 constructor(){this.cargar();}
 vacio(){return {username:'',nombre:'',correo:'',telefono:'',password:'',rol:'VENDEDOR',activo:true};}
 cargar(){this.loading.set(true);this.error.set('');this.http.get<Usuario[]>('/api/usuarios').subscribe({next:u=>{this.usuarios.set(u);this.loading.set(false);},error:e=>{this.loading.set(false);this.fallo(e);}});}
 filtrados(){const q=this.buscar.trim().toLocaleLowerCase('es');return this.usuarios().filter(u=>(!q||[u.nombre,u.username,u.correo??''].join(' ').toLocaleLowerCase('es').includes(q))&&(!this.rol||u.roles.includes(this.rol))&&(!this.estado||u.activo===(this.estado==='ACTIVO')));}
 activos(){return this.usuarios().filter(u=>u.activo).length;}
 admins(){return this.usuarios().filter(u=>u.activo&&u.roles.includes('ADMIN')).length;}
 propio(){return this.id!==null&&this.usuarios().find(u=>u.id===this.id)?.username===this.auth.username();}
 nuevo(){this.id=null;this.modelo=this.vacio();this.visible=false;this.error.set('');this.editor.set(true);}
 editar(u:Usuario){this.id=u.id;this.modelo={username:u.username,nombre:u.nombre,correo:u.correo??'',telefono:u.telefono??'',password:'',rol:u.roles.includes('ADMIN')?'ADMIN':'VENDEDOR',activo:u.activo};this.visible=false;this.error.set('');this.editor.set(true);}
 cerrar(){if(this.busy())return;this.editor.set(false);this.detalle.set(null);this.confirmar.set(null);this.modelo.password='';}
 ver(u:Usuario){this.busy.set(true);this.error.set('');this.http.get<{usuario:Usuario;fotoPerfil:string|null}>('/api/usuarios/'+u.id).subscribe({next:d=>{this.detalle.set(d);this.busy.set(false);},error:e=>{this.busy.set(false);this.fallo(e);}});}
 guardar(){
  if(this.busy())return;if(this.modelo.password&&(this.modelo.password.length<12||new TextEncoder().encode(this.modelo.password).length>72)){this.error.set('La contraseña requiere al menos 12 caracteres y como máximo 72 bytes.');return;}
  const propiaClave=this.propio()&&!!this.modelo.password;this.busy.set(true);this.error.set('');
  const body={...this.modelo,username:this.modelo.username.trim(),nombre:this.modelo.nombre.trim(),correo:this.modelo.correo.trim(),telefono:this.modelo.telefono.trim()};
  const request=this.id===null?this.http.post('/api/usuarios',body):this.http.put('/api/usuarios/'+this.id,body);
  request.subscribe({next:()=>{this.busy.set(false);this.cerrar();if(propiaClave){this.auth.logout();void this.router.navigateByUrl('/login');return;}this.mensaje.set('Usuario guardado correctamente.');this.cargar();if(this.auth.perfil()?.id===this.id)this.auth.cargarPerfil().subscribe({error:()=>{}});},error:e=>{this.busy.set(false);this.fallo(e);}});
 }
 cambiarEstado(){const u=this.confirmar();if(!u||this.busy())return;this.busy.set(true);this.error.set('');this.http.put('/api/usuarios/'+u.id+'/activo',null,{params:{valor:!u.activo}}).subscribe({next:()=>{this.busy.set(false);this.confirmar.set(null);this.mensaje.set(u.activo?'Usuario desactivado. Su historial se conserva.':'Usuario reactivado.');this.cargar();},error:e=>{this.busy.set(false);this.fallo(e);}});}
 fallo(e:HttpErrorResponse){this.error.set(e.error?.message||'No se pudo completar la operación. Inténtalo de nuevo.');}
 @HostListener('document:keydown.escape') escape(){this.cerrar();}
}
