import { Component, HostListener, inject, signal } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import {
  CategoriasProductosService,
  CategoriaProducto,
} from '../core/categorias-productos.service';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { IconComponent } from '../core/icon.component';
import { ProductoImagenComponent } from '../core/producto-imagen.component';
import { ApiService, Producto, ProductoInput } from '../core/api.service';
@Component({
  standalone: true,
  imports: [FormsModule, CurrencyPipe, IconComponent, A11yModule, ProductoImagenComponent],
  templateUrl: './productos.component.html',
  styleUrl: './productos.component.css',
})
export class ProductosComponent {
  api = inject(ApiService);
  productos = signal<Producto[]>([]);
  detalle = signal<Producto | null>(null);
  cajasCompletas(p: Producto) {
    return p.caja ? Math.floor(p.stock / p.caja.unidades) : 0;
  }
  mensaje = signal('');
  busy = signal(false);
  id?: number;
  imagenNueva = signal<string | null>(null);
  imagenCargando = signal(false);
  imagenError = signal('');
  quitarImagen = false;
  private lecturaImagen = 0;
  private reiniciarImagen() {
    this.lecturaImagen++;
    this.imagenNueva.set(null);
    this.imagenCargando.set(false);
    this.imagenError.set('');
    this.quitarImagen = false;
  }
  quitarFoto() {
    this.reiniciarImagen();
    this.modelo.fotoUrl = '';
    this.quitarImagen = true;
  }
  usarUrlImagen() {
    this.lecturaImagen++;
    this.imagenNueva.set(null);
    this.imagenCargando.set(false);
    this.imagenError.set('');
    this.quitarImagen = false;
  }
  async seleccionarImagen(event: Event) {
    const input = event.target as HTMLInputElement,
      file = input.files?.[0];
    input.value = '';
    if (!file || this.busy()) return;
    const lectura = ++this.lecturaImagen;
    this.imagenError.set('');
    this.imagenCargando.set(true);
    try {
      if (
        !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) ||
        file.size > 10 * 1024 * 1024
      )
        throw new Error('Selecciona una imagen JPG, PNG o WebP de hasta 10 MB.');
      const data = await new Promise<string>((resolve, reject) => {
        const r = new FileReader();
        r.onload = () => resolve(String(r.result));
        r.onerror = () => reject(new Error('No se pudo leer la imagen.'));
        r.readAsDataURL(file);
      });
      const imagen = await new Promise<HTMLImageElement>((resolve, reject) => {
        const img = new Image();
        img.onload = () => resolve(img);
        img.onerror = () => reject(new Error('El archivo no es una imagen válida.'));
        img.src = data;
      });
      if (
        imagen.naturalWidth < 1 ||
        imagen.naturalHeight < 1 ||
        imagen.naturalWidth * imagen.naturalHeight > 40000000
      )
        throw new Error('La imagen es demasiado grande. Elige una de hasta 40 megapíxeles.');
      const escala = Math.min(1, 1024 / Math.max(imagen.naturalWidth, imagen.naturalHeight));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(imagen.naturalWidth * escala));
      canvas.height = Math.max(1, Math.round(imagen.naturalHeight * escala));
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('No se pudo preparar la imagen.');
      if (file.type !== 'image/png') {
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
      }
      ctx.drawImage(imagen, 0, 0, canvas.width, canvas.height);
      let normalizada = canvas.toDataURL(
        file.type === 'image/png' ? 'image/png' : 'image/jpeg',
        0.85,
      );
      if (normalizada.length > 1350000) {
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(imagen, 0, 0, canvas.width, canvas.height);
        normalizada = canvas.toDataURL('image/jpeg', 0.8);
      }
      if (normalizada.length > 1350000)
        throw new Error('No se pudo reducir la imagen. Elige una más pequeña.');
      if (lectura === this.lecturaImagen) {
        this.imagenNueva.set(normalizada);
        this.modelo.fotoUrl = '';
        this.quitarImagen = false;
      }
    } catch (e) {
      if (lectura === this.lecturaImagen)
        this.imagenError.set(e instanceof Error ? e.message : 'No se pudo cargar la imagen.');
    } finally {
      if (lectura === this.lecturaImagen) this.imagenCargando.set(false);
    }
  }
  codigoAutomatico = true;
  compraPorCaja = false;
  vendeCaja = false;
  cajaConfig = {
    unidades: 12,
    precioCompra: 0,
    precioVenta: null as number | null,
    cajasIniciales: 0,
  };
  cambiarCompraCaja() {
    if (this.compraPorCaja) {
      this.modelo.unidad = 'UNIDAD';
      this.recalcularCaja();
    }
  }
  cambiarUnidad() {
    if (this.modelo.tipo === 'PRODUCTO' && this.modelo.unidad === 'CAJA') {
      this.compraPorCaja = true;
      this.cambiarCompraCaja();
    }
  }
  recalcularCaja() {
    const n = this.cajaConfig.unidades,
      precio = this.cajaConfig.precioCompra;
    if (
      this.compraPorCaja &&
      Number.isInteger(n) &&
      n >= 2 &&
      Number.isFinite(precio) &&
      precio >= 0
    )
      this.modelo.precioCompra = Math.round(Math.round(precio * 100) / n) / 100;
  }
  unidadesInicialesCaja() {
    return (
      (this.modelo.existenciasIniciales ?? 0) +
      this.cajaConfig.cajasIniciales * this.cajaConfig.unidades
    );
  }
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
      existenciasIniciales: 0,
    };
  }
  categoriasApi = inject(CategoriasProductosService);
  categorias = signal<CategoriaProducto[]>([]);
  categoriasCargando = signal(false);
  categoriasError = signal('');
  categoriasDialogo = signal(false);
  categoriaBusy = signal(false);
  categoriaMensaje = signal('');
  categoriaEdicion?: CategoriaProducto;
  categoriaNombre = '';
  categoriaPagina = 0;
  categoriasPagina() {
    return this.categorias().slice(this.categoriaPagina * 6, this.categoriaPagina * 6 + 6);
  }
  categoriaAnterior?: string;
  categoriasDisponibles() {
    return this.categorias().filter(
      (c) => c.activo || (this.id !== undefined && c.nombre === this.categoriaAnterior),
    );
  }
  cargarCategorias() {
    this.categoriasCargando.set(true);
    this.categoriasError.set('');
    this.categoriasApi.listar().subscribe({
      next: (c) => {
        this.categorias.set(c);
        this.categoriasCargando.set(false);
      },
      error: () => {
        this.categoriasCargando.set(false);
        this.categoriasError.set('No se pudieron cargar las categorías. Intenta actualizar.');
      },
    });
  }
  abrirCategorias() {
    this.categoriaMensaje.set('');
    this.categoriasDialogo.set(true);
    this.categoriaEdicion = undefined;
    this.categoriaNombre = '';
    this.categoriaPagina = 0;
    this.cargarCategorias();
  }
  cerrarCategorias() {
    if (!this.categoriaBusy()) this.categoriasDialogo.set(false);
  }
  @HostListener('document:keydown.escape') cerrarCategoriasConEscape() {
    this.cerrarCategorias();
    this.detalle.set(null);
  }
  editarCategoria(c: CategoriaProducto) {
    this.categoriaEdicion = c;
    this.categoriaNombre = c.nombre;
    this.categoriasError.set('');
  }
  cancelarCategoria() {
    this.categoriaEdicion = undefined;
    this.categoriaNombre = '';
  }
  guardarCategoria() {
    if (this.categoriaBusy() || !this.categoriaNombre.trim()) return;
    this.persistirCategoria(
      this.categoriaNombre,
      this.categoriaEdicion?.activo ?? true,
      this.categoriaEdicion,
    );
  }
  cambiarEstadoCategoria(c: CategoriaProducto) {
    if (!this.categoriaBusy()) this.persistirCategoria(c.nombre, !c.activo, c, false);
  }
  private persistirCategoria(
    nombre: string,
    activo: boolean,
    anterior?: CategoriaProducto,
    seleccionar = true,
  ) {
    this.categoriaBusy.set(true);
    this.categoriasError.set('');
    this.categoriasApi.guardar(nombre, activo, anterior).subscribe({
      next: (c) => {
        this.categorias.update((lista) =>
          [...lista.filter((x) => x.id !== c.id), c].sort((a, b) =>
            a.nombre.localeCompare(b.nombre, 'es'),
          ),
        );
        this.categoriaPagina = Math.floor(this.categorias().findIndex((x) => x.id === c.id) / 6);
        this.categoriaMensaje.set(
          anterior
            ? 'Categoría actualizada.'
            : 'Categoría creada. Ya está disponible en el selector.',
        );
        if (anterior && this.modelo.categoria === anterior.nombre) this.modelo.categoria = c.nombre;
        if (anterior && this.categoriaAnterior === anterior.nombre)
          this.categoriaAnterior = c.nombre;
        if (!anterior && seleccionar && this.editor()) this.modelo.categoria = c.nombre;
        this.cancelarCategoria();
        this.categoriaBusy.set(false);
        this.cargar();
      },
      error: (e) => {
        this.categoriaBusy.set(false);
        this.categoriasError.set(
          e.status === 409
            ? 'Ya existe ese nombre o la categoría cambió. Actualiza la lista y revisa los datos.'
            : 'No se pudo guardar la categoría. Inténtalo de nuevo.',
        );
      },
    });
  }
  unidades = ['UNIDAD', 'PAQUETE', 'CAJA', 'RESMA', 'DOCENA', 'PAGINA', 'FOTOGRAFIA', 'TRABAJO'];
  cambiarTipo() {
    if (this.modelo.tipo === 'SERVICIO') {
      this.compraPorCaja = false;
      this.vendeCaja = false;
    }
    if (this.modelo.tipo === 'SERVICIO') this.modelo.existenciasIniciales = 0;
    this.modelo.unidad = this.modelo.tipo === 'SERVICIO' ? 'PAGINA' : 'UNIDAD';
    this.modelo.stockMinimo = this.modelo.tipo === 'SERVICIO' ? 0 : 5;
  }
  nuevoServicio() {
    this.nuevo();
    this.modelo.tipo = 'SERVICIO';
    this.modelo.categoria =
      this.categorias().find((c) => c.activo && c.nombre === 'Impresiones')?.nombre ??
      this.modelo.categoria;
    this.cambiarTipo();
  }
  pagina = 0;
  paginas = signal(0);
  total = signal(0);
  private consulta = 0;
  private demora?: ReturnType<typeof setTimeout>;
  constructor() {
    this.cargar();
    this.cargarCategorias();
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
    this.reiniciarImagen();
    this.compraPorCaja = false;
    this.vendeCaja = false;
    this.cajaConfig = { unidades: 12, precioCompra: 0, precioVenta: null, cajasIniciales: 0 };
    this.codigoAutomatico = true;
    this.id = undefined;
    this.modelo = this.vacio();
    this.categoriaAnterior = undefined;
    this.modelo.categoria =
      this.categorias().find((c) => c.activo && c.nombre === 'General')?.nombre ??
      this.categorias().find((c) => c.activo)?.nombre ??
      '';
    this.editor.set(true);
  }
  cerrar() {
    this.reiniciarImagen();
    this.id = undefined;
    this.modelo = this.vacio();
    this.editor.set(false);
  }
  editar(p: Producto) {
    this.reiniciarImagen();
    this.compraPorCaja = !!p.caja;
    this.vendeCaja = p.caja?.precioVenta != null;
    this.cajaConfig = {
      unidades: p.caja?.unidades ?? 12,
      precioCompra: p.caja?.precioCompra ?? 0,
      precioVenta: p.caja?.precioVenta ?? null,
      cajasIniciales: 0,
    };
    this.codigoAutomatico = false;
    this.id = p.id;
    const { id, stock, ...input } = p;
    this.modelo = input;
    this.categoriaAnterior = p.categoria;
    this.editor.set(true);
    document
      .getElementById('product-editor')
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  guardar() {
    if (this.busy() || this.imagenCargando() || this.imagenError()) return;
    this.busy.set(true);
    const entrada = {
      ...this.modelo,
      imagen: this.imagenNueva() ?? undefined,
      quitarImagen: this.quitarImagen,
      caja: this.compraPorCaja
        ? {
            ...this.cajaConfig,
            precioVenta: this.vendeCaja ? this.cajaConfig.precioVenta : null,
            cajasIniciales: this.id === undefined ? this.cajaConfig.cajasIniciales : 0,
          }
        : null,
      sku: this.id === undefined && this.codigoAutomatico ? '' : this.modelo.sku,
    };
    this.api.guardar(entrada, this.id).subscribe({
      next: (producto) => {
        this.busy.set(false);
        this.mensaje.set('Producto guardado. Código: ' + producto.sku);
        this.cerrar();
        this.cargar();
      },
      error: (e) => {
        this.busy.set(false);
        this.mensaje.set(
          'No se pudo guardar: ' +
            (e.error?.message ?? 'verifica los campos y que el código sea único.'),
        );
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
