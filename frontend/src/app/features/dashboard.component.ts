import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { Producto,Movimiento } from '../core/api.service';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { IconComponent } from '../core/icon.component';
@Component({standalone:true,imports:[FormsModule,CurrencyPipe,RouterLink,IconComponent],templateUrl:'./dashboard.component.html'})
export class DashboardComponent {
 api=inject(ApiService);fecha=new Intl.DateTimeFormat('en-CA',{timeZone:'America/El_Salvador'}).format(new Date());ganancia=signal(0);error=signal('');
 productos=signal<Producto[]>([]);movimientos=signal<Movimiento[]>([]);loading=signal(true);
 constructor(){this.cargar();}
 cargar(){this.loading.set(true);this.error.set('');forkJoin({reporte:this.api.ganancia(this.fecha),productos:this.api.productos(),movimientos:this.api.movimientos()}).subscribe({next:r=>{this.ganancia.set(r.reporte.ganancia);this.productos.set(r.productos);this.movimientos.set(r.movimientos);this.loading.set(false);},error:()=>{this.loading.set(false);this.error.set('No se pudo cargar el resumen. Actualiza para intentarlo de nuevo.');}});}
 unidades(){return this.productos().reduce((n,p)=>n+p.stock,0);}
 bajoStock(){return this.productos().filter(p=>p.stock<=5);}
 nombreProducto(id:number){return this.productos().find(p=>p.id===id)?.nombre||'Producto #'+id;}
}
