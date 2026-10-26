import { Component, inject } from '@angular/core';
import { IconComponent } from './icon.component';
import { ThemeService } from './theme.service';

@Component({
  selector: 'ps-theme-toggle',
  standalone: true,
  imports: [IconComponent],
  template: `
    <button
      class="theme-toggle"
      type="button"
      (click)="theme.toggle()"
      [attr.aria-label]="theme.dark() ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'"
      [attr.title]="theme.dark() ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'"
    >
      <ps-icon [name]="theme.dark() ? 'sun' : 'moon'" />
    </button>
  `,
})
export class ThemeToggleComponent {
  readonly theme = inject(ThemeService);
}
