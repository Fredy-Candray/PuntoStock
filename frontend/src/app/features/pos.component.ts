import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { ApiService, Producto } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,CurrencyPipe,MatButtonModule],template:`<h1>Punto de venta</h1><input aria-label="Buscar por SKU o nombre" placeholder="Buscar SKU o nombre" [(ngModel)]="buscar"><table><thead><tr><th>Producto</th><th>Stock</th><th>Precio</th><th></th></tr></thead><tbody>@for(p of filtrados();track p.id){<tr><td>{{p.sku}} · {{p.nombre}}<small> {{p.descripcion}}</small></td><td>{{p.stock}}</td><td>{{p.precioVenta|currency:'USD'}}</td><td><button mat-button [disabled]="busy()||p.stock===0" (click)="agregar(p)">Agregar</button></td></tr>}</tbody></table><h2>Venta actual</h2>@for(l of carrito();track l.producto.id){<p>{{l.producto.nombre}} × {{l.cantidad}} <button [disabled]="busy()" (click)="quitar(l.producto.id)">Quitar</button></p>}<p>Total estimado: {{total()|currency:'USD'}}</p><button mat-flat-button (click)="cobrar()" [disabled]="busy()||!carrito().length">Registrar venta</button><p role="status">{{mensaje()}}</p>`})
export class PosComponent {
 api=inject(ApiService);productos=signal<Producto[]>([]);carrito=signal<{producto:Producto;cantidad:number}[]>([]);buscar='';busy=signal(false);mensaje=signal('');
 constructor(){this.cargar();}cargar(){this.api.productos().subscribe({next:p=>this.productos.set(p),error:()=>this.mensaje.set('No se pudo consultar productos.')});}
 filtrados(){const q=this.buscar.toLowerCase();return this.productos().filter(p=>(p.sku+' '+p.nombre).toLowerCase().includes(q));}
 agregar(p:Producto){const items=this.carrito().map(x=>({...x}));const l=items.find(x=>x.producto.id===p.id);if(l){if(l.cantidad>=p.stock)return;l.cantidad++;}else items.push({producto:p,cantidad:1});this.carrito.set(items);}
 quitar(id:number){this.carrito.update(xs=>xs.filter(x=>x.producto.id!==id));}
 total(){return this.carrito().reduce((s,l)=>s+l.producto.precioVenta*l.cantidad,0);}
 cobrar(){if(this.busy()||!this.carrito().length)return;this.busy.set(true);this.api.vender(this.carrito().map(l=>({productoId:l.producto.id,cantidad:l.cantidad}))).subscribe({next:r=>{this.busy.set(false);this.carrito.set([]);this.mensaje.set('Venta registrada #'+r.id+' · Total confirmado: $'+r.total.toFixed(2));this.cargar();},error:e=>{this.busy.set(false);this.mensaje.set(e.status===409?'Stock insuficiente. Actualice el carrito.':'No se pudo confirmar la venta. Verifique su estado antes de volver a cobrar.');this.cargar();}});}
}
