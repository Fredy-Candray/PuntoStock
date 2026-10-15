import { Component, inject } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient,withInterceptors } from '@angular/common/http';
import { provideRouter,Router,RouterLink,RouterOutlet,Routes } from '@angular/router';
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
@Component({selector:'app-root',standalone:true,imports:[RouterLink,RouterOutlet],template:`<nav><a routerLink="/pos">PuntoStock · POS</a>@if(auth.admin()){<a routerLink="/dashboard">Dashboard</a><a routerLink="/productos">Productos</a><a routerLink="/inventario">Inventario</a>}@if(auth.token()){<button (click)="salir()">Salir</button>}</nav><main><router-outlet/></main>`})
class App {auth=inject(AuthService);router=inject(Router);salir(){this.auth.logout();void this.router.navigateByUrl('/login');}}
bootstrapApplication(App,{providers:[provideHttpClient(withInterceptors([authInterceptor])),provideRouter(routes)]}).catch(console.error);
