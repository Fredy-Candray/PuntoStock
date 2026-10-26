import '@angular/compiler';
class StoragePrueba {
  private datos = new Map<string, string>();
  getItem(k: string) {
    return this.datos.get(k) ?? null;
  }
  setItem(k: string, v: string) {
    this.datos.set(k, v);
  }
  removeItem(k: string) {
    this.datos.delete(k);
  }
}
Object.defineProperty(globalThis, 'localStorage', {
  value: new StoragePrueba(),
  configurable: true,
});
Object.defineProperty(globalThis, 'sessionStorage', {
  value: new StoragePrueba(),
  configurable: true,
});
