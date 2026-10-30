import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface CategoriaProducto {
  id: number;
  nombre: string;
  activo: boolean;
  version: number;
}
@Injectable({ providedIn: 'root' })
export class CategoriasProductosService {
  private http = inject(HttpClient);
  listar() {
    return this.http.get<CategoriaProducto[]>('/api/productos/categorias');
  }
  guardar(nombre: string, activo: boolean, categoria?: CategoriaProducto) {
    const body = { nombre: nombre.trim(), activo, version: categoria?.version };
    return categoria
      ? this.http.put<CategoriaProducto>('/api/productos/categorias/' + categoria.id, body)
      : this.http.post<CategoriaProducto>('/api/productos/categorias', body);
  }
}
