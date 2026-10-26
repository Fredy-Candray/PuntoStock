export type ResultadoCompartir = 'abierto' | 'cancelado' | 'no-disponible';

// El archivo se prepara antes del clic para conservar la activación del usuario.
export async function compartirComprobante(
  archivo: File,
  dispositivo: Pick<Navigator, 'share' | 'canShare'> = navigator,
): Promise<ResultadoCompartir> {
  if (typeof dispositivo.share !== 'function' || typeof dispositivo.canShare !== 'function')
    return 'no-disponible';
  try {
    if (!dispositivo.canShare({ files: [archivo] })) return 'no-disponible';
    await dispositivo.share({ files: [archivo], title: 'Comprobante de PuntoStock' });
    return 'abierto';
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') return 'cancelado';
    return 'no-disponible';
  }
}
