import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { IconComponent } from '../core/icon.component';
import { ApiService, Producto } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,CurrencyPipe,IconComponent],templateUrl:'./pos.component.html'})
export class PosComponent {
 api=inject(ApiService);productos=signal<Producto[]>([]);carrito=signal<{producto:Producto;cantidad:number}[]>([]);buscar='';busy=signal(false);mensaje=signal('');
 loading=signal(true);messageError=signal(false);
 constructor(){this.cargar();}cargar(){this.loading.set(true);this.api.productos().subscribe({next:p=>{this.productos.set(p);this.loading.set(false);},error:()=>{this.loading.set(false);this.messageError.set(true);this.mensaje.set('No se pudo consultar productos.');}});}
 filtrados(){const q=this.buscar.toLowerCase();return this.productos().filter(p=>(p.sku+' '+p.nombre).toLowerCase().includes(q));}
 agregar(p:Producto){if(p.stock<=0||this.busy())return;const items=this.carrito().map(x=>({...x}));const l=items.find(x=>x.producto.id===p.id);if(l){if(l.cantidad>=p.stock)return;l.cantidad++;}else items.push({producto:p,cantidad:1});this.carrito.set(items);}
 quitar(id:number){this.carrito.update(xs=>xs.filter(x=>x.producto.id!==id));}
 total(){return this.carrito().reduce((s,l)=>s+l.producto.precioVenta*l.cantidad,0);}
 unidades(){return this.carrito().reduce((s,l)=>s+l.cantidad,0);}
 disminuir(id:number){if(this.busy())return;this.carrito.update(items=>items.map(l=>l.producto.id===id?{...l,cantidad:l.cantidad-1}:l).filter(l=>l.cantidad>0));}
 cobrar(){if(this.busy()||!this.carrito().length)return;this.busy.set(true);this.mensaje.set('');this.api.vender(this.carrito().map(l=>({productoId:l.producto.id,cantidad:l.cantidad}))).subscribe({next:r=>{this.busy.set(false);this.messageError.set(false);this.carrito.set([]);this.mensaje.set('Venta registrada #'+r.id+' · Total confirmado: $'+r.total.toFixed(2));this.cargar();},error:e=>{this.busy.set(false);this.messageError.set(true);this.mensaje.set(e.status===409?'Stock insuficiente. Actualice el carrito.':'No se pudo confirmar la venta. Verifique su estado antes de volver a cobrar.');this.cargar();}});}
}
