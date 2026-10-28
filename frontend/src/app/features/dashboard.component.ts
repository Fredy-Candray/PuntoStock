import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { Producto, Movimiento } from '../core/api.service';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { IconComponent } from '../core/icon.component';
@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, RouterLink, IconComponent, A11yModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent {
  api = inject(ApiService);
  fecha = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/El_Salvador' }).format(new Date());
  ganancia = signal(0);
  error = signal('');
  productos = signal<Producto[]>([]);
  agotados = computed(() =>
    this.productos()
      .filter((p) => p.tipo !== 'SERVICIO' && p.stock === 0)
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')),
  );
  modalAgotados = signal(false);
  paginaAgotados = signal(0);
  readonly limiteAgotados = 6;
  paginasAgotados = computed(() =>
    Math.max(1, Math.ceil(this.agotados().length / this.limiteAgotados)),
  );
  agotadosVisibles = computed(() => {
    const inicio = this.paginaAgotados() * this.limiteAgotados;
    return this.agotados().slice(inicio, inicio + this.limiteAgotados);
  });
  movimientos = signal<Movimiento[]>([]);
  loading = signal(true);
  constructor() {
    this.cargar();
  }
  cargar() {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      reporte: this.api.ganancia(this.fecha),
      productos: this.api.productos(),
      movimientos: this.api.movimientos(),
    }).subscribe({
      next: (r) => {
        this.ganancia.set(r.reporte.ganancia);
        this.productos.set(r.productos);
        this.paginaAgotados.set(Math.min(this.paginaAgotados(), this.paginasAgotados() - 1));
        this.movimientos.set(r.movimientos);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el resumen. Actualiza para intentarlo de nuevo.');
      },
    });
  }
  unidades() {
    return this.productos()
      .filter((p) => p.tipo !== 'SERVICIO')
      .reduce((n, p) => n + p.stock, 0);
  }
  bajoStock() {
    return this.productos().filter((p) => p.tipo !== 'SERVICIO' && p.stock <= p.stockMinimo);
  }
  abrirAgotados() {
    this.paginaAgotados.set(0);
    this.modalAgotados.set(true);
  }
  cambiarPaginaAgotados(delta: number) {
    const siguiente = this.paginaAgotados() + delta;
    if (siguiente >= 0 && siguiente < this.paginasAgotados()) this.paginaAgotados.set(siguiente);
  }
  @HostListener('document:keydown.escape')
  cerrarAgotados() {
    this.modalAgotados.set(false);
  }
  nombreProducto(id: number) {
    return this.productos().find((p) => p.id === id)?.nombre || 'Producto #' + id;
  }
}
