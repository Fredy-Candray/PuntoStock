import { Component, effect, inject } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient,withInterceptors } from '@angular/common/http';
import { provideRouter,Router,RouterLink,RouterLinkActive,RouterOutlet,Routes,NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter,map } from 'rxjs';
import { IconComponent } from './app/core/icon.component';
import { AuthService } from './app/core/api.service';
import { authGuard,adminGuard,authInterceptor } from './app/core/security';
import { ThemeToggleComponent } from './app/core/theme-toggle.component';
import { UserMenuComponent } from './app/core/user-menu.component';
const routes:Routes=[
 {path:'login',loadComponent:()=>import('./app/features/login.component').then(m=>m.LoginComponent)},
 {path:'perfil',canActivate:[authGuard],loadComponent:()=>import('./app/features/perfil.component').then(m=>m.PerfilComponent)},
 {path:'pos',canActivate:[authGuard],loadComponent:()=>import('./app/features/pos.component').then(m=>m.PosComponent)},
 {path:'ventas',canActivate:[authGuard],loadComponent:()=>import('./app/features/ventas.component').then(m=>m.VentasComponent)},
 {path:'dashboard',canActivate:[authGuard,adminGuard],loadComponent:()=>import('./app/features/dashboard.component').then(m=>m.DashboardComponent)},
 {path:'productos',canActivate:[authGuard,adminGuard],loadComponent:()=>import('./app/features/productos.component').then(m=>m.ProductosComponent)},
 {path:'inventario',canActivate:[authGuard,adminGuard],loadComponent:()=>import('./app/features/inventario.component').then(m=>m.InventarioComponent)},
 {path:'**',redirectTo:'pos'}
];
@Component({selector:'app-root',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet,IconComponent,ThemeToggleComponent,UserMenuComponent],template:`
 @if(auth.token()&&!url().startsWith('/login')){
  <div class="app-layout">
   <aside class="sidebar">
    <a class="brand brand-light" [routerLink]="auth.admin()?'/dashboard':'/pos'"><span class="brand-mark"><ps-icon name="box"/></span><span>PuntoStock<small>VENTAS E INVENTARIO</small></span></a>
    <div class="nav-label">ESPACIO DE TRABAJO</div>
    <nav class="side-nav" aria-label="Navegación principal">
     @if(auth.admin()){<a routerLink="/dashboard" routerLinkActive="active"><ps-icon name="grid"/> Resumen</a>}
     <a routerLink="/pos" routerLinkActive="active"><ps-icon name="cart"/> Punto de venta</a>
     <a routerLink="/ventas" routerLinkActive="active"><ps-icon name="receipt"/> Ventas</a>
     @if(auth.admin()){<a routerLink="/productos" routerLinkActive="active"><ps-icon name="box"/> Productos</a><a routerLink="/inventario" routerLinkActive="active"><ps-icon name="history"/> Inventario</a>}
    </nav>
    <div class="sidebar-bottom"><div class="sidebar-theme"><ps-theme-toggle/></div></div>
   </aside>
   <div class="workspace">
    <header class="workspace-header"><div><span class="header-caption">TU NEGOCIO, BAJO CONTROL</span><strong>{{pageTitle()}}</strong></div><ps-user-menu/></header>
    <main class="workspace-content"><router-outlet/></main>
    <footer class="workspace-footer"><span>PuntoStock · Ventas e inventario</span><span>Hecho para simplificar tu operación</span></footer>
   </div>
  </div>
 }@else{<div class="login-theme-toggle"><ps-theme-toggle/></div><router-outlet/>}
`})
class App {
 auth=inject(AuthService);router=inject(Router);
 url=toSignal(this.router.events.pipe(filter((event):event is NavigationEnd=>event instanceof NavigationEnd),map(event=>event.urlAfterRedirects)),{initialValue:this.router.url});
 constructor(){effect(onCleanup=>{if(this.auth.token()){const sub=this.auth.cargarPerfil().subscribe({error:()=>{}});onCleanup(()=>sub.unsubscribe());}});}
 pageTitle(){return ({'/dashboard':'Resumen general','/pos':'Punto de venta','/ventas':'Historial de ventas','/productos':'Artículos y servicios','/inventario':'Control de inventario','/perfil':'Mi perfil'} as Record<string,string>)[this.url()]||'PuntoStock';}
}
bootstrapApplication(App,{providers:[provideHttpClient(withInterceptors([authInterceptor])),provideRouter(routes)]}).catch(console.error);
