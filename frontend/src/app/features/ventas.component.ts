import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ApiService, AuthService, HistorialVentas, Ticket } from '../core/api.service';
import { IconComponent } from '../core/icon.component';
import { TicketComponent } from '../core/ticket.component';

@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, IconComponent, TicketComponent],
  templateUrl: './ventas.component.html',
})
export class VentasComponent {
  readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  fecha = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/El_Salvador' }).format(new Date());
  datos = signal<HistorialVentas | null>(null);
  loading = signal(false);
  error = signal('');
  comprobante = signal<Ticket | null>(null);
  abriendo = signal<number | null>(null);
  constructor() {
    this.cargar();
  }
  cargar(pagina = 0) {
    this.loading.set(true);
    this.datos.set(null);
    this.error.set('');
    this.api.historialVentas(this.fecha, pagina).subscribe({
      next: (r) => {
        this.datos.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo consultar el historial de ventas.');
      },
    });
  }
  abrir(id: number) {
    if (this.abriendo() !== null) return;
    this.abriendo.set(id);
    this.error.set('');
    this.api.ticket(id).subscribe({
      next: (t) => {
        this.comprobante.set(t);
        this.abriendo.set(null);
      },
      error: () => {
        this.abriendo.set(null);
        this.error.set('No se pudo recuperar el comprobante.');
      },
    });
  }
  medio(value: string) {
    return (
      (
        {
          EFECTIVO: 'Efectivo',
          TARJETA: 'Tarjeta',
          TRANSFERENCIA: 'Transferencia',
          SIN_REGISTRO: 'No registrado',
        } as Record<string, string>
      )[value] || value
    );
  }
}
