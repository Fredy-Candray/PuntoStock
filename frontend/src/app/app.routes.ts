import { Routes } from '@angular/router';
import { authGuard, adminGuard, menuGuard } from './core/security';
export const routes: Routes = [
  {
    path: 'inicio',
    canActivate: [authGuard],
    loadComponent: () => import('./features/inicio.component').then((m) => m.InicioComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  {
    path: 'login',
    loadComponent: () => import('./features/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/perfil.component').then((m) => m.PerfilComponent),
  },
  {
    path: 'pos',
    canActivate: [authGuard, menuGuard],
    loadComponent: () => import('./features/pos.component').then((m) => m.PosComponent),
  },
  {
    path: 'ventas',
    canActivate: [authGuard, menuGuard],
    loadComponent: () => import('./features/ventas.component').then((m) => m.VentasComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, adminGuard, menuGuard],
    loadComponent: () => import('./features/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'productos',
    canActivate: [authGuard, adminGuard, menuGuard],
    loadComponent: () => import('./features/productos.component').then((m) => m.ProductosComponent),
  },
  {
    path: 'inventario',
    canActivate: [authGuard, adminGuard, menuGuard],
    loadComponent: () =>
      import('./features/inventario.component').then((m) => m.InventarioComponent),
  },
  {
    path: 'usuarios',
    canActivate: [authGuard, adminGuard, menuGuard],
    loadComponent: () => import('./features/usuarios.component').then((m) => m.UsuariosComponent),
  },
  {
    path: 'administrar-menu',
    canActivate: [authGuard, adminGuard, menuGuard],
    loadComponent: () =>
      import('./features/administrar-menu.component').then((m) => m.AdministrarMenuComponent),
  },
  { path: '**', redirectTo: 'pos' },
];
