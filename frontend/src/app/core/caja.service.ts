import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import type { Pagina } from './api.service';
export interface EstadoCaja {
  abierta: boolean;
  id: number | null;
}
export interface ResumenCaja {
  id: number;
  apertura: string;
  abiertoPor: string;
  inicial: number;
  notaApertura: string;
  cierre: string | null;
  cerradoPor: string | null;
  contado: number | null;
  diferencia: number | null;
  notaCierre: string | null;
  efectivo: number;
  tarjeta: number;
  transferencia: number;
  ventas: number;
  entradas: number;
  salidas: number;
  esperado: number;
}
export interface RegistroCaja {
  id: string;
  fecha: string;
  usuario: string;
  tipo: string;
  motivo: string;
  monto: number;
  metodoPago: string;
}
export type OperacionCaja =
  | { tipo: 'abrir'; cuerpo: { inicial: number; nota: string; solicitudId: string } }
  | {
      tipo: 'mover';
      id: number;
      cuerpo: { tipo: string; monto: number; motivo: string; solicitudId: string };
    }
  | {
      tipo: 'cerrar';
      id: number;
      cuerpo: {
        contado: number;
        nota: string;
        solicitudId: string;
        esperado: number;
        ventas: number;
      };
    };
@Injectable({ providedIn: 'root' })
export class CajaService {
  private http = inject(HttpClient);
  estado() {
    return this.http.get<EstadoCaja>('/api/caja/estado');
  }
  historial(pagina = 0) {
    return this.http.get<Pagina<ResumenCaja>>('/api/caja', { params: { pagina } });
  }
  detalle(id: number) {
    return this.http.get<ResumenCaja>('/api/caja/' + id);
  }
  movimientos(id: number, pagina = 0) {
    return this.http.get<Pagina<RegistroCaja>>('/api/caja/' + id + '/movimientos', {
      params: { pagina },
    });
  }
  ejecutar(op: OperacionCaja) {
    const ruta =
      op.tipo === 'abrir'
        ? '/api/caja/abrir'
        : '/api/caja/' + op.id + (op.tipo === 'mover' ? '/movimientos' : '/cerrar');
    return this.http.post<ResumenCaja>(ruta, op.cuerpo).pipe(timeout(20000));
  }
}
