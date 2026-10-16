import { Component, inject } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient,withInterceptors } from '@angular/common/http';
import { provideRouter,Router,RouterLink,RouterLinkActive,RouterOutlet,Routes,NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter,map } from 'rxjs';
import { IconComponent } from './app/core/icon.component';
import { AuthService } from './app/core/api.service';
import { authGuard,adminGuard,authInterceptor } from './app/core/security';
const routes:Routes=[
 {path:'login',loadComponent:()=>import('./app/features/login.component').then(m=>m.LoginComponent)},
 {path:'pos',canActivate:[authGuard],loadComponent:()=>import('./app/features/pos.component').then(m=>m.PosComponent)},
 {path:'dashboard',canActivate:[authGuard,adminGuard],loadComponent:()=>import('./app/features/dashboard.component').then(m=>m.DashboardComponent)},
 {path:'productos',canActivate:[authGuard,adminGuard],loadComponent:()=>import('./app/features/productos.component').then(m=>m.ProductosComponent)},
 {path:'inventario',canActivate:[authGuard,adminGuard],loadComponent:()=>import('./app/features/inventario.component').then(m=>m.InventarioComponent)},
 {path:'**',redirectTo:'pos'}
];
@Component({selector:'app-root',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet,IconComponent],template:`
 @if(auth.token()&&!url().startsWith('/login')){
  <div class="app-layout">
   <aside class="sidebar">
    <a class="brand brand-light" [routerLink]="auth.admin()?'/dashboard':'/pos'"><span class="brand-mark"><ps-icon name="box"/></span><span>PuntoStock<small>VENTAS E INVENTARIO</small></span></a>
    <div class="nav-label">ESPACIO DE TRABAJO</div>
    <nav class="side-nav" aria-label="Navegación principal">
     @if(auth.admin()){<a routerLink="/dashboard" routerLinkActive="active"><ps-icon name="grid"/> Resumen</a>}
     <a routerLink="/pos" routerLinkActive="active"><ps-icon name="cart"/> Punto de venta</a>
     @if(auth.admin()){<a routerLink="/productos" routerLinkActive="active"><ps-icon name="box"/> Productos</a><a routerLink="/inventario" routerLinkActive="active"><ps-icon name="history"/> Inventario</a>}
    </nav>
    <div class="sidebar-bottom"><div class="sidebar-tip"><ps-icon name="shield"/><span>{{auth.admin()?'Administración del negocio':'Espacio del vendedor'}}<small>Permisos según tu rol</small></span></div><button class="logout-button" (click)="salir()"><ps-icon name="logout"/> Cerrar sesión</button></div>
   </aside>
   <div class="workspace">
    <header class="workspace-header"><div><span class="header-caption">TU NEGOCIO, BAJO CONTROL</span><strong>{{pageTitle()}}</strong></div><div class="account"><div class="account-text"><strong>{{auth.username()||'Mi cuenta'}}</strong><span>{{auth.admin()?'Administrador':'Vendedor'}}</span></div><span class="avatar">{{(auth.username()||'P').slice(0,1).toUpperCase()}}</span></div></header>
    <main class="workspace-content"><router-outlet/></main>
    <footer class="workspace-footer"><span>PuntoStock · Ventas e inventario</span><span>Hecho para simplificar tu operación</span></footer>
   </div>
  </div>
 }@else{<router-outlet/>}
`})
class App {
 auth=inject(AuthService);router=inject(Router);
 url=toSignal(this.router.events.pipe(filter((event):event is NavigationEnd=>event instanceof NavigationEnd),map(event=>event.urlAfterRedirects)),{initialValue:this.router.url});
 pageTitle(){return ({'/dashboard':'Resumen general','/pos':'Punto de venta','/productos':'Catálogo de productos','/inventario':'Control de inventario'} as Record<string,string>)[this.url()]||'PuntoStock';}
 salir(){this.auth.logout();void this.router.navigateByUrl('/login');}
}
bootstrapApplication(App,{providers:[provideHttpClient(withInterceptors([authInterceptor])),provideRouter(routes)]}).catch(console.error);
