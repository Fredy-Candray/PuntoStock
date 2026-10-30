import { Component, Input, OnChanges, OnDestroy, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { IconComponent } from './icon.component';

@Component({
  selector: 'ps-producto-imagen',
  standalone: true,
  imports: [IconComponent],
  template: `@if (url()) {
      <img [src]="url()" [alt]="alt" loading="lazy" (error)="fallo()" />
    } @else {
      <ps-icon [name]="icon" />
    }`,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        flex-shrink: 0;
      }
      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
      ps-icon {
        width: 60%;
        height: 60%;
      }
    `,
  ],
})
export class ProductoImagenComponent implements OnChanges, OnDestroy {
  @Input() src: string | null | undefined = '';
  @Input() alt = 'Imagen del producto';
  @Input() icon = 'box';
  private http = inject(HttpClient);
  private peticion?: Subscription;
  private objeto?: string;
  url = signal<string | null>(null);
  ngOnChanges() {
    this.liberar();
    this.url.set(null);
    const src = this.src;
    if (!src || src === '/assets/producto.svg' || src === '/assets/producto-demo.svg') return;
    if (/^\/api\/productos\/\d+\/imagen(?:\?.*)?$/.test(src)) {
      this.peticion = this.http.get(src, { responseType: 'blob' }).subscribe({
        next: (blob) => {
          this.objeto = URL.createObjectURL(blob);
          this.url.set(this.objeto);
        },
        error: () => this.url.set(null),
      });
    } else this.url.set(src);
  }
  fallo() {
    this.url.set(null);
  }
  private liberar() {
    this.peticion?.unsubscribe();
    if (this.objeto) URL.revokeObjectURL(this.objeto);
    this.objeto = undefined;
  }
  ngOnDestroy() {
    this.liberar();
  }
}
