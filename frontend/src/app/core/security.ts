import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './api.service';
export const authInterceptor:HttpInterceptorFn=(req,next)=>{
 const auth=inject(AuthService),router=inject(Router);
 const request=req.url.startsWith('/api/')&&auth.token()?req.clone({setHeaders:{Authorization:'Bearer '+auth.token()}}):req;
 return next(request).pipe(catchError(error=>{if(error.status===401){auth.logout();void router.navigateByUrl('/login');}return throwError(()=>error);}));
};
export const authGuard:CanActivateFn=()=>inject(AuthService).token()?true:inject(Router).createUrlTree(['/login']);
export const adminGuard:CanActivateFn=()=>inject(AuthService).admin()?true:inject(Router).createUrlTree(['/pos']);
