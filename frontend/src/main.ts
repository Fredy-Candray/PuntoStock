import { Component, effect, inject } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient,withInterceptors } from '@angular/common/http';
import { provideRouter,Router,RouterLink,RouterLinkActive,RouterOutlet,Routes,NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter,map } from 'rxjs';
import { IconComponent } from './app/core/icon.component';
import { AuthService } from './app/core/api.service';
import { authGuard,adminGuard,menuGuard,authInterceptor } from './app/core/security';
import { MenuService } from './app/core/menu.service';
import { ThemeToggleComponent } from './app/core/theme-toggle.component';
import { UserMenuComponent } from './app/core/user-menu.component';
const routes:Routes=[
 {path:'login',loadComponent:()=>import('./app/features/login.component').then(m=>m.LoginComponent)},
 {path:'perfil',canActivate:[authGuard],loadComponent:()=>import('./app/features/perfil.component').then(m=>m.PerfilComponent)},
 {path:'pos',canActivate:[authGuard,menuGuard],loadComponent:()=>import('./app/features/pos.component').then(m=>m.PosComponent)},
 {path:'ventas',canActivate:[authGuard,menuGuard],loadComponent:()=>import('./app/features/ventas.component').then(m=>m.VentasComponent)},
 {path:'dashboard',canActivate:[authGuard,adminGuard,menuGuard],loadComponent:()=>import('./app/features/dashboard.component').then(m=>m.DashboardComponent)},
 {path:'productos',canActivate:[authGuard,adminGuard,menuGuard],loadComponent:()=>import('./app/features/productos.component').then(m=>m.ProductosComponent)},
 {path:'inventario',canActivate:[authGuard,adminGuard,menuGuard],loadComponent:()=>import('./app/features/inventario.component').then(m=>m.InventarioComponent)},
 {path:'usuarios',canActivate:[authGuard,adminGuard,menuGuard],loadComponent:()=>import('./app/features/usuarios.component').then(m=>m.UsuariosComponent)},
 {path:'administrar-menu',canActivate:[authGuard,adminGuard,menuGuard],loadComponent:()=>import('./app/features/administrar-menu.component').then(m=>m.AdministrarMenuComponent)},
 {path:'**',redirectTo:'pos'}
];
@Component({selector:'app-root',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet,IconComponent,ThemeToggleComponent,UserMenuComponent],template:`
 @if(auth.token()&&!url().startsWith('/login')){
  <div class="app-layout">
   <aside class="sidebar">
    <a class="brand brand-light sidebar-brand" [routerLink]="menu.inicio()" aria-label="PuntoStock, inicio"><img class="sidebar-store-icon" src="assets/puntostock-icon-192.png" alt="" width="48" height="48"><span class="brand-copy"><span class="brand-name"><span class="brand-punto">Punto</span><span class="brand-stock">Stock</span></span><small>VENTAS E INVENTARIO</small></span></a>
    <nav class="side-nav" aria-label="Navegación principal">
     @for(s of menu.secciones();track s.id){<div class="nav-section"><div class="nav-label">{{s.titulo}}</div>@for(o of s.opciones;track o.id){<a [routerLink]="o.ruta" routerLinkActive="active"><ps-icon [name]="o.icono"/>{{o.etiqueta}}</a>}</div>}
     @if(menu.error()){<button class="btn btn-quiet" (click)="recargarMenu()">Reintentar cargar menú</button>}
    </nav>
    <div class="sidebar-bottom"><div class="sidebar-theme"><ps-theme-toggle/></div></div>
   </aside>
   <div class="workspace">
    <header class="workspace-header"><div><span class="header-caption">TU NEGOCIO, BAJO CONTROL</span><strong>{{pageTitle()}}</strong></div><ps-user-menu/></header>
    <main class="workspace-content" [class.home-watermark]="url()==='/pos'||url()==='/dashboard'"><router-outlet/></main>
    <footer class="workspace-footer"><span>PuntoStock · Ventas e inventario</span><span>Hecho para simplificar tu operación</span></footer>
   </div>
  </div>
 }@else{<div class="login-theme-toggle"><ps-theme-toggle/></div><router-outlet/>}
`})
class App {
 auth=inject(AuthService);router=inject(Router);menu=inject(MenuService);
 url=toSignal(this.router.events.pipe(filter((event):event is NavigationEnd=>event instanceof NavigationEnd),map(event=>event.urlAfterRedirects)),{initialValue:this.router.url});
 constructor(){effect(onCleanup=>{if(this.auth.token()){const sub=this.auth.cargarPerfil().subscribe({error:()=>{}});const nav=this.menu.cargar().subscribe({error:()=>{}});onCleanup(()=>{sub.unsubscribe();nav.unsubscribe();});}else this.menu.limpiar();});}
 recargarMenu(){this.menu.cargar().subscribe({error:()=>{}});}
 pageTitle(){return this.menu.secciones().flatMap(s=>s.opciones).find(o=>o.ruta===this.url())?.etiqueta||({'/perfil':'Mi perfil'} as Record<string,string>)[this.url()]||'PuntoStock';}
}
bootstrapApplication(App,{providers:[provideHttpClient(withInterceptors([authInterceptor])),provideRouter(routes)]}).catch(console.error);
