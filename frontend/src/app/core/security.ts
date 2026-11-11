import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { MenuService } from './menu.service';
import { map, of, switchMap } from 'rxjs';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './api.service';
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService),
    router = inject(Router);
  const protegida =
    req.url.startsWith('/api/') &&
    ![
      '/api/auth/login',
      '/api/auth/renovar',
      '/api/auth/salir',
      '/api/auth/recuperacion',
      '/api/auth/restablecer',
    ].includes(req.url);
  const request =
    protegida && auth.token()
      ? req.clone({ setHeaders: { Authorization: 'Bearer ' + auth.token() } })
      : req;
  return next(request).pipe(
    catchError((error) => {
      if (error.status !== 401 || !protegida) return throwError(() => error);
      return auth.renovar().pipe(
        switchMap((s) =>
          next(req.clone({ setHeaders: { Authorization: 'Bearer ' + s.accessToken } })),
        ),
        catchError((e) => {
          auth.limpiar();
          void router.navigateByUrl('/login');
          return throwError(() => e);
        }),
      );
    }),
  );
};
export const authGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService),
    router = inject(Router);
  const destino = () => {
    if (auth.cambioClavePendiente())
      return route.routeConfig?.path === 'cambiar-clave'
        ? true
        : router.createUrlTree(['/cambiar-clave']);
    return route.routeConfig?.path === 'cambiar-clave'
      ? router.parseUrl(auth.destinoInicio())
      : true;
  };
  return auth.token()
    ? destino()
    : auth.restaurar().pipe(map((s) => (s ? destino() : router.createUrlTree(['/login']))));
};
export const adminGuard: CanActivateFn = () =>
  inject(AuthService).admin() ? true : inject(Router).createUrlTree(['/pos']);
export const menuGuard: CanActivateFn = (route) => {
  const menu = inject(MenuService),
    router = inject(Router),
    path = '/' + route.routeConfig?.path;
  return menu.cargar().pipe(
    map((s) =>
      s.some((section) => section.opciones.some((o) => o.ruta === path))
        ? true
        : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/perfil']))),
  );
};
