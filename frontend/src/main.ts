import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { inject, provideAppInitializer } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { App } from './app/app.component';
import { routes } from './app/app.routes';
import { AuthService } from './app/core/auth.service';
import { authInterceptor } from './app/core/security';
bootstrapApplication(App, {
  providers: [
    provideAppInitializer(() => firstValueFrom(inject(AuthService).restaurar())),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
  ],
}).catch(console.error);
