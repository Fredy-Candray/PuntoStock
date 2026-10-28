import { test, expect, Page } from '@playwright/test';

async function resumen(page: Page) {
  let sesion = false,
    cantidadAgotados = 8;
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const acceso = {
      accessToken: 'qa-resumen',
      username: 'admin',
      roles: ['ADMIN'],
      expiresIn: 900,
    };
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
              id: 'resumen',
              etiqueta: 'Resumen',
              ruta: '/dashboard',
              icono: 'grid',
              orden: 1,
              activo: true,
              roles: ['ADMIN'],
            },
          ],
        },
      ]);
    if (path === '/api/reportes/ganancia') return responder({ ganancia: 0 });
    if (path === '/api/inventario/movimientos') return responder([]);
    if (path === '/api/productos') {
      const base = {
        descripcion: 'Prueba de existencias',
        precioVenta: 1,
        precioCompra: 0.75,
        stockMinimo: 5,
        categoria: 'Papelería',
        unidad: 'UNIDAD',
        marca: '',
        ubicacion: '',
        fotoUrl: '/assets/producto.svg',
      };
      return responder([
        ...Array.from({ length: cantidadAgotados }, (_, i) => ({
          ...base,
          id: i + 1,
          nombre: 'Agotado ' + String(i + 1).padStart(2, '0'),
          sku: 'AG-' + String(i + 1).padStart(3, '0'),
          tipo: 'PRODUCTO',
          stock: 0,
        })),
        { ...base, id: 20, nombre: 'Bajo mínimo', sku: 'BAJO-01', tipo: 'PRODUCTO', stock: 2 },
        {
          ...base,
          id: 21,
          nombre: 'Servicio sin inventario',
          sku: 'SRV-01',
          tipo: 'SERVICIO',
          stock: 0,
        },
        { ...base, id: 22, nombre: 'Disponible', sku: 'DIS-01', tipo: 'PRODUCTO', stock: 10 },
      ]);
    }
    return responder({ message: 'Ruta de prueba no prevista' }, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page.getByRole('link', { name: 'Resumen', exact: true }).click();
  return {
    agotados: (n: number) => {
      cantidadAgotados = n;
    },
  };
}

test('agotados muestra solo stock cero, abre con teclado, pagina y restaura el foco', async ({
  page,
}) => {
  const s = await resumen(page);
  const tarjeta = page.getByRole('button', { name: /Artículos agotados/ });
  await expect(tarjeta.getByText('8', { exact: true })).toBeVisible();
  await tarjeta.focus();
  await page.keyboard.press('Enter');
  const dialogo = page.getByRole('dialog', { name: 'Artículos agotados', exact: true });
  await expect(dialogo).toBeVisible();
  await expect(dialogo.locator('tbody tr')).toHaveCount(6);
  await expect(dialogo.getByText('Bajo mínimo', { exact: true })).not.toBeVisible();
  await expect(dialogo.getByText('Servicio sin inventario', { exact: true })).not.toBeVisible();
  await expect(dialogo.getByText('Disponible', { exact: true })).not.toBeVisible();
  await expect(
    dialogo.getByRole('button', { name: 'Cerrar artículos agotados', exact: true }),
  ).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  expect(await page.evaluate(() => !!document.activeElement?.closest('[role="dialog"]'))).toBe(
    true,
  );
  await dialogo.getByRole('button', { name: 'Siguiente', exact: true }).click();
  await expect(dialogo.locator('tbody tr')).toHaveCount(2);
  await expect(dialogo.getByText('Agotado 08', { exact: true })).toBeVisible();
  await expect(dialogo.getByRole('button', { name: 'Siguiente', exact: true })).toBeDisabled();
  await page.screenshot({ path: '../.cache/agotados-escritorio.png', fullPage: true });
  await page.keyboard.press('Escape');
  await expect(dialogo).not.toBeVisible();
  await expect(tarjeta).toBeFocused();
  await tarjeta.click();
  await expect(dialogo.locator('tbody tr')).toHaveCount(6);
  await dialogo.getByRole('button', { name: 'Cerrar artículos agotados', exact: true }).click();
  await expect(dialogo).not.toBeVisible();
  s.agotados(0);
  await page.getByRole('button', { name: 'Actualizar resumen', exact: true }).click();
  await expect(tarjeta.getByText('0', { exact: true })).toBeVisible();
  await tarjeta.click();
  await expect(
    dialogo.getByRole('heading', { name: 'Sin artículos agotados', exact: true }),
  ).toBeVisible();
  await dialogo.getByRole('button', { name: 'Cerrar', exact: true }).click();
  await expect(dialogo).not.toBeVisible();
  await tarjeta.click();
  await page.locator('.admin-overlay').click({ position: { x: 5, y: 5 } });
  await expect(dialogo).not.toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  s.agotados(8);
  await page.getByRole('button', { name: 'Actualizar resumen', exact: true }).click();
  await expect(tarjeta.getByText('8', { exact: true })).toBeVisible();
  await tarjeta.click();
  await expect(dialogo.locator('tbody tr')).toHaveCount(6);
  const caja = await dialogo.boundingBox();
  expect(caja!.width).toBeLessThanOrEqual(390);
  await dialogo.screenshot({ path: '../.cache/agotados-modal-movil.png' });
});
