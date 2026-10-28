import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Producto, Movimiento } from '../core/api.service';
import { DatePipe } from '@angular/common';
import { IconComponent } from '../core/icon.component';
@Component({
  standalone: true,
  imports: [FormsModule, DatePipe, IconComponent],
  templateUrl: './inventario.component.html',
})
export class InventarioComponent {
  api = inject(ApiService);
  productos = signal<Producto[]>([]);
  movimientos = signal<Movimiento[]>([]);
  mensaje = signal('');
  busy = signal(false);
  pagina = 0;
  paginas = signal(0);
  total = signal(0);
  desde = '';
  hasta = '';
  filtroProducto = '';
  private consultaMovimientos = 0;
  cargarMovimientos() {
    const consulta = ++this.consultaMovimientos;
    this.api.movimientosPagina(this.pagina, this.desde, this.hasta, this.filtroProducto).subscribe({
      next: (p) => {
        if (consulta !== this.consultaMovimientos) return;
        this.movimientos.set(p.contenido);
        this.paginas.set(p.paginas);
        this.total.set(p.cantidad);
      },
      error: () => {
        if (consulta === this.consultaMovimientos)
          this.mensaje.set('No se pudo consultar movimientos.');
      },
    });
  }
  cambiarPagina(delta: number) {
    this.pagina += delta;
    this.cargarMovimientos();
  }
  filtrar() {
    this.pagina = 0;
    this.cargarMovimientos();
  }
  limpiarFiltros() {
    this.desde = '';
    this.hasta = '';
    this.filtroProducto = '';
    this.filtrar();
  }
  productoId: number | null = null;
  tipo = 'COMPRA';
  cantidad = 1;
  motivo = '';
  tipos = ['COMPRA', 'DEVOLUCION_CLIENTE', 'DEVOLUCION_PROVEEDOR', 'MERMA', 'AJUSTE'];
  constructor() {
    this.cargar();
  }
  cargar() {
    this.api.productos().subscribe({
      next: (p) => this.productos.set(p.filter((x) => x.tipo !== 'SERVICIO')),
      error: () => this.mensaje.set('No se pudo consultar stock.'),
    });
    this.cargarMovimientos();
  }
  tipoNombre(tipo: string) {
    return (
      (
        {
          COMPRA: 'Compra / entrada',
          DEVOLUCION_CLIENTE: 'Devolución de cliente',
          DEVOLUCION_PROVEEDOR: 'Devolución al proveedor',
          MERMA: 'Merma / salida',
          AJUSTE: 'Ajuste manual',
          VENTA: 'Venta',
        } as Record<string, string>
      )[tipo] || tipo
    );
  }
  nombreProducto(id: number) {
    return this.productos().find((p) => p.id === id)?.nombre || 'Producto #' + id;
  }
  esSalida() {
    return ['MERMA', 'DEVOLUCION_PROVEEDOR'].includes(this.tipo);
  }
  delta() {
    return this.esSalida() ? -Math.abs(this.cantidad) : this.cantidad;
  }
  cambiarTipo() {
    if (this.tipo !== 'AJUSTE') this.cantidad = Math.abs(this.cantidad) || 1;
  }
  guardar() {
    if (!this.productoId || this.busy() || !Number.isInteger(this.cantidad) || !this.cantidad)
      return;
    this.busy.set(true);
    this.api
      .mover({
        productoId: this.productoId,
        tipo: this.tipo,
        cantidad: this.delta(),
        motivo: this.motivo,
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.mensaje.set('Movimiento registrado.');
          this.motivo = '';
          this.cargar();
        },
        error: () => {
          this.busy.set(false);
          this.mensaje.set('Movimiento rechazado: revise cantidad, motivo y stock disponible.');
        },
      });
  }
}
