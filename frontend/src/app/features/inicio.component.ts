import { Component } from '@angular/core';

@Component({
  selector: 'ps-inicio',
  standalone: true,
  template: `
    <div class="workspace-home" aria-label="Inicio de PuntoStock">
      <div class="home-brand">
        <img src="assets/puntostock-icon-192.png" alt="" width="192" height="192" />
        <div class="home-brand-copy">
          <div class="home-brand-name"><span>Punto</span><span>Stock</span></div>
          <small>VENTAS E INVENTARIO</small>
        </div>
      </div>
    </div>
  `,
})
export class InicioComponent {}
