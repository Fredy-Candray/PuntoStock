import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { IconComponent } from '../core/icon.component';
import { ApiService, Producto, ProductoInput } from '../core/api.service';
@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, IconComponent],
  templateUrl: './productos.component.html',
})
export class ProductosComponent {
  api = inject(ApiService);
  productos = signal<Producto[]>([]);
  mensaje = signal('');
  busy = signal(false);
  id?: number;
  modelo: ProductoInput = this.vacio();
  vacio(): ProductoInput {
    return {
      sku: '',
      nombre: '',
      descripcion: '',
      precioCompra: 0,
      precioVenta: 0,
      fotoUrl: '',
      tipo: 'PRODUCTO',
      categoria: 'General',
      marca: '',
      unidad: 'UNIDAD',
      ubicacion: '',
      stockMinimo: 5,
    };
  }
  categorias = [
    'Impresiones',
    'Fotocopias',
    'Fotografías',
    'Papelería',
    'Útiles escolares',
    'Juguetes',
    'Carteras',
    'Audífonos y accesorios',
    'Otros',
  ];
  unidades = ['UNIDAD', 'PAQUETE', 'CAJA', 'RESMA', 'DOCENA', 'PAGINA', 'FOTOGRAFIA', 'TRABAJO'];
  cambiarTipo() {
    this.modelo.unidad = this.modelo.tipo === 'SERVICIO' ? 'PAGINA' : 'UNIDAD';
    this.modelo.stockMinimo = this.modelo.tipo === 'SERVICIO' ? 0 : 5;
  }
  nuevoServicio() {
    this.nuevo();
    this.modelo.tipo = 'SERVICIO';
    this.modelo.categoria = 'Impresiones';
    this.cambiarTipo();
  }
  pagina = 0;
  paginas = signal(0);
  total = signal(0);
  private consulta = 0;
  private demora?: ReturnType<typeof setTimeout>;
  constructor() {
    this.cargar();
  }
  buscarEnServidor() {
    clearTimeout(this.demora);
    this.demora = setTimeout(() => {
      this.pagina = 0;
      this.cargar();
    }, 300);
  }
  cargar() {
    const id = ++this.consulta;
    this.api.productosPagina(this.buscar, this.pagina).subscribe({
      next: (p) => {
        if (id !== this.consulta) return;
        this.productos.set(p.contenido);
        this.paginas.set(p.paginas);
        this.total.set(p.cantidad);
      },
      error: () => this.mensaje.set('Error al consultar productos.'),
    });
  }
  cambiarPagina(delta: number) {
    this.pagina += delta;
    this.cargar();
  }
  editor = signal(false);
  buscar = '';
  filtrados() {
    return this.productos();
  }
  nuevo() {
    this.id = undefined;
    this.modelo = this.vacio();
    this.editor.set(true);
  }
  cerrar() {
    this.id = undefined;
    this.modelo = this.vacio();
    this.editor.set(false);
  }
  editar(p: Producto) {
    this.id = p.id;
    const { id, stock, ...input } = p;
    this.modelo = input;
    this.editor.set(true);
    document
      .getElementById('product-editor')
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  guardar() {
    if (this.busy()) return;
    this.busy.set(true);
    this.api.guardar(this.modelo, this.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.mensaje.set('Producto guardado.');
        this.cerrar();
        this.cargar();
      },
      error: () => {
        this.busy.set(false);
        this.mensaje.set('No se pudo guardar; verifique campos y SKU único.');
      },
    });
  }
  eliminar(p: Producto) {
    if (!confirm('¿Desactivar ' + p.nombre + '?')) return;
    this.busy.set(true);
    this.api.eliminar(p.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.cargar();
      },
      error: () => {
        this.busy.set(false);
        this.mensaje.set('No se pudo desactivar.');
      },
    });
  }
}
