import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { A11yModule } from '@angular/cdk/a11y';
import { Ticket } from './api.service';
import { IconComponent } from './icon.component';

@Component({selector:'ps-ticket',standalone:true,imports:[CurrencyPipe,DatePipe,A11yModule,IconComponent],template:`
 <div class="receipt-modal"><section class="receipt-dialog" role="dialog" aria-modal="true" aria-labelledby="ticket-title" cdkTrapFocus [cdkTrapFocusAutoCapture]="true">
  <div class="receipt-toolbar no-print"><h2 id="ticket-title">Venta confirmada #{{ticket.id}}</h2><button type="button" class="icon-btn" (click)="cerrar.emit()" aria-label="Cerrar comprobante"><ps-icon name="close"/></button></div>
  <article class="receipt-print" aria-label="Comprobante de venta">
   <header><strong>PuntoStock</strong><h3>Comprobante de venta</h3><p>Venta {{numero()}}</p><p>{{ticket.fecha|date:'dd/MM/yyyy HH:mm':'-0600'}}</p></header>
   <div class="receipt-meta"><p>Atendió: {{ticket.vendedor}}</p><p>Cliente: {{ticket.cliente||'Público general'}}</p></div>
   <table><thead><tr><th>Detalle</th><th class="text-right">Importe</th></tr></thead><tbody>@for(l of ticket.lineas;track l.productoId){<tr><td><strong>{{l.nombre}}</strong><small>{{l.sku}} · {{l.cantidad}} × {{l.precioUnitario|currency:'USD'}}</small>@if(l.observaciones){<small>{{l.observaciones}}</small>}</td><td class="text-right">{{l.subtotal|currency:'USD'}}</td></tr>}</tbody></table>
   <div class="receipt-total"><span>Total</span><strong>{{ticket.total|currency:'USD'}}</strong></div>
   <div class="receipt-payment"><p><span>Medio de pago</span><strong>{{medio()}}</strong></p>@if(ticket.recibido!==null){<p><span>{{ticket.metodoPago==='EFECTIVO'?'Recibido':'Pagado'}}</span><span>{{ticket.recibido|currency:'USD'}}</span></p>}@if(ticket.metodoPago==='EFECTIVO'&&ticket.cambio!==null){<p><span>Cambio</span><strong>{{ticket.cambio|currency:'USD'}}</strong></p>}@if(ticket.referenciaPago){<p><span>Referencia</span><span>{{ticket.referenciaPago}}</span></p>}</div>
   <footer>Gracias por su compra.<small>Comprobante de la operación registrada en PuntoStock.</small></footer>
  </article>
  <div class="receipt-actions no-print"><button type="button" class="btn btn-secondary" (click)="cerrar.emit()">Continuar</button><button type="button" class="btn btn-primary" (click)="imprimir()"><ps-icon name="printer"/> Imprimir comprobante</button></div>
 </section></div>
`})
export class TicketComponent {
 @Input({required:true}) ticket!:Ticket;@Output() cerrar=new EventEmitter<void>();
 numero(){return 'V-'+String(this.ticket.id).padStart(6,'0');}
 medio(){return ({EFECTIVO:'Efectivo',TARJETA:'Tarjeta',TRANSFERENCIA:'Transferencia',SIN_REGISTRO:'No registrado'} as Record<string,string>)[this.ticket.metodoPago]||this.ticket.metodoPago;}
 imprimir(){window.print();}
 @HostListener('document:keydown.escape') escape(){this.cerrar.emit();}
}
