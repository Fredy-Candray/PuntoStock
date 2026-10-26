import { Component, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { A11yModule } from '@angular/cdk/a11y';
import { AuthService, PerfilInput } from '../core/api.service';
import { IconComponent } from '../core/icon.component';

@Component({
  standalone: true,
  imports: [FormsModule, IconComponent, A11yModule],
  templateUrl: './perfil.component.html',
})
export class PerfilComponent {
  readonly auth = inject(AuthService);
  loading = signal(true);
  busy = signal(false);
  editing = signal(false);
  error = signal('');
  message = signal('');
  draft: PerfilInput = { nombre: '', correo: '', telefono: '' };
  crop = signal('');
  expanded = signal('');
  target: 'foto' | 'portada' = 'foto';
  frameWidth = 240;
  frameHeight = 240;
  zoom = 1;
  offsetX = 0;
  offsetY = 0;
  private image: HTMLImageElement | null = null;
  private pointer: { x: number; y: number; offsetX: number; offsetY: number } | null = null;
  constructor() {
    this.cargar();
  }
  cargar() {
    this.loading.set(true);
    this.error.set('');
    this.auth.cargarPerfil().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar tu perfil. Intenta nuevamente.');
      },
    });
  }
  editar() {
    const p = this.auth.perfil();
    if (!p) return;
    this.draft = { nombre: p.nombre, correo: p.correo ?? '', telefono: p.telefono ?? '' };
    this.error.set('');
    this.message.set('');
    this.editing.set(true);
  }
  guardar() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.auth
      .guardarPerfil({
        nombre: this.draft.nombre.trim(),
        correo: this.draft.correo.trim(),
        telefono: this.draft.telefono.trim(),
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.editing.set(false);
          this.message.set('Tu perfil se actualizó correctamente.');
        },
        error: () => {
          this.busy.set(false);
          this.error.set('No se pudieron guardar los cambios. Revisa nombre, correo y teléfono.');
        },
      });
  }
  creado() {
    const value = this.auth.perfil()?.fechaCreacion;
    return value
      ? new Intl.DateTimeFormat('es-SV', {
          dateStyle: 'long',
          timeZone: 'America/El_Salvador',
        }).format(new Date(value))
      : '';
  }
  seleccionar(event: Event, target: 'foto' | 'portada') {
    const input = event.target as HTMLInputElement,
      file = input.files?.[0];
    input.value = '';
    if (!file || this.busy()) return;
    this.error.set('');
    this.message.set('');
    if (
      !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) ||
      file.size > 10 * 1024 * 1024
    ) {
      this.error.set('Selecciona una imagen JPEG, PNG o WebP de hasta 10 MB.');
      return;
    }
    const reader = new FileReader();
    reader.onerror = () => this.error.set('No se pudo leer la imagen.');
    reader.onload = () => {
      const image = new Image();
      image.onerror = () => this.error.set('El archivo no es una imagen válida.');
      image.onload = () => {
        if (image.naturalWidth * image.naturalHeight > 40_000_000) {
          this.error.set('La imagen es demasiado grande. Usa una de hasta 40 megapíxeles.');
          return;
        }
        this.target = target;
        this.frameWidth = Math.min(target === 'portada' ? 300 : 240, window.innerWidth - 80);
        this.frameHeight = target === 'portada' ? this.frameWidth / 3 : this.frameWidth;
        this.image = image;
        this.zoom = 1;
        this.offsetX = 0;
        this.offsetY = 0;
        this.crop.set(String(reader.result));
      };
      image.src = String(reader.result);
    };
    reader.readAsDataURL(file);
  }
  ancho() {
    return this.image
      ? this.image.naturalWidth *
          Math.max(
            this.frameWidth / this.image.naturalWidth,
            this.frameHeight / this.image.naturalHeight,
          )
      : 0;
  }
  alto() {
    return this.image
      ? this.image.naturalHeight *
          Math.max(
            this.frameWidth / this.image.naturalWidth,
            this.frameHeight / this.image.naturalHeight,
          )
      : 0;
  }
  transform() {
    return `translate(calc(-50% + ${this.offsetX}px),calc(-50% + ${this.offsetY}px)) scale(${this.zoom})`;
  }
  ajustar() {
    this.offsetX = Math.max(
      -(this.ancho() * this.zoom - this.frameWidth) / 2,
      Math.min((this.ancho() * this.zoom - this.frameWidth) / 2, this.offsetX),
    );
    this.offsetY = Math.max(
      -(this.alto() * this.zoom - this.frameHeight) / 2,
      Math.min((this.alto() * this.zoom - this.frameHeight) / 2, this.offsetY),
    );
  }
  comenzar(event: PointerEvent) {
    event.preventDefault();
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
    this.pointer = {
      x: event.clientX,
      y: event.clientY,
      offsetX: this.offsetX,
      offsetY: this.offsetY,
    };
  }
  arrastrar(event: PointerEvent) {
    if (!this.pointer) return;
    this.offsetX = this.pointer.offsetX + event.clientX - this.pointer.x;
    this.offsetY = this.pointer.offsetY + event.clientY - this.pointer.y;
    this.ajustar();
  }
  soltar() {
    this.pointer = null;
  }
  cancelarFoto() {
    this.crop.set('');
    this.image = null;
    this.pointer = null;
  }
  aplicarFoto() {
    if (!this.image || this.busy()) return;
    const canvas = document.createElement('canvas');
    canvas.width = this.target === 'portada' ? 1500 : 512;
    canvas.height = this.target === 'portada' ? 500 : 512;
    const context = canvas.getContext('2d');
    if (!context) {
      this.error.set('No se pudo preparar la imagen.');
      this.cancelarFoto();
      return;
    }
    const scale =
      Math.max(canvas.width / this.image.naturalWidth, canvas.height / this.image.naturalHeight) *
      this.zoom;
    const width = this.image.naturalWidth * scale,
      height = this.image.naturalHeight * scale;
    context.fillStyle = '#fff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(
      this.image,
      (canvas.width - width) / 2 + (this.offsetX * canvas.width) / this.frameWidth,
      (canvas.height - height) / 2 + (this.offsetY * canvas.height) / this.frameHeight,
      width,
      height,
    );
    let quality = 0.85,
      data = canvas.toDataURL('image/jpeg', quality);
    while (data.length > 1_300_000 && quality > 0.35) {
      quality -= 0.1;
      data = canvas.toDataURL('image/jpeg', quality);
    }
    if (data.length > 1_300_000) {
      this.error.set('La imagen sigue siendo demasiado grande. Prueba con otra.');
      this.cancelarFoto();
      return;
    }
    this.cancelarFoto();
    this.enviarFoto(data, this.target === 'portada');
  }
  quitarFoto(portada = false) {
    if (!this.busy()) this.enviarFoto('', portada);
  }
  private enviarFoto(imagen: string, portada: boolean) {
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.auth.guardarFoto(imagen, portada).subscribe({
      next: () => {
        this.busy.set(false);
        this.message.set(portada ? 'Portada actualizada.' : 'Foto de perfil actualizada.');
      },
      error: () => {
        this.busy.set(false);
        this.error.set('No se pudo guardar la imagen. Inténtalo nuevamente.');
      },
    });
  }
  @HostListener('document:keydown.escape') escape() {
    this.cancelarFoto();
    this.expanded.set('');
  }
}
