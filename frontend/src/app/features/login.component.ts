import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { IconComponent } from '../core/icon.component';
import { AuthService } from '../core/api.service';
@Component({
  standalone: true,
  imports: [FormsModule, IconComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  auth = inject(AuthService);
  router = inject(Router);
  username = '';
  password = '';
  error = signal('');
  busy = signal(false);
  visible = signal(false);
  rememberMe = false;
  http = inject(HttpClient);
  route = inject(ActivatedRoute);
  recuperacion = signal<'solicitar' | 'restablecer' | null>(null);
  correo = '';
  nuevaClave = '';
  confirmacion = '';
  tokenRecuperacion = '';
  mensaje = signal('');
  constructor() {
    const fragment = new URLSearchParams(this.route.snapshot.fragment ?? '');
    if (fragment.has('recuperar')) {
      this.tokenRecuperacion = fragment.get('recuperar') ?? '';
      this.username = fragment.get('usuario') ?? '';
      this.recuperacion.set('restablecer');
      history.replaceState(null, '', location.pathname + location.search);
    } else if (this.auth.token())
      void this.router.navigateByUrl(this.auth.admin() ? '/dashboard' : '/pos');
  }
  abrirRecuperacion() {
    this.error.set('');
    this.mensaje.set('');
    this.recuperacion.set('solicitar');
    this.password = '';
  }
  volver() {
    this.recuperacion.set(null);
    this.error.set('');
    this.mensaje.set('');
    this.nuevaClave = '';
    this.confirmacion = '';
    this.tokenRecuperacion = '';
  }
  recuperar() {
    if (this.busy()) return;
    const restablecer = this.recuperacion() === 'restablecer';
    if (
      restablecer &&
      (this.nuevaClave !== this.confirmacion ||
        this.nuevaClave.length < 12 ||
        new TextEncoder().encode(this.nuevaClave).length > 72)
    ) {
      this.error.set(
        'Las contraseñas deben coincidir y tener al menos 12 caracteres y como máximo 72 bytes.',
      );
      return;
    }
    this.error.set('');
    this.mensaje.set('');
    this.busy.set(true);
    const body = restablecer
      ? { username: this.username, token: this.tokenRecuperacion, password: this.nuevaClave }
      : { username: this.username.trim(), correo: this.correo.trim() };
    this.http
      .post<{ mensaje: string }>(
        '/api/auth/' + (restablecer ? 'restablecer' : 'recuperacion'),
        body,
      )
      .subscribe({
        next: (r) => {
          this.busy.set(false);
          if (restablecer) {
            this.auth.logout();
            this.volver();
          }
          this.mensaje.set(r.mensaje);
        },
        error: (e: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(
            e.status === 429
              ? 'Demasiados intentos. Espera un minuto antes de volver a intentar.'
              : e.status === 503
                ? 'El envío de recuperación no está disponible. Contacta a tu administrador para revisar la configuración de Gmail.'
                : e.status === 400
                  ? 'Revisa los datos. Si el enlace venció o ya fue utilizado, solicita uno nuevo.'
                  : 'No se pudo completar la solicitud. Inténtalo más tarde.',
          );
        },
      });
  }
  entrar() {
    if (this.busy() || !this.username.trim() || !this.password) return;
    this.error.set('');
    this.busy.set(true);
    this.auth.login(this.username.trim(), this.password, this.rememberMe).subscribe({
      next: (r) => {
        this.auth.iniciarSesion(r, this.username.trim(), this.rememberMe);
        this.busy.set(false);
        this.password = '';
        void this.router.navigateByUrl(this.auth.admin() ? '/dashboard' : '/pos');
      },
      error: (e: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(
          e.status === 429
            ? 'Demasiados intentos. Espera un minuto.'
            : e.status === 401
              ? 'El usuario o la contraseña no son correctos. Revisa la clave e inténtalo de nuevo.'
              : [0, 502, 503, 504].includes(e.status)
                ? 'No pudimos conectar con el servidor. Comprueba que el backend esté iniciado.'
                : 'No se pudo completar el ingreso. Inténtalo de nuevo.',
        );
      },
    });
  }
}
