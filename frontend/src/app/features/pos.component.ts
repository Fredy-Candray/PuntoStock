import { Component, ElementRef, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../core/icon.component';
import { TicketComponent } from '../core/ticket.component';
import { ApiService, AuthService, Cobro, MedioPago, Producto, Ticket } from '../core/api.service';
import { totalCentavos } from '../core/dinero';
import { CajaService } from '../core/caja.service';
interface LineaCarrito {
  producto: Producto;
  cantidad: number;
  observaciones: string;
  disponible: boolean;
}
@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, RouterLink, IconComponent, TicketComponent],
  templateUrl: './pos.component.html',
})
export class PosComponent {
  readonly api = inject(ApiService);
  readonly cajaApi = inject(CajaService);
  cajaAbierta = signal<boolean | null>(null);
  errorCaja = signal(false);
  readonly auth = inject(AuthService);
  readonly buscador = viewChild<ElementRef<HTMLInputElement>>('buscador');
  productos = signal<Producto[]>([]);
  carrito = signal<LineaCarrito[]>([]);
  busy = signal(false);
  loading = signal(true);
  mensaje = signal('');
  messageError = signal(false);
  private avisoDeBusqueda = false;
  comprobante = signal<Ticket | null>(null);
  pendiente = signal<Cobro | null>(null);
  buscar = '';
  categoria = '';
  tipo = 'TODOS';
  readonly limitePagina = 6;
  pagina = 0;
  metodo: MedioPago = 'EFECTIVO';
  recibido: number | null = null;
  cliente = '';
  referencia = '';
  constructor() {
    this.restaurar();
    this.cargar();
    afterNextRender(() => this.buscador()?.nativeElement.focus());
  }
  private clave() {
    return 'puntostock.cobro.pendiente:' + this.auth.username();
  }
  private restaurar() {
    try {
      const saved = JSON.parse(sessionStorage.getItem(this.clave()) ?? 'null');
      if (
        saved &&
        typeof saved.cobro?.solicitudId === 'string' &&
        Array.isArray(saved.cobro.lineas) &&
        saved.cobro.lineas.length &&
        Array.isArray(saved.carrito) &&
        saved.carrito.every(
          (l: LineaCarrito) =>
            l.producto &&
            typeof l.producto.nombre === 'string' &&
            Number.isInteger(l.cantidad) &&
            l.cantidad > 0,
        )
      ) {
        this.pendiente.set(saved.cobro);
        this.carrito.set(saved.carrito);
        this.metodo = saved.cobro.metodoPago;
        this.recibido = saved.cobro.recibido;
        this.cliente = saved.cobro.cliente;
        this.referencia = saved.cobro.referenciaPago;
        this.aviso(
          'Hay una venta pendiente de confirmación. Consulta su estado o reintenta la misma operación.',
          true,
        );
      }
    } catch {
      try {
        sessionStorage.removeItem(this.clave());
      } catch {}
    }
  }
  cargar() {
    this.cajaAbierta.set(null);
    this.errorCaja.set(false);
    this.cajaApi.estado().subscribe({
      next: (c) => this.cajaAbierta.set(c.abierta),
      error: () => this.errorCaja.set(true),
    });
    this.loading.set(true);
    this.api.productos().subscribe({
      next: (p) => {
        const list = p.map((x) => ({
          ...x,
          tipo: x.tipo ?? 'PRODUCTO',
          categoria: x.categoria ?? 'General',
          unidad: x.unidad ?? 'UNIDAD',
          stockMinimo: x.stockMinimo ?? 5,
          marca: x.marca ?? '',
          ubicacion: x.ubicacion ?? '',
        }));
        this.productos.set(list);
        this.pagina = Math.min(this.pagina, Math.max(0, this.paginas() - 1));
        if (!this.pendiente())
          this.carrito.update((items) =>
            items.map((l) => {
              const product = list.find((x) => x.id === l.producto.id);
              return { ...l, producto: product ?? l.producto, disponible: !!product };
            }),
          );
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.aviso(
          'No se pudo consultar el catálogo. Comprueba la conexión con el servidor.',
          true,
        );
      },
    });
  }
  private normalizar(value: string) {
    return value
      .toLocaleLowerCase('es')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
  categorias() {
    return [...new Set(this.productos().map((p) => p.categoria))].sort((a, b) =>
      a.localeCompare(b, 'es'),
    );
  }
  filtrados() {
    const q = this.normalizar(this.buscar.trim());
    return this.productos().filter(
      (p) =>
        (this.tipo === 'TODOS' || p.tipo === this.tipo) &&
        (!this.categoria || p.categoria === this.categoria) &&
        this.normalizar([p.sku, p.nombre, p.descripcion, p.categoria, p.marca].join(' ')).includes(
          q,
        ),
    );
  }
  paginas() {
    return Math.ceil(this.filtrados().length / this.limitePagina);
  }
  visibles() {
    const inicio = this.pagina * this.limitePagina;
    return this.filtrados().slice(inicio, inicio + this.limitePagina);
  }
  reiniciarPagina() {
    this.pagina = 0;
    this.limpiarAvisoBusqueda();
  }
  cambiarPagina(delta: number) {
    const siguiente = this.pagina + delta;
    if (siguiente >= 0 && siguiente < this.paginas()) this.pagina = siguiente;
  }
  servicio(p: Producto) {
    return p.tipo === 'SERVICIO';
  }
  unidad(p: Producto) {
    return (
      (
        {
          UNIDAD: 'unidad',
          PAQUETE: 'paquete',
          CAJA: 'caja',
          RESMA: 'resma',
          DOCENA: 'docena',
          PAGINA: 'página',
          FOTOGRAFIA: 'fotografía',
          TRABAJO: 'trabajo',
        } as Record<string, string>
      )[p.unidad] || p.unidad.toLowerCase()
    );
  }
  bloqueado() {
    return this.busy() || !!this.pendiente();
  }
  agregarCodigo(event: Event) {
    event.preventDefault();
    if (this.bloqueado()) return;
    const codigo = this.buscar.trim().toLowerCase();
    if (!codigo) return;
    const p = this.productos().find((x) => x.sku.toLowerCase() === codigo);
    if (!p) {
      this.aviso('No se encontró un artículo o servicio con ese código exacto.', true, true);
      return;
    }
    if (this.agregar(p)) {
      this.buscar = '';
      this.reiniciarPagina();
      this.buscador()?.nativeElement.focus();
    }
  }
  agregar(p: Producto) {
    if (this.bloqueado()) return false;
    if (p.precioVenta <= 0) {
      this.aviso('Configura un precio de venta para ' + p.nombre + '.', true);
      return false;
    }
    const items = this.carrito().map((x) => ({ ...x })),
      l = items.find((x) => x.producto.id === p.id),
      cantidad = (l?.cantidad ?? 0) + 1;
    if (cantidad > 100000 || (!this.servicio(p) && cantidad > p.stock)) {
      this.aviso('No hay existencias suficientes de ' + p.nombre + '.', true);
      return false;
    }
    if (!l && items.length >= 100) {
      this.aviso('La venta admite hasta 100 artículos o servicios diferentes.', true);
      return false;
    }
    if (l) l.cantidad = cantidad;
    else items.push({ producto: p, cantidad: 1, observaciones: '', disponible: true });
    this.carrito.set(items);
    this.limpiarAvisoBusqueda();
    return true;
  }
  cantidad(id: number, value: unknown) {
    if (this.bloqueado()) return;
    const n = Number(value),
      l = this.carrito().find((x) => x.producto.id === id);
    if (!l) return;
    const cantidad = Number.isInteger(n) && n >= 1 && n <= 100000 ? n : 0;
    this.carrito.update((xs) => xs.map((x) => (x.producto.id === id ? { ...x, cantidad } : x)));
    if (!cantidad || (!this.servicio(l.producto) && cantidad > l.producto.stock))
      this.aviso('Revisa la cantidad y las existencias disponibles.', true);
  }
  quitar(id: number) {
    if (!this.bloqueado()) this.carrito.update((xs) => xs.filter((x) => x.producto.id !== id));
  }
  disminuir(id: number) {
    const l = this.carrito().find((x) => x.producto.id === id);
    if (l) {
      if (l.cantidad === 1) this.quitar(id);
      else this.cantidad(id, l.cantidad - 1);
    }
  }
  vaciar() {
    if (!this.bloqueado()) {
      this.carrito.set([]);
      this.recibido = null;
      this.cliente = '';
      this.referencia = '';
      this.mensaje.set('');
    }
  }
  totalCentavos() {
    return totalCentavos(this.carrito());
  }
  total() {
    return this.totalCentavos() / 100;
  }
  unidades() {
    return this.carrito().reduce((s, l) => s + l.cantidad, 0);
  }
  cambio() {
    return this.metodo === 'EFECTIVO' && this.recibido !== null
      ? Math.max(0, (Math.round(this.recibido * 100) - this.totalCentavos()) / 100)
      : 0;
  }
  puedeCobrar() {
    return (
      !!this.pendiente() ||
      (this.cajaAbierta() === true &&
        !!this.carrito().length &&
        this.carrito().every(
          (l) =>
            l.disponible &&
            l.producto.precioVenta > 0 &&
            Number.isInteger(l.cantidad) &&
            l.cantidad > 0 &&
            (this.servicio(l.producto) || l.cantidad <= l.producto.stock),
        ) &&
        (this.metodo !== 'EFECTIVO' ||
          (this.recibido !== null &&
            Number.isFinite(this.recibido) &&
            Math.abs(this.recibido * 100 - Math.round(this.recibido * 100)) < 0.000001 &&
            Math.round(this.recibido * 100) >= this.totalCentavos())))
    );
  }
  cambiarPago() {
    this.recibido = null;
    this.referencia = '';
  }
  cobrar() {
    if (this.busy() || !this.puedeCobrar()) return;
    let cobro = this.pendiente();
    if (!cobro) {
      cobro = {
        lineas: this.carrito().map((l) => ({
          productoId: l.producto.id,
          cantidad: l.cantidad,
          observaciones: l.observaciones.trim(),
        })),
        metodoPago: this.metodo,
        recibido: this.metodo === 'EFECTIVO' ? this.recibido : null,
        totalEsperado: this.total(),
        cliente: this.cliente.trim(),
        referenciaPago: this.referencia.trim(),
        solicitudId: crypto.randomUUID(),
      };
      this.pendiente.set(cobro);
      try {
        sessionStorage.setItem(this.clave(), JSON.stringify({ cobro, carrito: this.carrito() }));
      } catch {}
    }
    this.busy.set(true);
    this.mensaje.set('');
    this.api.vender(cobro).subscribe({
      next: (r) => this.confirmada(r),
      error: (e) => {
        this.busy.set(false);
        if (e.status >= 400 && e.status < 500 && e.status !== 401) {
          this.limpiarPendiente();
          this.aviso(
            e.error?.message ||
              'Venta rechazada. Revisa los datos y la disponibilidad antes de cobrar.',
            true,
          );
          this.cargar();
        } else
          this.aviso(
            'No se pudo confirmar la venta. No repitas el cobro: consulta su estado o reintenta esta misma operación.',
            true,
          );
      },
    });
  }
  recuperar() {
    const cobro = this.pendiente();
    if (!cobro || this.busy()) return;
    this.busy.set(true);
    this.api.recuperarVenta(cobro.solicitudId).subscribe({
      next: (r) => this.confirmada(r),
      error: (e) => {
        this.busy.set(false);
        this.aviso(
          e.status === 404
            ? 'Todavía no hay una confirmación. Reintenta la misma operación para registrarla una sola vez.'
            : 'No se pudo consultar el estado. Comprueba la conexión y vuelve a consultar.',
          true,
        );
      },
    });
  }
  private limpiarPendiente() {
    this.pendiente.set(null);
    try {
      sessionStorage.removeItem(this.clave());
    } catch {}
  }
  private confirmada(ticket: Ticket) {
    this.busy.set(false);
    this.limpiarPendiente();
    this.carrito.set([]);
    this.recibido = null;
    this.cliente = '';
    this.referencia = '';
    this.comprobante.set(ticket);
    this.aviso('Venta #' + ticket.id + ' confirmada. Total: $' + ticket.total.toFixed(2) + '.');
    this.cargar();
  }
  private limpiarAvisoBusqueda() {
    if (!this.avisoDeBusqueda) return;
    this.mensaje.set('');
    this.messageError.set(false);
    this.avisoDeBusqueda = false;
  }
  private aviso(texto: string, error = false, busqueda = false) {
    this.avisoDeBusqueda = busqueda;
    this.messageError.set(error);
    this.mensaje.set(texto);
  }
}
