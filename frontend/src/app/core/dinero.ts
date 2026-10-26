export function totalCentavos(
  lineas: readonly { producto: { precioVenta: number }; cantidad: number }[],
): number {
  return lineas.reduce(
    (total, linea) => total + Math.round(linea.producto.precioVenta * 100) * linea.cantidad,
    0,
  );
}
