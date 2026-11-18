import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { ApiService, AuthService, MedioPago, Pagina, Producto } from '../core/api.service';
import { IconComponent } from '../core/icon.component';

type Estado = 'PENDIENTE' | 'LISTO' | 'ENTREGADO' | 'CANCELADO';
interface Pedido {
  id: number;
  cliente: string;
  descripcion: string;
  servicios: string;
  cantidad: number;
  precioUnitario: number;
  total: number;
  saldo: number;
  entrega: string;
  estado: Estado;
}
interface Abono {
  id: number;
  fecha: string;
  monto: number;
  saldo: number;
  metodoPago: MedioPago;
}
interface Detalle {
  pedido: Pedido;
  abonos: Abono[];
  materiales: {
    id: number;
    nombre: string;
    unidad: string;
    cantidad: number;
    fecha: string;
    costoUnitario: number | null;
    costoTotal: number | null;
  }[];
  costoMateriales: number | null;
}
interface Operacion {
  metodo: 'POST' | 'PUT';
  ruta: string;
  cuerpo: object;
}

@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, IconComponent],
  templateUrl: './pedidos.component.html',
  styleUrl: './pedidos.component.css',
})
export class PedidosComponent {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  private readonly injector = inject(Injector);
  private readonly tarjeta = viewChild<ElementRef<HTMLElement>>('tarjetaPedido');
  private readonly cabecera = viewChild<ElementRef<HTMLElement>>('cabeceraPedidos');
  private readonly dialogo = viewChild<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly url = '/api/pedidos';
  private readonly clave = 'puntostock.pedido.pendiente:' + this.auth.username();
  readonly estados: Record<Estado, string> = {
    PENDIENTE: 'Pendiente',
    LISTO: 'Listo',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado',
  };
  readonly medios: Record<MedioPago, string> = {
    EFECTIVO: 'Efectivo',
    TARJETA: 'Tarjeta',
    TRANSFERENCIA: 'Transferencia',
  };
  readonly opcionesPago: MedioPago[] = ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'];
  lista = signal<Pagina<Pedido> | null>(null);
  detalle = signal<Detalle | null>(null);
  materiales = signal<Producto[]>([]);
  loading = signal(false);
  consultando = signal(false);
  busy = signal(false);
  nuevo = signal(false);
  editando = signal<Pedido | null>(null);
  cancelando = signal<Detalle | null>(null);
  pendiente = signal<Operacion | null>(null);
  error = signal('');
  errorCatalogo = signal('');
  aviso = signal('');
  borrador = {
    cliente: '',
    descripcion: '',
    servicios: '',
    cantidad: 1,
    precioUnitario: null as number | null,
    entrega: '',
  };
  anticipo: number | null = null;
  metodoInicial: MedioPago = 'EFECTIVO';
  monto: number | null = null;
  medio: MedioPago = 'EFECTIVO';
  materialId: number | null = null;
  cantidadMaterial = 1;

  costoEstimado(): number | null {
    const costo = this.materiales().find((p) => p.id === this.materialId)?.precioCompra;
    if (costo == null || !Number.isInteger(this.cantidadMaterial) || this.cantidadMaterial < 1)
      return null;
    return (Math.round(costo * 100) * this.cantidadMaterial) / 100;
  }

  constructor() {
    try {
      const op = JSON.parse(sessionStorage.getItem(this.clave) ?? 'null') as Operacion | null;
      if (
        op &&
        /^\/api\/pedidos(?:\/\d+(?:\/(abonos|materiales|estado))?)?$/.test(op.ruta) &&
        ['POST', 'PUT'].includes(op.metodo) &&
        op.cuerpo
      )
        this.pendiente.set(op);
    } catch {}
    this.cargar();
    this.cargarMateriales();
  }

