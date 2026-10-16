import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { IconComponent } from '../core/icon.component';
import { ApiService,Producto,ProductoInput } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,CurrencyPipe,IconComponent],templateUrl:'./productos.component.html'})
export class ProductosComponent {
 api=inject(ApiService);productos=signal<Producto[]>([]);mensaje=signal('');busy=signal(false);id?:number;modelo:ProductoInput=this.vacio();
 vacio():ProductoInput{return {sku:'',nombre:'',descripcion:'',precioCompra:0,precioVenta:0,fotoUrl:''};}
 constructor(){this.cargar();}cargar(){this.api.productos().subscribe({next:p=>this.productos.set(p),error:()=>this.mensaje.set('Error al consultar productos.')});}
 editor=signal(false);buscar='';
 filtrados(){const q=this.buscar.trim().toLowerCase();return this.productos().filter(p=>(p.nombre+' '+p.sku).toLowerCase().includes(q));}
 nuevo(){this.id=undefined;this.modelo=this.vacio();this.editor.set(true);}
 cerrar(){this.id=undefined;this.modelo=this.vacio();this.editor.set(false);}
 editar(p:Producto){this.id=p.id;this.modelo={sku:p.sku,nombre:p.nombre,descripcion:p.descripcion,precioCompra:p.precioCompra,precioVenta:p.precioVenta,fotoUrl:p.fotoUrl};this.editor.set(true);document.getElementById('product-editor')?.scrollIntoView({behavior:'smooth',block:'start'});}
 guardar(){if(this.busy())return;this.busy.set(true);this.api.guardar(this.modelo,this.id).subscribe({next:()=>{this.busy.set(false);this.mensaje.set('Producto guardado.');this.cerrar();this.cargar();},error:()=>{this.busy.set(false);this.mensaje.set('No se pudo guardar; verifique campos y SKU único.');}});}
 eliminar(p:Producto){if(!confirm('¿Desactivar '+p.nombre+'?'))return;this.busy.set(true);this.api.eliminar(p.id).subscribe({next:()=>{this.busy.set(false);this.cargar();},error:()=>{this.busy.set(false);this.mensaje.set('No se pudo desactivar.');}});}
}
