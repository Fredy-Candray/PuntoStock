import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly key = 'puntostock.theme';
  readonly dark = signal(false);
  constructor() {
    let saved: string | null = null;
    try {
      saved = this.document.defaultView?.localStorage.getItem(this.key) ?? null;
    } catch {}
    this.apply(saved === 'dark');
  }
  toggle() {
    this.apply(!this.dark());
    try {
      this.document.defaultView?.localStorage.setItem(this.key, this.dark() ? 'dark' : 'light');
    } catch {}
  }
  private apply(dark: boolean) {
    this.dark.set(dark);
    this.document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  }
}
