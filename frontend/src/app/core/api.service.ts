import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, timeout } from 'rxjs';
export interface Pagina<T> {
  contenido: T[];
  cantidad: number;
  pagina: number;
  paginas: number;
}
export interface Perfil {
  id: number;
  username: string;
  nombre: string;
  correo: string | null;
  telefono: string | null;
  roles: string[];
  activo: boolean;
  fechaCreacion: string | null;
  fotoPerfil: string | null;
  fotoPortada: string | null;
}
export interface PerfilInput {
  nombre: string;
  correo: string;
  telefono: string;
}
export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  descripcion: string;
  precioCompra?: number;
  precioVenta: number;
  stock: number;
  fotoUrl: string;
  tipo: 'PRODUCTO' | 'SERVICIO';
  categoria: string;
  marca: string;
  unidad: string;
  ubicacion: string;
  stockMinimo: number;
}
export type ProductoInput = Omit<Producto, 'id' | 'stock'>;
export interface Movimiento {
  id: number;
  productoId: number;
  tipo: string;
  cantidad: number;
  fecha: string;
  motivo: string;
  stockAnterior: number;
  stockPosterior: number;
}
export type MedioPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';
export interface Cobro {
  lineas: { productoId: number; cantidad: number; observaciones: string }[];
  metodoPago: MedioPago;
  recibido: number | null;
  totalEsperado: number;
  cliente: string;
  referenciaPago: string;
  solicitudId: string;
}
export interface Ticket {
  id: number;
  total: number;
  fecha: string;
  vendedor: string;
  metodoPago: string;
  recibido: number | null;
  cambio: number | null;
  cliente: string | null;
  referenciaPago: string | null;
  lineas: {
    productoId: number;
    sku: string;
    nombre: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
    observaciones: string | null;
  }[];
}
export interface HistorialVentas {
  ventas: {
    id: number;
    fecha: string;
    vendedor: string;
    total: number;
    metodoPago: string;
    cliente: string | null;
  }[];
  cantidad: number;
  pagina: number;
  paginas: number;
  total: number;
  efectivo: number;
  tarjeta: number;
  transferencia: number;
  sinRegistro: number;
}
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  productosPagina(buscar: string, pagina = 0) {
    return this.http.get<Pagina<Producto>>('/api/productos/pagina', {
      params: { buscar, pagina, tamano: 6 },
    });
  }
  movimientosPagina(pagina = 0, desde = '', hasta = '', productoId = '') {
    return this.http.get<Pagina<Movimiento>>('/api/inventario/movimientos/pagina', {
      params: {
        pagina,
        tamano: 6,
        ...(desde ? { desde } : {}),
        ...(hasta ? { hasta } : {}),
        ...(productoId ? { productoId } : {}),
      },
    });
  }
  productos() {
    return this.http.get<Producto[]>('/api/productos');
  }
  guardar(p: ProductoInput, id?: number) {
    return id ? this.http.put('/api/productos/' + id, p) : this.http.post('/api/productos', p);
  }
  eliminar(id: number) {
    return this.http.delete('/api/productos/' + id);
  }
  vender(cobro: Cobro) {
    return this.http.post<Ticket>('/api/ventas', cobro).pipe(timeout(20000));
  }
  historialVentas(fecha: string, pagina = 0) {
    return this.http.get<HistorialVentas>('/api/ventas', { params: { fecha, pagina } });
  }
  ticket(id: number) {
    return this.http.get<Ticket>('/api/ventas/' + id);
  }
  recuperarVenta(solicitudId: string) {
    return this.http.get<Ticket>('/api/ventas/solicitud/' + solicitudId).pipe(timeout(20000));
  }
  mover(body: { productoId: number; tipo: string; cantidad: number; motivo: string }) {
    return this.http.post('/api/inventario/movimientos', body);
  }
  movimientos() {
    return this.http.get<Movimiento[]>('/api/inventario/movimientos');
  }
  ganancia(fecha: string) {
    return this.http.get<{ ganancia: number }>('/api/reportes/ganancia', { params: { fecha } });
  }
}
export { AuthService } from './auth.service';
