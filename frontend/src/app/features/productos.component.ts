import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ApiService,Producto,ProductoInput } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,MatButtonModule],template:`<h1>Gestión de productos</h1><form #f="ngForm" (ngSubmit)="guardar()"><label>SKU<input name="sku" [(ngModel)]="modelo.sku" required maxlength="80"></label><label>Nombre<input name="nombre" [(ngModel)]="modelo.nombre" required maxlength="160"></label><label>Descripción<textarea name="descripcion" [(ngModel)]="modelo.descripcion" required maxlength="2000"></textarea></label><label>Costo<input name="costo" type="number" min="0" step="0.01" [(ngModel)]="modelo.precioCompra" required></label><label>Precio<input name="precio" type="number" min="0" step="0.01" [(ngModel)]="modelo.precioVenta" required></label><label>URL o ruta de foto<input name="foto" [(ngModel)]="modelo.fotoUrl" required maxlength="1000"></label><button mat-flat-button [disabled]="f.invalid||busy()">{{id?'Actualizar':'Crear'}}</button><button type="button" (click)="nuevo()">Nuevo</button></form><p role="status">{{mensaje()}}</p><table><thead><tr><th>SKU</th><th>Nombre</th><th>Stock</th><th>Acciones</th></tr></thead><tbody>@for(p of productos();track p.id){<tr><td>{{p.sku}}</td><td>{{p.nombre}}</td><td>{{p.stock}}</td><td><button (click)="editar(p)">Editar</button> <button [disabled]="busy()" (click)="eliminar(p)">Desactivar</button></td></tr>}</tbody></table>`})
export class ProductosComponent {
 api=inject(ApiService);productos=signal<Producto[]>([]);mensaje=signal('');busy=signal(false);id?:number;modelo:ProductoInput=this.vacio();
 vacio():ProductoInput{return {sku:'',nombre:'',descripcion:'',precioCompra:0,precioVenta:0,fotoUrl:''};}
 constructor(){this.cargar();}cargar(){this.api.productos().subscribe({next:p=>this.productos.set(p),error:()=>this.mensaje.set('Error al consultar productos.')});}
 nuevo(){this.id=undefined;this.modelo=this.vacio();}
 editar(p:Producto){this.id=p.id;this.modelo={sku:p.sku,nombre:p.nombre,descripcion:p.descripcion,precioCompra:p.precioCompra,precioVenta:p.precioVenta,fotoUrl:p.fotoUrl};}
 guardar(){if(this.busy())return;this.busy.set(true);this.api.guardar(this.modelo,this.id).subscribe({next:()=>{this.busy.set(false);this.mensaje.set('Producto guardado.');this.nuevo();this.cargar();},error:()=>{this.busy.set(false);this.mensaje.set('No se pudo guardar; verifique campos y SKU único.');}});}
 eliminar(p:Producto){if(!confirm('¿Desactivar '+p.nombre+'?'))return;this.busy.set(true);this.api.eliminar(p.id).subscribe({next:()=>{this.busy.set(false);this.cargar();},error:()=>{this.busy.set(false);this.mensaje.set('No se pudo desactivar.');}});}
}
