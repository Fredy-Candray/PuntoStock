import { Component, Input } from '@angular/core';
@Component({selector:'ps-icon',standalone:true,template:`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="paths[name] || paths['box']"/></svg>`})
export class IconComponent {
 @Input() name='box';
 readonly paths:Record<string,string>={
  printer:'M6 9V3h12v6M6 18H3V9h18v9h-3M6 14h12v7H6zM17 11h.01',
  receipt:'M5 3h14v19l-3-2-4 2-4-2-3 2V3ZM8 7h8M8 11h8M8 15h5',
  chevronDown:'m6 9 6 6 6-6',camera:'M3 7h4l2-3h6l2 3h4v14H3zM16 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  moon:'M20.9 13.2A9 9 0 0 1 10.8 3.1a9 9 0 1 0 10.1 10.1Z',
  sun:'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5 19 19M5 19l1.5-1.5M17.5 6.5 19 5',
  box:'M12 3 3 7.5v9L12 21l9-4.5v-9L12 3ZM3 7.5l9 4.5 9-4.5M12 12v9M7.5 5.25l9 4.5',
  grid:'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
  cart:'M2 3h3l2.5 12h11L21 7H6M9 20h.01M17 20h.01M8 18h10',
  history:'M3 11a9 9 0 1 1 2.6 7M3 4v7h7M12 7v5l3 2',
  chart:'M4 3v17h17M8 15l4-5 4 2 5-7',
  arrow:'M4 12h16M14 6l6 6-6 6',
  user:'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2',
  lock:'M6 10h12v11H6zM8 10V7a4 4 0 0 1 8 0v3M12 14v3',
  eye:'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
  eyeOff:'m3 3 18 18M10.6 5.1 12 5c6.5 0 10 7 10 7a19 19 0 0 1-3 4M6 6a20 20 0 0 0-4 6s3.5 7 10 7a13 13 0 0 0 5-1M10 10a3 3 0 0 0 4 4',
  shield:'M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6l-8-3Zm-4 9 3 3 5-6',
  logout:'M9 4H4v16h5M10 12h11M17 8l4 4-4 4',
  search:'M16 10a6 6 0 1 1-12 0 6 6 0 0 1 12 0Zm-2 4 7 7',
  plus:'M12 5v14M5 12h14',minus:'M5 12h14',close:'m6 6 12 12M6 18 18 6',
  edit:'m14 5 5 5M4 20l5-1L21 7a2.8 2.8 0 0 0-4-4L5 15l-1 5Z',
  trash:'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
  refresh:'M20 8a9 9 0 0 0-15-3L2 8m0-5v5h5M4 16a9 9 0 0 0 15 3l3-3m0 5v-5h-5',
  alert:'m12 3 10 18H2L12 3ZM12 9v5M12 17h.01',check:'m5 12 4 4L19 6',
  calendar:'M4 5h16v16H4zM8 3v4M16 3v4M4 10h16',money:'M12 2v20M17 6H9a3 3 0 0 0 0 6h6a3 3 0 0 1 0 6H6',
  arrowDown:'M12 4v16m-6-6 6 6 6-6',arrowUp:'M12 20V4m-6 6 6-6 6 6'
 };
}
