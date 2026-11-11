import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../core/api.service';
import { IconComponent } from '../core/icon.component';

@Component({
  standalone: true,
  imports: [FormsModule, IconComponent],
  template: `
    <main class="clave-inicial">
      <section class="panel clave-inicial-panel" aria-labelledby="clave-titulo">
        <div class="panel-body">
          <img
            class="clave-logo"
            src="/assets/puntostock-icon-192.png"
            alt="PuntoStock"
            width="80"
            height="80"
          />
          <h1 id="clave-titulo">Crea tu contraseña personal</h1>
          <p>
            Hola, {{ auth.username() }}. Tu administrador restableció tu acceso. Cambia la
            contraseña temporal para continuar.
          </p>
          @if (guardada()) {
            <p class="notice notice-info" role="status">
              Contraseña actualizada. Ya puedes iniciar sesión con tu nueva contraseña.
            </p>
            <button class="btn btn-primary" type="button" (click)="irLogin()">
              Ir al inicio de sesión
            </button>
          } @else {
            <form #f="ngForm" (ngSubmit)="guardar()">
              <label
                >Nueva contraseña
                <div class="admin-password">
                  <input
                    name="password"
                    [type]="visible ? 'text' : 'password'"
                    [(ngModel)]="password"
                    required
                    minlength="8"
                    maxlength="72"
                    autocomplete="new-password"
                    aria-describedby="clave-ayuda"
                    [disabled]="busy()"
                  />
                  <button
                    class="icon-btn"
                    type="button"
                    (click)="visible = !visible"
                    [attr.aria-label]="visible ? 'Ocultar contraseñas' : 'Mostrar contraseñas'"
                  >
                    <ps-icon [name]="visible ? 'eyeOff' : 'eye'" />
                  </button>
                </div>
              </label>
              <p id="clave-ayuda" class="field-help">
                Usa al menos 8 caracteres y como máximo 72 bytes. Puedes usar una frase que
                recuerdes.
              </p>
              <label
                >Confirmar nueva contraseña
                <input
                  name="confirmacion"
                  [type]="visible ? 'text' : 'password'"
                  [(ngModel)]="confirmacion"
                  required
                  minlength="8"
                  maxlength="72"
                  autocomplete="new-password"
                  [disabled]="busy()"
                />
              </label>
              @if (error()) {
                <p class="notice notice-error" role="alert">{{ error() }}</p>
              }
              <button class="btn btn-primary" type="submit" [disabled]="f.invalid || busy()">
                {{ busy() ? 'Guardando…' : 'Guardar contraseña' }}
              </button>
              <button class="btn btn-quiet" type="button" (click)="salir()" [disabled]="busy()">
                Cerrar sesión
              </button>
            </form>
          }
        </div>
      </section>
    </main>
  `,
  styles: [
    `
      .clave-inicial {
        min-height: 100dvh;
        display: grid;
        place-items: center;
        padding: 80px 16px 32px;
      }
      .clave-inicial-panel {
        width: min(100%, 480px);
      }
      .clave-logo {
        display: block;
        margin: 0 auto 20px;
      }
      h1 {
        font-size: 25px;
        text-align: center;
        margin-bottom: 12px;
      }
      p {
        margin-bottom: 20px;
      }
      form {
        display: grid;
        gap: 16px;
      }
      .field-help {
        margin: 0;
      }
      .btn {
        justify-content: center;
      }
    `,
  ],
})
export class CambiarClaveComponent {
  auth = inject(AuthService);
  http = inject(HttpClient);
  router = inject(Router);
  password = '';
  confirmacion = '';
  visible = false;
  busy = signal(false);
  error = signal('');
  guardada = signal(false);
  guardar() {
    if (this.busy()) return;
    this.error.set('');
    if (
      this.password !== this.confirmacion ||
      this.password.length < 8 ||
      new TextEncoder().encode(this.password).length > 72
    ) {
      this.error.set(
        'Las contraseñas deben coincidir y tener al menos 8 caracteres y como máximo 72 bytes.',
      );
      return;
    }
    this.busy.set(true);
    this.http
      .post('/api/auth/cambiar-clave-inicial', {
        password: this.password,
        confirmacion: this.confirmacion,
      })
      .subscribe({
        next: () => {
          this.password = '';
          this.confirmacion = '';
          this.busy.set(false);
          this.guardada.set(true);
          this.auth.logout();
        },
        error: (e: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(
            e.error?.message || 'No se pudo guardar la contraseña. Inténtalo nuevamente.',
          );
        },
      });
  }
  irLogin() {
    void this.router.navigateByUrl('/login');
  }
  salir() {
    this.auth.logout();
    this.irLogin();
  }
}
