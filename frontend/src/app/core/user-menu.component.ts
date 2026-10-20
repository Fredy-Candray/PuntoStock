import { Component, ElementRef, HostListener, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './api.service';
import { IconComponent } from './icon.component';

@Component({selector:'ps-user-menu',standalone:true,imports:[RouterLink,IconComponent],template:`
 <div class="user-menu">
  <button #trigger class="user-menu-trigger" type="button" (click)="open.set(!open())"
   aria-label="Abrir menú de usuario" [attr.aria-expanded]="open()" aria-controls="user-options">
   <span class="avatar">@if(auth.perfil()?.fotoPerfil){<img [src]="auth.perfil()!.fotoPerfil" alt="">}@else{<span>{{(auth.perfil()?.nombre||auth.username()||'P').slice(0,1).toUpperCase()}}</span>}</span>
   <span class="account-text"><strong>{{auth.perfil()?.nombre||auth.username()||'Mi cuenta'}}</strong><span>{{auth.admin()?'Administrador':'Vendedor'}}</span></span>
   <ps-icon name="chevronDown"/>
  </button>
  @if(open()){
   <nav id="user-options" class="user-dropdown" aria-label="Opciones de usuario">
    <a routerLink="/perfil" (click)="open.set(false)"><ps-icon name="user"/> Perfil</a>
    <button type="button" (click)="salir()"><ps-icon name="logout"/> Cerrar sesión</button>
   </nav>
  }
 </div>
`})
export class UserMenuComponent {
 readonly auth=inject(AuthService);private readonly router=inject(Router);private readonly host=inject(ElementRef);
 readonly open=signal(false);readonly trigger=viewChild<ElementRef<HTMLButtonElement>>('trigger');
 @HostListener('document:click',['$event']) fuera(event:MouseEvent){if(!this.host.nativeElement.contains(event.target))this.open.set(false);}
 @HostListener('document:focusin',['$event']) foco(event:FocusEvent){if(!this.host.nativeElement.contains(event.target))this.open.set(false);}
 @HostListener('document:keydown.escape') escape(){if(this.open()){this.open.set(false);this.trigger()?.nativeElement.focus();}}
 salir(){this.open.set(false);this.auth.logout();void this.router.navigateByUrl('/login');}
}
