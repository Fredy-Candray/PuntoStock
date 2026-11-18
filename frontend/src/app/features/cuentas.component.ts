import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { ApiService, AuthService, Producto } from '../core/api.service';

type Ambito = 'PERSONAL' | 'NEGOCIO';
type Tipo = 'APORTE' | 'RETIRO' | 'TRASPASO' | 'INGRESO' | 'GASTO' | 'COMPRA' | 'PAGO_COMPRA';
interface Cuenta {
  id: number;
  nombre: string;
  ambito: Ambito;
  saldo: number;
}
interface Vista {
  cuentas: Cuenta[];
  movimientos: {
    id: number;
    fecha: string;
    concepto: string;
    tipo: Tipo;
    monto: number;
    origen: string | null;
    destino: string | null;
    cajaId: number | null;
    cantidad: number | null;
    pagadoInicial: number | null;
  }[];
  pagina: number;
  totalMovimientos: number;
  porPagar: { id: number; proveedor: string; pendiente: number }[];
  porCobrar: { id: number; cliente: string; pendiente: number }[];
  caja: { id: number | null; pendienteDeCierre: boolean };
  resultado: {
    ventas: number;
    gananciaBruta: number;
    otrosIngresos: number;
    gastos: number;
    pedidosEntregados: number;
    costoPedidos: number;
    pedidosSinCosto: number;
    resultadoEstimado: number;
    compras: number;
    inventario: number;
    entregadosSinFecha: number;
    totalPorPagar: number;
    totalPorCobrar: number;
  };
}
interface Solicitud {
  ruta: '/api/cuentas' | '/api/cuentas/movimientos';
  cuerpo: object;
}
type Seleccion = number | 'CAJA' | null;

