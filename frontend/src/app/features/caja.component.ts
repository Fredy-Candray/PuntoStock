import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { IconComponent } from '../core/icon.component';
import { AuthService, Pagina } from '../core/api.service';
import { CajaService, ResumenCaja, RegistroCaja, OperacionCaja } from '../core/caja.service';
import { ImporteCentavosDirective } from '../core/importe-centavos.directive';

@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, IconComponent, ImporteCentavosDirective],
  templateUrl: './caja.component.html',
  styleUrl: './caja.component.css',
})
export class CajaComponent {
  readonly api = inject(CajaService);
  readonly auth = inject(AuthService);
  readonly aperturaForm = viewChild<NgForm>('ap');
  readonly movimientoForm = viewChild<NgForm>('mv');
  readonly cierreForm = viewChild<NgForm>('ci');
  loading = signal(false);
  busy = signal(false);
  error = signal('');
  mensaje = signal('');
  actualId = signal<number | null>(null);
  seleccion = signal<ResumenCaja | null>(null);
  historial = signal<Pagina<ResumenCaja> | null>(null);
  registros = signal<Pagina<RegistroCaja> | null>(null);
  pendiente = signal<OperacionCaja | null>(null);
  inicial: number | null = null;
  notaApertura = '';
  tipo = 'ENTRADA';
  monto: number | null = null;
  motivo = '';
  contado: number | null = null;
  notaCierre = '';
  confirmar = false;
  pagina = 0;
  constructor() {
    try {
      const p = JSON.parse(sessionStorage.getItem(this.clave()) ?? 'null');
      if (
        p &&
        ['abrir', 'mover', 'cerrar'].includes(p.tipo) &&
        typeof p.cuerpo?.solicitudId === 'string'
      )
        this.pendiente.set(p);
    } catch {}
    this.cargar();
  }
  private clave() {
    return 'puntostock.caja.pendiente:' + this.auth.username();
  }
  bloqueado() {
    return this.loading() || this.busy() || !!this.pendiente();
  }
  dinero(n: number | null, positivo = false) {
    return (
      n !== null &&
      Number.isFinite(n) &&
      n >= (positivo ? 0.01 : 0) &&
      n <= 999999999.99 &&
      Math.abs(n * 100 - Math.round(n * 100)) < 0.00001
    );
  }
  diferencia() {
    const c = this.seleccion();
    return c && this.contado !== null
      ? (Math.round(this.contado * 100) - Math.round(c.esperado * 100)) / 100
      : 0;
  }
  cargar(id?: number, pagina = this.pagina) {
    if (this.loading() || this.busy()) return;
    this.loading.set(true);
    this.error.set('');
    this.pagina = pagina;
    forkJoin({ estado: this.api.estado(), historial: this.api.historial(pagina) }).subscribe({
      next: (r) => {
        this.actualId.set(r.estado.id);
        this.historial.set(r.historial);
        const elegido = id ?? r.estado.id ?? r.historial.contenido[0]?.id;
        if (!elegido) {
          this.seleccion.set(null);
          this.registros.set(null);
          this.loading.set(false);
          return;
        }
        forkJoin({ caja: this.api.detalle(elegido), mov: this.api.movimientos(elegido) }).subscribe(
          {
            next: (d) => {
              this.seleccion.set(d.caja);
              this.registros.set(d.mov);
              this.confirmar = false;
              this.loading.set(false);
            },
            error: () => {
              this.seleccion.set(null);
              this.registros.set(null);
              this.loading.set(false);
              this.error.set('No se pudo consultar el detalle de caja. Vuelve a actualizar.');
            },
          },
        );
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo consultar la caja. Vuelve a actualizar.');
      },
    });
  }
  abrir() {
    if (this.bloqueado() || !this.dinero(this.inicial)) return;
    this.enviar({
      tipo: 'abrir',
      cuerpo: {
        inicial: this.inicial!,
        nota: this.notaApertura.trim(),
        solicitudId: crypto.randomUUID(),
      },
    });
  }
  mover() {
    const c = this.seleccion();
    if (this.bloqueado() || !c || c.cierre || !this.dinero(this.monto, true) || !this.motivo.trim())
      return;
    this.enviar({
      tipo: 'mover',
      id: c.id,
      cuerpo: {
        tipo: this.tipo,
        monto: this.monto!,
        motivo: this.motivo.trim(),
        solicitudId: crypto.randomUUID(),
      },
    });
  }
  cerrar() {
    const c = this.seleccion();
    if (
      this.bloqueado() ||
      !c ||
      c.cierre ||
      !this.confirmar ||
      !this.dinero(this.contado) ||
      (this.diferencia() !== 0 && !this.notaCierre.trim())
    )
      return;
    this.enviar({
      tipo: 'cerrar',
      id: c.id,
      cuerpo: {
        contado: this.contado!,
        nota: this.notaCierre.trim(),
        solicitudId: crypto.randomUUID(),
        esperado: c.esperado,
        ventas: c.ventas,
      },
    });
  }
  private limpiar() {
    this.pendiente.set(null);
    try {
      sessionStorage.removeItem(this.clave());
    } catch {}
  }
  enviar(op: OperacionCaja) {
    if (this.busy()) return;
    this.pendiente.set(op);
    try {
      sessionStorage.setItem(this.clave(), JSON.stringify(op));
    } catch {}
    this.busy.set(true);
    this.error.set('');
    this.mensaje.set('');
    this.api.ejecutar(op).subscribe({
      next: (c) => {
        this.busy.set(false);
        this.limpiar();
        this.monto = null;
        this.motivo = '';
        this.contado = null;
        this.notaCierre = '';
        this.confirmar = false;
        this.inicial = null;
        this.notaApertura = '';
        this.aperturaForm()?.resetForm({ inicial: null, notaApertura: '' });
        this.movimientoForm()?.resetForm({ tipo: this.tipo, monto: null, motivo: '' });
        this.cierreForm()?.resetForm({ contado: null, notaCierre: '', confirmar: false });
        this.mensaje.set(
          op.tipo === 'abrir'
            ? 'Caja abierta. Ya puedes registrar ventas.'
            : op.tipo === 'mover'
              ? 'Movimiento registrado.'
              : 'Cierre registrado. Revisa el resultado y la diferencia.',
        );
        this.cargar(c.id, 0);
      },
      error: (e) => {
        this.busy.set(false);
        if (e.status >= 400 && e.status < 500 && e.status !== 401 && e.status !== 408) {
          this.limpiar();
          this.confirmar = false;
          this.cargar(op.tipo === 'abrir' ? undefined : op.id, 0);
          this.error.set(
            e.error?.message || 'Operación rechazada. Actualiza el resumen antes de continuar.',
          );
        } else
          this.error.set(
            'No se pudo confirmar la operación. Reintenta la misma solicitud para comprobar su resultado sin duplicarla.',
          );
      },
    });
  }
  movimientos(pagina: number) {
    const c = this.seleccion();
    if (!c || this.loading() || this.busy()) return;
    this.loading.set(true);
    this.api.movimientos(c.id, pagina).subscribe({
      next: (r) => {
        this.registros.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron consultar los movimientos.');
      },
    });
  }
}
