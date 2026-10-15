import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { ApiService } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,CurrencyPipe],template:`<h1>Dashboard</h1><div class="card"><label>Fecha<input type="date" [(ngModel)]="fecha" (change)="cargar()"></label><h2>Ganancia bruta: {{ganancia() | currency:'USD'}}</h2><p>Precios y costos históricos de las ventas, en horario de El Salvador.</p><p role="alert">{{error()}}</p></div>`})
export class DashboardComponent {
 api=inject(ApiService);fecha=new Intl.DateTimeFormat('en-CA',{timeZone:'America/El_Salvador'}).format(new Date());ganancia=signal(0);error=signal('');
 constructor(){this.cargar();}cargar(){this.api.ganancia(this.fecha).subscribe({next:r=>{this.ganancia.set(r.ganancia);this.error.set('');},error:()=>this.error.set('No se pudo cargar el reporte.')});}
}
