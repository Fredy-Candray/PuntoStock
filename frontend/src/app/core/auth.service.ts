import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  tap,
  shareReplay,
  finalize,
  of,
  catchError,
  switchMap,
  throwError,
  Observable,
} from 'rxjs';
import type { Perfil, PerfilInput } from './api.service';
interface Sesion {
  accessToken: string;
  roles: string[];
  expiresIn: number;
  username: string;
}
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private renovando?: ReturnType<AuthService['peticionRenovar']>;
  private cerrando = false;
  token = signal<string | null>(null);
  roles = signal<string[]>([]);
  username = signal('');
  perfil = signal<Perfil | null>(null);
  perfilError = signal(false);
  constructor() {
    for (const persistente of [false, true]) {
      try {
        (persistente ? localStorage : sessionStorage).removeItem('puntostock.session');
      } catch {}
    }
  }
  iniciarSesion(s: Sesion, username = s.username, _rememberMe = false) {
    this.cerrando = false;
    this.token.set(s.accessToken);
    this.roles.set(s.roles);
    this.username.set(username);
  }
  login(username: string, password: string, rememberMe = false) {
    return this.http.post<Sesion>(
      '/api/auth/login',
      { username, password, rememberMe },
      { headers: { 'X-PuntoStock': '1' } },
    );
  }
  private peticionRenovar() {
    return this.http
      .post<Sesion>('/api/auth/renovar', {}, { headers: { 'X-PuntoStock': '1' } })
      .pipe(
        tap((s) => {
          if (!this.cerrando) this.iniciarSesion(s);
        }),
      );
  }
  renovar() {
    if (this.cerrando) return throwError(() => new Error('Sesión cerrada'));
    if (!this.renovando)
      this.renovando = this.peticionRenovar().pipe(
        finalize(() => (this.renovando = undefined)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    return this.renovando;
  }
  restaurar() {
    return this.renovar().pipe(
      catchError(() => {
        this.limpiar();
        return of(null);
      }),
    );
  }
  cargarPerfil() {
    return this.http.get<Perfil>('/api/auth/me').pipe(
      tap({
        next: (p) => {
          this.perfil.set(p);
          this.perfilError.set(false);
        },
        error: () => this.perfilError.set(true),
      }),
    );
  }
  guardarPerfil(input: PerfilInput) {
    return this.http.put<Perfil>('/api/auth/me', input).pipe(tap((p) => this.perfil.set(p)));
  }
  guardarFoto(imagen: string, portada = false) {
    return this.http
      .put<Perfil>('/api/auth/me/' + (portada ? 'portada' : 'foto'), { imagen })
      .pipe(tap((p) => this.perfil.set(p)));
  }
  admin() {
    return this.roles().includes('ADMIN');
  }
  limpiar() {
    this.token.set(null);
    this.roles.set([]);
    this.username.set('');
    this.perfil.set(null);
  }
  logout() {
    this.cerrando = true;
    this.limpiar();
    const pendiente: Observable<unknown> = this.renovando ?? of(null);
    pendiente
      .pipe(
        catchError(() => of(null)),
        switchMap(() =>
          this.http.post('/api/auth/salir', {}, { headers: { 'X-PuntoStock': '1' } }),
        ),
        finalize(() => this.limpiar()),
      )
      .subscribe({ error: () => this.perfilError.set(true) });
  }
}
