import { describe, it, expect, vi } from 'vitest';
import { Injector, runInInjectionContext } from '@angular/core';
import { HttpClient, HttpRequest, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError, firstValueFrom, Subject } from 'rxjs';
import { AuthService } from '../src/app/core/auth.service';
import { MenuService } from '../src/app/core/menu.service';
import { authInterceptor } from '../src/app/core/security';
import { totalCentavos } from '../src/app/core/dinero';
describe('dinero y sesión', () => {
  it('cobra centavos sin el error de 0.1 + 0.2', () =>
    expect(
      totalCentavos([
        { producto: { precioVenta: 0.1 }, cantidad: 1 },
        { producto: { precioVenta: 0.2 }, cantidad: 1 },
      ]),
    ).toBe(30));
  it('mantiene exacto el total de cien fotocopias', () =>
    expect(totalCentavos([{ producto: { precioVenta: 0.15 }, cantidad: 100 }])).toBe(1500));
  it('elimina las credenciales antiguas de ambos almacenamientos y conserva el nuevo token solo en memoria', () => {
    localStorage.setItem('puntostock.session', 'viejo');
    sessionStorage.setItem('puntostock.session', 'viejo');
    const injector = Injector.create({
      providers: [AuthService, { provide: HttpClient, useValue: {} }],
    });
    const auth = injector.get(AuthService);
    auth.iniciarSesion({ accessToken: 'nuevo', roles: ['ADMIN'], expiresIn: 900, username: 'ana' });
    expect(localStorage.getItem('puntostock.session')).toBeNull();
    expect(sessionStorage.getItem('puntostock.session')).toBeNull();
    expect(auth.token()).toBe('nuevo');
  });
  it('agrupa renovaciones simultáneas y no duplica rotaciones', async () => {
    const response = new Subject<unknown>();
    const post = vi.fn(() => response);
    const auth = Injector.create({
      providers: [AuthService, { provide: HttpClient, useValue: { post } }],
    }).get(AuthService);
    const a = firstValueFrom(auth.renovar()),
      b = firstValueFrom(auth.renovar());
    expect(post).toHaveBeenCalledTimes(1);
    response.next({ accessToken: 'nuevo', roles: [], expiresIn: 900, username: 'ana' });
    response.complete();
    await Promise.all([a, b]);
    expect(auth.username()).toBe('ana');
  });
  it('menú evita consultas repetidas pero permite invalidación explícita', async () => {
    const get = vi.fn(() => of([]));
    const menu = Injector.create({
      providers: [MenuService, { provide: HttpClient, useValue: { get } }],
    }).get(MenuService);
    await firstValueFrom(menu.cargar());
    await firstValueFrom(menu.cargar());
    expect(get).toHaveBeenCalledTimes(1);
    await firstValueFrom(menu.cargar(true));
    expect(get).toHaveBeenCalledTimes(2);
  });
  it('un 401 renueva y repite la petición con el nuevo acceso', async () => {
    const auth = {
      token: () => 'viejo',
      renovar: () => of({ accessToken: 'nuevo' }),
      limpiar: vi.fn(),
    };
    const navegar = vi.fn();
    const injector = Injector.create({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: { navigateByUrl: navegar } },
      ],
    });
    const next = vi
      .fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 401 })))
      .mockReturnValueOnce(of(new HttpResponse({ status: 200 })));
    await firstValueFrom(
      runInInjectionContext(injector, () =>
        authInterceptor(new HttpRequest('GET', '/api/productos'), next),
      ),
    );
    expect(next.mock.calls[1][0].headers.get('Authorization')).toBe('Bearer nuevo');
    expect(navegar).not.toHaveBeenCalled();
  });
});