@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './cuentas.component.html',
  styleUrl: './cuentas.component.css',
})
export class CuentasComponent {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly clave = 'puntostock.cuentas.pendiente:' + inject(AuthService).username();
  readonly vista = signal<Vista | null>(null);
  readonly productos = signal<Producto[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly errorCatalogo = signal('');
  readonly aviso = signal('');
  readonly pendiente = signal<Solicitud | null>(null);
  readonly etiquetas: Record<Tipo, string> = {
    APORTE: 'Aporte al negocio',
    RETIRO: 'Retiro del dueño',
    TRASPASO: 'Traspaso',
    INGRESO: 'Entrada de dinero',
    GASTO: 'Gasto',
    COMPRA: 'Compra de mercadería',
    PAGO_COMPRA: 'Pago a proveedor',
  };
  readonly tipos = Object.keys(this.etiquetas) as Tipo[];
  private readonly hoy = new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'America/El_Salvador',
  }).format(new Date());
  desde = this.hoy.slice(0, 7) + '-01';
  hasta = this.hoy;
  private filtro = { desde: this.desde, hasta: this.hasta };
  nombre = '';
  ambito: Ambito = 'NEGOCIO';
  inicial: number | null = 0;
  tipo: Tipo = 'APORTE';
  concepto = '';
  monto: number | null = null;
  origen: Seleccion = null;
  destino: Seleccion = null;
  productoId: number | null = null;
  cantidad: number | null = 1;
  pagadoInicial: number | null = 0;
  compraId: number | null = null;

  constructor() {
    try {
      const op = JSON.parse(sessionStorage.getItem(this.clave) ?? 'null') as Solicitud | null;
      if (op?.cuerpo && ['/api/cuentas', '/api/cuentas/movimientos'].includes(op.ruta))
        this.pendiente.set(op);
    } catch {}
    this.cargar();
    this.cargarProductos();
  }

  cargarProductos() {
    this.errorCatalogo.set('');
    this.api
      .productos()
      .pipe(timeout(20000))
      .subscribe({
        next: (p) => this.productos.set(p.filter((x) => x.tipo === 'PRODUCTO')),
        error: () => this.errorCatalogo.set('No se pudieron cargar los productos.'),
      });
  }

  filtrar() {
    if (this.cargando() || this.guardando()) return;
    this.error.set('');
    this.filtro = { desde: this.desde, hasta: this.hasta };
    this.cargar();
  }

  cargar(pagina = 0) {
    if (this.cargando()) return;
    this.cargando.set(true);
    this.http
      .get<Vista>('/api/cuentas', { params: { ...this.filtro, pagina } })
      .pipe(
        timeout(20000),
        finalize(() => this.cargando.set(false)),
      )
      .subscribe({
        next: (r) => this.vista.set(r),
        error: (e) => this.error.set(e.error?.message || 'No se pudo consultar Cuentas.'),
      });
  }

  saldo(ambito: Ambito) {
    return (
      (this.vista()?.cuentas ?? [])
        .filter((c) => c.ambito === ambito)
        .reduce((total, c) => total + Math.round(c.saldo * 100), 0) / 100
    );
  }

  origenes() {
    const cuentas = this.vista()?.cuentas ?? [];
    if (this.tipo === 'APORTE') return cuentas.filter((c) => c.ambito === 'PERSONAL');
    if (['RETIRO', 'COMPRA', 'PAGO_COMPRA'].includes(this.tipo))
      return cuentas.filter((c) => c.ambito === 'NEGOCIO');
    return cuentas;
  }

  destinos() {
    const cuentas = this.vista()?.cuentas ?? [];
    if (this.tipo === 'APORTE' || this.origen === 'CAJA')
      return cuentas.filter((c) => c.ambito === 'NEGOCIO');
    if (this.tipo === 'RETIRO') return cuentas.filter((c) => c.ambito === 'PERSONAL');
    if (this.tipo === 'TRASPASO') {
      const origen = cuentas.find((c) => c.id === this.origen);
      return cuentas.filter((c) => c.id !== origen?.id && c.ambito === origen?.ambito);
    }
    return cuentas;
  }

  requiereOrigen() {
    return this.tipo !== 'INGRESO' && (this.tipo !== 'COMPRA' || (this.pagadoInicial ?? 0) > 0);
  }

  origenEsNegocio() {
    return this.vista()?.cuentas.find((c) => c.id === this.origen)?.ambito === 'NEGOCIO';
  }

  requiereDestino() {
    return ['APORTE', 'RETIRO', 'TRASPASO', 'INGRESO'].includes(this.tipo);
  }

  cambiarTipo() {
    this.origen = null;
    this.destino = null;
    this.compraId = null;
    this.productoId = null;
    this.cantidad = 1;
    this.pagadoInicial = 0;
  }

  pagar(id: number) {
    this.tipo = 'PAGO_COMPRA';
    this.cambiarTipo();
    this.compraId = id;
    this.concepto = 'Pago de compra #' + id;
    this.monto = this.vista()?.porPagar.find((d) => d.id === id)?.pendiente ?? null;
  }

  private solicitud() {
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
    return hex.replace(/^(........)(....)(....)(....)(............)$/, '$1-$2-$3-$4-$5');
  }

  alta() {
    this.preparar({
      ruta: '/api/cuentas',
      cuerpo: {
        nombre: this.nombre.trim(),
        ambito: this.ambito,
        inicial: this.inicial,
        solicitud: this.solicitud(),
      },
    });
  }

  mover() {
    const origen = this.requiereOrigen() ? this.origen : null;
    const destino = this.requiereDestino() ? this.destino : null;
    this.preparar({
      ruta: '/api/cuentas/movimientos',
      cuerpo: {
        tipo: this.tipo,
        concepto: this.concepto.trim(),
        monto: this.monto,
        solicitud: this.solicitud(),
        origen: typeof origen === 'number' ? origen : null,
        destino: typeof destino === 'number' ? destino : null,
        cajaId: origen === 'CAJA' || destino === 'CAJA' ? this.vista()?.caja.id : null,
        productoId: this.tipo === 'COMPRA' ? this.productoId : null,
        cantidad: this.tipo === 'COMPRA' ? this.cantidad : null,
        pagadoInicial: this.tipo === 'COMPRA' ? this.pagadoInicial : null,
        compraId: this.tipo === 'PAGO_COMPRA' ? this.compraId : null,
      },
    });
  }

  private preparar(op: Solicitud) {
    if (this.pendiente() || this.guardando() || this.cargando()) return;
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

  reintentar() {
    const op = this.pendiente();
    if (!op || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.aviso.set('');
    this.http
      .post(op.ruta, op.cuerpo)
      .pipe(
        timeout(20000),
        finalize(() => this.guardando.set(false)),
      )
      .subscribe({
        next: () => {
          this.limpiarPendiente();
          if (op.ruta === '/api/cuentas') {
            this.nombre = '';
            this.inicial = 0;
          } else {
            this.concepto = '';
            this.monto = null;
            this.cambiarTipo();
            this.cargarProductos();
          }
          this.aviso.set('Guardado.');
          this.cargar();
        },
        error: (e) => {
          if ([400, 403, 404, 409].includes(e.status)) {
            this.limpiarPendiente();
            this.error.set(
              e.error?.message || e.error?.detail || 'Revisa los datos de la operación.',
            );
          } else {
            this.error.set('No se pudo confirmar el registro. Reintenta esta misma operación.');
          }
        },
      });
  }
}
