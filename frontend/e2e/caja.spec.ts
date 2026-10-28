import { test, expect } from '@playwright/test';

test('abrir, recuperar entrada incierta, retirar y cerrar con diferencia', async ({ page }) => {
  let sesion = false;
  let caja: Record<string, any> | null = null;
  const registros: Record<string, any>[] = [];
  const solicitudes = new Set<string>();
  let interrumpir = true;
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    const method = route.request().method();
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const acceso = { accessToken: 'qa-caja', username: 'admin', roles: ['ADMIN'], expiresIn: 900 };
    if (path === '/api/auth/login') {
      sesion = true;
      return responder(acceso);
    }
    if (path === '/api/auth/renovar') return responder(sesion ? acceso : {}, sesion ? 200 : 401);
    if (path === '/api/auth/me')
      return responder({
        id: 1,
        username: 'admin',
        nombre: 'Admin',
        roles: ['ADMIN'],
        activo: true,
      });
    if (path === '/api/auth/me/menu')
      return responder([
        {
          id: 'op',
          titulo: 'Operación',
          orden: 1,
          activo: true,
          opciones: [
            {
              id: 'caja',
              etiqueta: 'Caja',
              ruta: '/caja',
              icono: 'receipt',
              orden: 1,
              activo: true,
              roles: ['ADMIN'],
            },
          ],
        },
      ]);
    if (path === '/api/caja/estado')
      return responder({ abierta: !!caja && !caja.cierre, id: caja && !caja.cierre ? 1 : null });
    if (path === '/api/caja')
      return responder({
        contenido: caja ? [caja] : [],
        cantidad: caja ? 1 : 0,
        pagina: 0,
        paginas: caja ? 1 : 0,
      });
    if (path === '/api/caja/1') return responder(caja);
    if (path === '/api/caja/1/movimientos' && method === 'GET')
      return responder({ contenido: registros, cantidad: registros.length, pagina: 0, paginas: 1 });
    if (method === 'POST') {
      const inData = route.request().postDataJSON();
      if (path === '/api/caja/abrir') {
        caja = {
          id: 1,
          apertura: '2026-10-27T14:00:00Z',
          abiertoPor: 'admin',
          inicial: inData.inicial,
          notaApertura: inData.nota,
          cierre: null,
          cerradoPor: null,
          contado: null,
          diferencia: null,
          notaCierre: null,
          efectivo: 0,
          tarjeta: 0,
          transferencia: 0,
          ventas: 0,
          entradas: 0,
          salidas: 0,
          esperado: inData.inicial,
        };
        return responder(caja);
      }
      if (path === '/api/caja/1/movimientos' && caja) {
        if (!solicitudes.has(inData.solicitudId)) {
          solicitudes.add(inData.solicitudId);
          if (inData.tipo === 'ENTRADA') caja.entradas += inData.monto;
          else caja.salidas += inData.monto;
          caja.esperado = caja.inicial + caja.entradas - caja.salidas;
          registros.push({
            id: 'm-' + registros.length,
            fecha: '2026-10-27T14:15:00Z',
            usuario: 'admin',
            tipo: inData.tipo,
            motivo: inData.motivo,
            monto: inData.tipo === 'SALIDA' ? -inData.monto : inData.monto,
            metodoPago: 'EFECTIVO',
          });
          if (interrumpir) {
            interrumpir = false;
            return route.abort('failed');
          }
        }
        return responder(caja);
      }
      if (path === '/api/caja/1/cerrar' && caja) {
        caja = {
          ...caja,
          cierre: '2026-10-27T15:00:00Z',
          cerradoPor: 'admin',
          contado: inData.contado,
          diferencia: inData.contado - caja.esperado,
          notaCierre: inData.nota,
        };
        return responder(caja);
      }
    }
    return responder({ message: 'Ruta de prueba no prevista' }, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page.getByRole('link', { name: 'Caja', exact: true }).click();
  await page.getByLabel('Efectivo inicial · USD').fill('100');
  await page.getByRole('button', { name: 'Abrir caja', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Caja del día · 27/10/2026 · Abierta' }),
  ).toBeVisible();
  await expect(
    page.locator('.caja-historial').getByText('Referencia #1', { exact: true }),
  ).toBeVisible();
  const importe = page.getByLabel('Importe · USD', { exact: true });
  await expect(importe).toHaveValue('$0.00');
  await importe.pressSequentially('1');
  await expect(importe).toHaveValue('$0.01');
  await importe.pressSequentially('2');
  await expect(importe).toHaveValue('$0.12');
  await importe.pressSequentially('3');
  await expect(importe).toHaveValue('$1.23');
  await importe.press('Backspace');
  await expect(importe).toHaveValue('$0.12');
  await importe.fill('500');
  await expect(importe).toHaveValue('$5.00');
  await page.getByLabel('Motivo', { exact: true }).fill('Aporte para cambio');
  await page.getByRole('button', { name: 'Registrar movimiento', exact: true }).click();
  await expect(
    page.getByText('Operación pendiente de confirmación', { exact: true }),
  ).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Reintentar la misma solicitud' }).click();
  await expect(page.getByText('Movimiento registrado.', { exact: true })).toBeVisible();
  expect(registros).toHaveLength(1);
  await page.getByRole('combobox').selectOption('SALIDA');
  await page.getByLabel('Importe · USD', { exact: true }).fill('300');
  await page.getByLabel('Motivo', { exact: true }).fill('Transporte');
  await page.getByRole('button', { name: 'Registrar movimiento', exact: true }).click();
  await expect(page.getByText('Transporte', { exact: true })).toBeVisible();
  await page.screenshot({ path: '../.cache/caja-desktop.png', fullPage: true });
  await page.getByLabel('Efectivo contado · USD').fill('101');
  await expect(page.getByRole('button', { name: 'Confirmar cierre', exact: true })).toBeDisabled();
  await page.getByLabel(/Observación.*obligatoria/).fill('Falta un dólar en el conteo');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Confirmar cierre', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Caja del día · 27/10/2026 · Cerrada' }),
  ).toBeVisible();
  await expect(page.getByText('Falta un dólar en el conteo', { exact: true })).toBeVisible();
  expect(caja?.diferencia).toBe(-1);
  expect(registros).toHaveLength(2);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: '../.cache/caja-mobile.png', fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  );
});
