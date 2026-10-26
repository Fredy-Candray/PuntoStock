import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MenuService, ConfiguracionMenu, SeccionMenu, OpcionMenu } from '../core/menu.service';
import { IconComponent } from '../core/icon.component';
@Component({
  standalone: true,
  imports: [FormsModule, IconComponent],
  templateUrl: './administrar-menu.component.html',
})
export class AdministrarMenuComponent {
  menu = inject(MenuService);
  config = signal<ConfiguracionMenu | null>(null);
  loading = signal(true);
  busy = signal(false);
  error = signal('');
  mensaje = signal('');
  constructor() {
    this.cargar();
  }
  cargar() {
    this.loading.set(true);
    this.error.set('');
    this.menu.configuracion().subscribe({
      next: (c) => {
        this.config.set(c);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el menú. Comprueba la conexión y vuelve a cargar.');
      },
    });
  }
  agregarSeccion() {
    const c = this.config();
    if (!c) return;
    c.secciones.push({
      id: crypto.randomUUID(),
      titulo: 'Nueva sección',
      orden: c.secciones.length + 1,
      activo: true,
      opciones: [],
    });
  }
  quitarSeccion(s: SeccionMenu) {
    const c = this.config();
    if (c) c.secciones = c.secciones.filter((x) => x !== s);
  }
  agregarOpcion(s: SeccionMenu) {
    s.opciones.push({
      id: crypto.randomUUID(),
      etiqueta: '',
      ruta: '',
      icono: 'grid',
      orden: s.opciones.length + 1,
      activo: true,
      roles: ['ADMIN'],
    });
  }
  quitarOpcion(s: SeccionMenu, o: OpcionMenu) {
    s.opciones = s.opciones.filter((x) => x !== o);
  }
  permisos(o: OpcionMenu) {
    return this.config()?.rutas.find((r) => r.ruta === o.ruta)?.roles ?? [];
  }
  cambiarRuta(o: OpcionMenu) {
    const ruta = this.config()?.rutas.find((r) => r.ruta === o.ruta);
    if (ruta) {
      o.roles = [...ruta.roles];
      if (!o.etiqueta) o.etiqueta = ruta.etiqueta;
    }
  }
  cambiarRol(o: OpcionMenu, rol: string, event: Event) {
    o.roles = (event.target as HTMLInputElement).checked
      ? [...new Set([...o.roles, rol])]
      : o.roles.filter((r) => r !== rol);
  }
  guardar() {
    const c = this.config();
    if (!c || this.busy()) return;
    this.error.set('');
    this.mensaje.set('');
    if (
      c.secciones.some(
        (s) => !s.titulo.trim() || s.opciones.some((o) => !o.etiqueta.trim() || !o.ruta),
      )
    ) {
      this.error.set(
        'Completa el título de las secciones, el nombre y la pantalla de cada opción.',
      );
      return;
    }
    this.busy.set(true);
    this.menu.guardar(c).subscribe({
      next: (r) => {
        this.config.set(r);
        this.menu.cargar(true).subscribe({
          next: () => {
            this.busy.set(false);
            this.mensaje.set('Menú guardado. Los cambios ya se reflejan en la navegación.');
          },
          error: () => {
            this.busy.set(false);
            this.mensaje.set(
              'Menú guardado. Vuelve a cargar la página para actualizar la navegación.',
            );
          },
        });
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e.error?.message || 'No se pudo guardar el menú. Inténtalo de nuevo.');
      },
    });
  }
}
