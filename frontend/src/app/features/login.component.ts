import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { IconComponent } from '../core/icon.component';
import { AuthService } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,IconComponent],templateUrl:'./login.component.html'})
export class LoginComponent {
 auth=inject(AuthService);router=inject(Router);username='';password='';error=signal('');busy=signal(false);
 visible=signal(false);
 entrar(){
  if(this.busy()||!this.username.trim()||!this.password)return;
  this.error.set('');this.busy.set(true);
  this.auth.login(this.username.trim(),this.password).subscribe({next:r=>{
   this.auth.token.set(r.accessToken);this.auth.roles.set(r.roles);this.auth.username.set(this.username.trim());
   this.busy.set(false);this.password='';void this.router.navigateByUrl(this.auth.admin()?'/dashboard':'/pos');
  },error:(e:HttpErrorResponse)=>{
   this.busy.set(false);
   this.error.set(e.status===401?'El usuario o la contraseña no son correctos. Revisa la clave e inténtalo de nuevo.':
    [0,502,503,504].includes(e.status)?'No pudimos conectar con el servidor. Comprueba que el backend esté iniciado.':'No se pudo completar el ingreso. Inténtalo de nuevo.');
  }});
 }
}
