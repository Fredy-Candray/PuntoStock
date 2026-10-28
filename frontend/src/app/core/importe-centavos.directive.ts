import { Directive, ElementRef, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Directive({
  selector: 'input[psImporteCentavos]',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ImporteCentavosDirective),
      multi: true,
    },
  ],
  host: {
    inputmode: 'numeric',
    autocomplete: 'off',
    '(input)': 'actualizar()',
    '(blur)': 'tocar()',
    '(keydown)': 'filtrarTecla($event)',
  },
})
export class ImporteCentavosDirective implements ControlValueAccessor {
  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private readonly formato = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  private cambiar: (valor: number | null) => void = () => {};
  private tocado: () => void = () => {};

  writeValue(valor: number | null): void {
    this.elemento.nativeElement.value = this.formato.format(
      valor !== null && Number.isFinite(valor) ? valor : 0,
    );
  }
  registerOnChange(fn: (valor: number | null) => void): void {
    this.cambiar = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.tocado = fn;
  }
  setDisabledState(disabled: boolean): void {
    this.elemento.nativeElement.disabled = disabled;
  }
  actualizar(): void {
    const input = this.elemento.nativeElement;
    const texto = input.value;
    const centavos = Number(texto.replace(/\D/g, '') || '0');
    if (!/^[\d$,.\s]*$/.test(texto) || !Number.isSafeInteger(centavos)) {
      this.cambiar(null);
      return;
    }
    const valor = centavos / 100;
    this.writeValue(valor);
    input.setSelectionRange(input.value.length, input.value.length);
    this.cambiar(valor);
  }
  tocar(): void {
    this.tocado();
  }
  filtrarTecla(event: KeyboardEvent): void {
    if (event.ctrlKey || event.metaKey || event.altKey) return;
    const input = this.elemento.nativeElement;
    if (/^\d$/.test(event.key) || event.key === 'Backspace') {
      // La captura es por centavos, incluso si el campo recibió el foco al inicio.
      if (input.selectionStart === input.selectionEnd)
        input.setSelectionRange(input.value.length, input.value.length);
      return;
    }
    if (event.key.length === 1) event.preventDefault();
  }
}
