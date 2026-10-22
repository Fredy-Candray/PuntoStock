import { Component, EventEmitter, HostListener, Input, Output, OnChanges, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { A11yModule } from '@angular/cdk/a11y';
import { Ticket } from './api.service';
import { IconComponent } from './icon.component';
import { compartirComprobante } from './ticket-share';

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
  @if(errorDescarga()){<div class="notice notice-error no-print" role="alert">{{errorDescarga()}}</div>}
  @if(mensajeCompartir()){<div class="notice notice-info no-print" role="status"><div>{{mensajeCompartir()}}@if(alternativaCompartir()){<button type="button" class="btn btn-secondary btn-small" (click)="descargar()" [disabled]="descargando()">Descargar para adjuntar</button>}</div></div>}
  @if(!preparando()&&!archivoCompartir()){<div class="no-print"><button type="button" class="btn btn-quiet" (click)="prepararCompartir()">Volver a preparar el PDF para compartir</button></div>}
  <div class="receipt-actions no-print"><button type="button" class="btn btn-secondary" (click)="cerrar.emit()">Continuar</button><button type="button" class="btn btn-secondary" (click)="descargar()" [disabled]="descargando()||compartiendo()"><ps-icon name="arrowDown"/> {{descargando()?'Generando…':'Descargar PDF'}}</button><button type="button" class="btn btn-secondary" (click)="compartir()" [disabled]="preparando()||!archivoCompartir()||compartiendo()||descargando()"><ps-icon name="share"/> {{preparando()?'Preparando PDF…':compartiendo()?'Compartiendo…':'Compartir PDF'}}</button><button type="button" class="btn btn-primary" (click)="imprimir()"><ps-icon name="printer"/> Imprimir comprobante</button></div>
 </section></div>
`})
export class TicketComponent implements OnChanges {
 @Input({required:true}) ticket!:Ticket;@Output() cerrar=new EventEmitter<void>();
 numero(){return 'V-'+String(this.ticket.id).padStart(6,'0');}
 medio(){return ({EFECTIVO:'Efectivo',TARJETA:'Tarjeta',TRANSFERENCIA:'Transferencia',SIN_REGISTRO:'No registrado'} as Record<string,string>)[this.ticket.metodoPago]||this.ticket.metodoPago;}
 imprimir(){window.print();}
 descargando=signal(false);errorDescarga=signal('');
 preparando=signal(true);compartiendo=signal(false);archivoCompartir=signal<File|null>(null);
 mensajeCompartir=signal('');alternativaCompartir=signal(false);
 ngOnChanges(){void this.prepararCompartir();}
 async prepararCompartir(){
  const ticket=this.ticket;
  this.archivoCompartir.set(null);this.preparando.set(true);this.errorDescarga.set('');this.mensajeCompartir.set('');this.alternativaCompartir.set(false);
  try{
   const {crearComprobantePdf}=await import('./ticket-pdf');
   if(this.ticket!==ticket)return;
   const blob=crearComprobantePdf(ticket).output('blob');
   this.archivoCompartir.set(new File([blob],'PuntoStock-'+this.numero()+'.pdf',{type:'application/pdf'}));
  }catch{if(this.ticket===ticket)this.errorDescarga.set('No se pudo preparar el PDF para compartir. Puedes reintentarlo o utilizar Descargar PDF.');}
  finally{if(this.ticket===ticket)this.preparando.set(false);}
 }
 async compartir(){
  const archivo=this.archivoCompartir();if(!archivo||this.compartiendo())return;
  this.compartiendo.set(true);this.mensajeCompartir.set('');this.alternativaCompartir.set(false);
  try{
   const resultado=await compartirComprobante(archivo);
   if(resultado==='no-disponible'){
    this.alternativaCompartir.set(true);
    this.mensajeCompartir.set('No se pudo abrir el menú para compartir archivos en este navegador. Descarga el PDF y adjúntalo en WhatsApp o en tu correo.');
   }else if(resultado==='cancelado')this.mensajeCompartir.set('Se cerró el menú de compartir. Puedes volver a intentarlo.');
  }finally{this.compartiendo.set(false);}
 }
 async descargar(){
  if(this.descargando())return;
  this.descargando.set(true);this.errorDescarga.set('');
  try{const {crearComprobantePdf}=await import('./ticket-pdf');await crearComprobantePdf(this.ticket).save('PuntoStock-'+this.numero()+'.pdf',{returnPromise:true});}
  catch{this.errorDescarga.set('No se pudo generar el PDF. Inténtalo de nuevo o utiliza Imprimir.');}
  finally{this.descargando.set(false);}
 }
 @HostListener('document:keydown.escape') escape(){this.cerrar.emit();}
}
