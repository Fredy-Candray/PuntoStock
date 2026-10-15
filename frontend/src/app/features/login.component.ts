import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/api.service';
@Component({standalone:true,imports:[FormsModule,MatButtonModule],template:`<h1>Ingresar a PuntoStock</h1><form (ngSubmit)="entrar()"><label>Usuario<input name="usuario" [(ngModel)]="username" required autocomplete="username"></label><label>Contraseña<input name="clave" type="password" [(ngModel)]="password" required autocomplete="current-password"></label><button mat-flat-button [disabled]="busy()">Ingresar</button></form><p class="error" role="alert">{{error()}}</p>`})
export class LoginComponent {
 auth=inject(AuthService);router=inject(Router);username='';password='';error=signal('');busy=signal(false);
 entrar(){if(this.busy())return;this.busy.set(true);this.auth.login(this.username,this.password).subscribe({next:r=>{this.auth.token.set(r.accessToken);this.auth.roles.set(r.roles);this.busy.set(false);this.password='';void this.router.navigateByUrl(this.auth.admin()?'/dashboard':'/pos');},error:()=>{this.busy.set(false);this.error.set('No se pudo ingresar. Verifique sus credenciales y la conexión.');}});}
}