  cargar(pagina = 0) {
    if (this.loading()) return;
    this.loading.set(true);
    this.http
      .get<Pagina<Pedido>>(this.url, { params: { pagina } })
      .pipe(
        timeout(20000),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (r) => this.lista.set(r),
        error: () => this.error.set('No se pudo consultar la lista de pedidos.'),
      });
  }

  cargarMateriales() {
    this.errorCatalogo.set('');
    this.api
      .productos()
      .pipe(timeout(20000))
      .subscribe({
        next: (r) => this.materiales.set(r.filter((p) => p.tipo === 'PRODUCTO')),
        error: () => this.errorCatalogo.set('No se pudo cargar el catálogo de materiales.'),
      });
  }

  abrir(id: number, accion: 'ver' | 'editar' | 'cancelar' = 'ver') {
    if (this.consultando() || this.busy() || this.pendiente()) return;
    this.nuevo.set(false);
    this.error.set('');
    this.aviso.set('');
    this.consultando.set(true);
    this.http
      .get<Detalle>(this.url + '/' + id)
      .pipe(
        timeout(20000),
        finalize(() => this.consultando.set(false)),
      )
      .subscribe({
        next: (r) => {
          const p = r.pedido;
          if (accion === 'editar' && p.estado === 'PENDIENTE') {
            const { cliente, descripcion, servicios, cantidad, precioUnitario, entrega } = p;
            this.borrador = { cliente, descripcion, servicios, cantidad, precioUnitario, entrega };
            this.editando.set(p);
            this.detalle.set(null);
            this.nuevo.set(true);
            this.mostrarTarjeta();
            return;
          }
          if (accion === 'cancelar' && this.puedeCancelar(p)) {
            this.cancelando.set(r);
            afterNextRender(() => this.dialogo()?.nativeElement.showModal(), {
              injector: this.injector,
            });
            return;
          }
          if (accion !== 'ver') this.error.set('El estado del pedido ya no permite esta acción.');
          this.detalle.set(r);
          this.mostrarTarjeta();
          this.monto = null;
          this.materialId = null;
          this.cantidadMaterial = 1;
          this.cargarMateriales();
        },
        error: () => this.error.set('No se pudo consultar el pedido.'),
      });
  }

  iniciar() {
    this.limpiarBorrador();
    this.detalle.set(null);
    this.nuevo.set(true);
    this.error.set('');
    this.aviso.set('');
    this.mostrarTarjeta(this.cabecera);
  }

  mostrarHistorial() {
    this.editando.set(null);
    this.nuevo.set(false);
    this.detalle.set(null);
    this.error.set('');
    this.aviso.set('');
    this.mostrarTarjeta(this.cabecera);
  }

  private mostrarTarjeta(destino = this.tarjeta) {
    afterNextRender(
      () => {
        const tarjeta = destino()?.nativeElement;
        if (!tarjeta) return;
        const barra = tarjeta.closest('.workspace')?.querySelector('.workspace-header');
        tarjeta.querySelector<HTMLElement>('h1, h2')?.focus({ preventScroll: true });
        tarjeta.style.scrollMarginBlockStart = (barra?.getBoundingClientRect().height ?? 0) + 'px';
        tarjeta.scrollIntoView({ block: 'start', behavior: 'instant' });
      },
      { injector: this.injector },
    );
  }

  total() {
    return (Math.round((this.borrador.precioUnitario ?? 0) * 100) * this.borrador.cantidad) / 100;
  }

  puedeCancelar(p: Pedido) {
    return p.estado === 'PENDIENTE' || p.estado === 'LISTO';
  }

  pagado(p: Pedido) {
    return (Math.round(p.total * 100) - Math.round(p.saldo * 100)) / 100;
  }

  cerrarCancelacion() {
    this.dialogo()?.nativeElement.close();
    this.cancelando.set(null);
  }

  confirmarCancelacion(d: Detalle) {
    this.cerrarCancelacion();
    this.enviar({
      metodo: 'PUT',
      ruta: this.url + '/' + d.pedido.id + '/estado',
      cuerpo: {
        estado: 'CANCELADO',
        ultimoAbono: d.abonos[0]?.id ?? null,
        ultimoConsumo: d.materiales[0]?.id ?? null,
      },
    });
  }

  importe(valor: number | null, max: number): valor is number {
    return (
      valor !== null &&
      Number.isFinite(valor) &&
      valor >= 0.01 &&
      valor <= max &&
      Math.abs(valor * 100 - Math.round(valor * 100)) < 0.001
    );
  }

  private solicitud() {
    // También funciona al abrir PuntoStock por HTTP en la red local.
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
    return [
      hex.slice(0, 8),
      hex.slice(8, 12),
      hex.slice(12, 16),
      hex.slice(16, 20),
      hex.slice(20),
    ].join('-');
  }

  crear() {
    const b = this.borrador;
    const p = this.editando();
    if (
      !b.cliente.trim() ||
      !b.descripcion.trim() ||
      !b.entrega ||
      !Number.isInteger(b.cantidad) ||
      b.cantidad < 1 ||
      b.cantidad > 1000000 ||
      !this.importe(b.precioUnitario, 999999999.99) ||
      this.total() > 999999999.99 ||
      (p ? this.total() < this.pagado(p) : !this.importe(this.anticipo, this.total()))
    )
      return;
    if (p) {
      this.enviar({ metodo: 'PUT', ruta: this.url + '/' + p.id, cuerpo: { ...b } });
      return;
    }
    this.enviar({
      metodo: 'POST',
      ruta: this.url,
      cuerpo: {
        ...b,
        anticipo: {
          monto: this.anticipo,
          metodoPago: this.metodoInicial,
          solicitudId: this.solicitud(),
        },
      },
    });
  }

  abonar(p: Pedido) {
    if (!this.importe(this.monto, p.saldo)) return;
    this.enviar({
      metodo: 'POST',
      ruta: this.url + '/' + p.id + '/abonos',
      cuerpo: {
        monto: this.monto,
        metodoPago: this.medio,
        solicitudId: this.solicitud(),
      },
    });
  }

  consumir(p: Pedido) {
    if (
      !this.materialId ||
      !Number.isInteger(this.cantidadMaterial) ||
      this.cantidadMaterial < 1 ||
      this.cantidadMaterial > 2147483647
    )
      return;
    this.enviar({
      metodo: 'POST',
      ruta: this.url + '/' + p.id + '/materiales',
      cuerpo: {
        productoId: this.materialId,
        cantidad: this.cantidadMaterial,
        solicitudId: this.solicitud(),
      },
    });
  }

  cambiarEstado(p: Pedido, estado: Estado) {
    this.enviar({ metodo: 'PUT', ruta: this.url + '/' + p.id + '/estado', cuerpo: { estado } });
  }

  private enviar(op: Operacion) {
    if (this.busy() || this.consultando() || this.pendiente()) return;
    this.pendiente.set(op);
    try {
      sessionStorage.setItem(this.clave, JSON.stringify(op));
    } catch {}
    this.reintentar();
  }

  private limpiarPendiente() {
    this.pendiente.set(null);
    try {
      sessionStorage.removeItem(this.clave);
    } catch {}
  }

  private limpiarBorrador() {
    this.editando.set(null);
    this.borrador = {
      cliente: '',
      descripcion: '',
      servicios: '',
      cantidad: 1,
      precioUnitario: null,
      entrega: '',
    };
    this.anticipo = null;
    this.metodoInicial = 'EFECTIVO';
  }

  reintentar() {
    const op = this.pendiente();
    if (!op || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.aviso.set('');
    this.http
      .request<Detalle>(op.metodo, op.ruta, { body: op.cuerpo })
      .pipe(
        timeout(20000),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.limpiarPendiente();
          this.detalle.set(r);
          this.nuevo.set(false);
          this.aviso.set(
            r.pedido.estado === 'CANCELADO' ? 'Pedido cancelado.' : 'Cambios guardados.',
          );
          if (
            op.ruta === this.url ||
            op.ruta === this.url + '/' + r.pedido.id ||
            r.pedido.estado === 'CANCELADO'
          ) {
            this.mostrarTarjeta();
            this.limpiarBorrador();
          }
          this.monto = null;
          this.cantidadMaterial = 1;
          this.cargar(op.ruta === this.url ? 0 : (this.lista()?.pagina ?? 0));
          this.cargarMateriales();
        },
        error: (e) => {
          if ([400, 403, 404, 409].includes(e.status)) {
            this.limpiarPendiente();
            this.error.set(
              e.error?.message ?? e.error?.detail ?? 'Revisa los datos y vuelve a intentar.',
            );
            return;
          }
          this.error.set(
            'No se pudo confirmar la operación. Usa Reintentar para consultar o completar el mismo registro.',
          );
        },
      });
  }

  async descargar(d: Detalle, a: Abono) {
    try {
      const p = d.pedido;
      const totalAlAbonar =
        (Math.round(a.saldo * 100) +
          d.abonos
            .filter((abono) => abono.id <= a.id)
            .reduce((centavos, abono) => centavos + Math.round(abono.monto * 100), 0)) /
        100;
      const { jsPDF } = await import('jspdf');
      const pdf = new jsPDF({ format: 'a5' });
      const dinero = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
      pdf.setFontSize(17);
      pdf.text('PuntoStock · Comprobante de abono', 12, 17);
      pdf.setFontSize(11);
      const lineas = [
        'Abono #' + a.id + ' · Pedido #' + p.id,
        'Fecha: ' +
          new Intl.DateTimeFormat('es-SV', {
            dateStyle: 'short',
            timeStyle: 'short',
            timeZone: 'America/El_Salvador',
          }).format(new Date(a.fecha)),
        'Cliente: ' + p.cliente,
        'Pedido: ' + p.descripcion,
        'Total del pedido al abonar: ' + dinero.format(totalAlAbonar),
        'Importe recibido: ' + dinero.format(a.monto),
        'Medio de pago: ' + this.medios[a.metodoPago],
        'Saldo después de este abono: ' + dinero.format(a.saldo),
      ];
      let y = 28;
      for (const linea of lineas) {
        const partes = pdf.splitTextToSize(linea, 124) as string[];
        for (const parte of partes) {
          if (y > 193) {
            pdf.addPage();
            y = 16;
          }
          pdf.text(parte, 12, y);
          y += 5;
        }
        y += 3;
      }
      pdf.save('Abono-' + a.id + '-Pedido-' + p.id + '.pdf');
    } catch {
      this.error.set('No se pudo descargar el comprobante.');
    }
  }
}
