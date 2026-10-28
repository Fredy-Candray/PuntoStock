import { describe, it, expect, vi } from 'vitest';
import { ElementRef, Injector } from '@angular/core';
import { ImporteCentavosDirective } from '../src/app/core/importe-centavos.directive';

function campo() {
  const input = {
    value: '',
    disabled: false,
    selectionStart: 0,
    selectionEnd: 0,
    setSelectionRange: vi.fn(),
  };
  const control = Injector.create({
    providers: [
      ImporteCentavosDirective,
      { provide: ElementRef, useValue: { nativeElement: input } },
    ],
  }).get(ImporteCentavosDirective);
  const cambiar = vi.fn();
  control.registerOnChange(cambiar);
  return { input, control, cambiar };
}
describe('importe escrito por centavos', () => {
  it('escribe y borra al final para que el primer dígito no se convierta en dólares', () => {
    const { input, control } = campo();
    control.writeValue(null);
    const evento = {
      key: '1',
      ctrlKey: false,
      metaKey: false,
      altKey: false,
      preventDefault: vi.fn(),
    } as unknown as KeyboardEvent;
    control.filtrarTecla(evento);
    expect(input.setSelectionRange).toHaveBeenLastCalledWith(5, 5);
    control.filtrarTecla({ ...evento, key: '.' } as KeyboardEvent);
    expect(evento.preventDefault).toHaveBeenCalledOnce();
  });
  it('presenta cero y permite escribir 1, 12 y 123 como centavos', () => {
    const { input, control, cambiar } = campo();
    control.writeValue(null);
    expect(input.value).toBe('$0.00');
    expect(cambiar).not.toHaveBeenCalled();
    for (const [tecla, valor, texto] of [
      ['1', 0.01, '$0.01'],
      ['2', 0.12, '$0.12'],
      ['3', 1.23, '$1.23'],
    ] as const) {
      input.value += tecla;
      control.actualizar();
      expect(cambiar).toHaveBeenLastCalledWith(valor);
      expect(input.value).toBe(texto);
    }
  });
  it('borra un dígito y vuelve a cero al limpiar todo el campo', () => {
    const { input, control, cambiar } = campo();
    control.writeValue(1.23);
    input.value = input.value.slice(0, -1);
    control.actualizar();
    expect(input.value).toBe('$0.12');
    expect(cambiar).toHaveBeenLastCalledWith(0.12);
    input.value = '';
    control.actualizar();
    expect(input.value).toBe('$0.00');
    expect(cambiar).toHaveBeenLastCalledWith(0);
  });
  it('admite importes pegados y separadores sin perder el valor monetario', () => {
    const { input, control, cambiar } = campo();
    input.value = '$1,234.56';
    control.actualizar();
    expect(cambiar).toHaveBeenLastCalledWith(1234.56);
    expect(input.value).toBe('$1,234.56');
    input.value = '1500';
    control.actualizar();
    expect(cambiar).toHaveBeenLastCalledWith(15);
    expect(input.value).toBe('$15.00');
  });
  it('rechaza texto, negativos y enteros que exceden la precisión segura', () => {
    const { input, control, cambiar } = campo();
    for (const texto of ['-150', 'importe 123', '99999999999999999999']) {
      input.value = texto;
      control.actualizar();
      expect(cambiar).toHaveBeenLastCalledWith(null);
    }
  });
  it('respeta la desactivación y notifica al salir del campo', () => {
    const { input, control } = campo();
    const tocar = vi.fn();
    control.registerOnTouched(tocar);
    control.setDisabledState(true);
    expect(input.disabled).toBe(true);
    control.setDisabledState(false);
    expect(input.disabled).toBe(false);
    control.tocar();
    expect(tocar).toHaveBeenCalledOnce();
  });
});
