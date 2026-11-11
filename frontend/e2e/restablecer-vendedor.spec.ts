import { test, expect } from '@playwright/test';

test('admin restablece vendedor y el acceso temporal exige una contraseña personal', async ({
  page,
}) => {
  let cuenta: 'admin' | 'vendedor' | null = null;
  let pendiente = false;
  let personal = false;
  let reseteos = 0;
  let cambios = 0;
  let consultasRestringidas = 0;
  await page.route('**/api/**', async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname;
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const sesion = () => ({
      accessToken: 'qa-' + cuenta,
      username: cuenta,
      roles: [cuenta === 'admin' ? 'ADMIN' : 'VENDEDOR'],
      expiresIn: 900,
      cambioClavePendiente: cuenta === 'vendedor' && pendiente,
    });
    if (path === '/api/auth/login') {
      const body = req.postDataJSON();
      if (
        body.username === 'vendedor' &&
        body.password !== (personal ? 'Mi-clave-personal-2026' : 'puntostock')
      )
        return responder({}, 401);
      cuenta = body.username === 'admin' ? 'admin' : 'vendedor';
      return responder(sesion());
    }
    if (path === '/api/auth/renovar') return responder(cuenta ? sesion() : {}, cuenta ? 200 : 401);
    if (path === '/api/auth/salir') {
      cuenta = null;
      return route.fulfill({ status: 200, body: '' });
    }
    if (path === '/api/auth/cambiar-clave-inicial') {
      const body = req.postDataJSON();
      expect(body).toEqual({
        password: 'Mi-clave-personal-2026',
        confirmacion: 'Mi-clave-personal-2026',
      });
      cambios++;
      pendiente = false;
      personal = true;
      return route.fulfill({ status: 200, body: '' });
    }
    if (cuenta === 'vendedor' && pendiente) {
      consultasRestringidas++;
      return responder({}, 403);
    }
    if (path === '/api/auth/me')
      return responder({
        id: cuenta === 'admin' ? 1 : 2,
        username: cuenta,
        nombre: cuenta,
        roles: sesion().roles,
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
              id: 'usuarios',
              etiqueta: 'Usuarios',
              ruta: '/usuarios',
              icono: 'users',
              orden: 1,
              activo: true,
              roles: ['ADMIN'],
            },
            {
              id: 'pos',
              etiqueta: 'Punto de venta',
              ruta: '/pos',
              icono: 'cart',
              orden: 2,
              activo: true,
              roles: ['ADMIN', 'VENDEDOR'],
            },
          ],
        },
      ]);
    if (path === '/api/usuarios/pagina')
      return responder({
        contenido: [
          {
            id: 1,
            username: 'admin',
            nombre: 'Administrador',
            correo: 'qa@example.com',
            roles: ['ADMIN'],
            activo: true,
          },
          {
            id: 2,
            username: 'vendedor',
            nombre: 'Vendedor de prueba',
            correo: 'qa@example.com',
            roles: ['VENDEDOR'],
            activo: true,
          },
        ],
        cantidad: 2,
        pagina: 0,
        paginas: 1,
      });
    if (path === '/api/usuarios/2/restablecer-clave') {
      reseteos++;
      pendiente = true;
      return route.fulfill({ status: 200, body: '' });
    }
    if (path === '/api/productos') return responder([]);
    if (path === '/api/caja/estado') return responder({ abierta: true });
    return responder({}, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba-local');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
  await expect(
    page.getByRole('button', { name: 'Restablecer contraseña de Administrador', exact: true }),
  ).toHaveCount(0);
  const restablecer = page.getByRole('button', {
    name: 'Restablecer contraseña de Vendedor de prueba',
    exact: true,
  });
  await restablecer.click();
  const modal = page.getByRole('alertdialog', { name: 'Restablecer contraseña', exact: true });
  await expect(modal).toContainText('puntostock');
  await modal.getByRole('button', { name: 'Cancelar', exact: true }).click();
  expect(reseteos).toBe(0);
  await restablecer.click();
  await modal.getByRole('button', { name: 'Restablecer contraseña', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Contraseña restablecida');
  expect(reseteos).toBe(1);
  cuenta = null;
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('vendedor');
  await page.getByLabel('Contraseña', { exact: true }).fill('puntostock');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Crea tu contraseña personal', exact: true }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator('.clave-logo')).toHaveJSProperty('complete', true);
  expect(
    await page.locator('.clave-logo').evaluate((el) => (el as HTMLImageElement).naturalWidth),
  ).toBeGreaterThan(0);
  expect(
    await page.locator('.clave-inicial-panel').evaluate((el) => el.scrollWidth <= el.clientWidth),
  ).toBe(true);
  await page.screenshot({ path: '../.cache/reset-clave-movil.png', fullPage: true });
  await expect(page.locator('.side-nav')).toHaveCount(0);
  await page.goto('/pos');
  await expect(
    page.getByRole('heading', { name: 'Crea tu contraseña personal', exact: true }),
  ).toBeVisible();
  await page.getByLabel('Nueva contraseña', { exact: true }).fill('Mi-clave-personal-2026');
  await page.getByLabel('Confirmar nueva contraseña', { exact: true }).fill('Otra-clave-personal');
  await page.getByRole('button', { name: 'Guardar contraseña', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('deben coincidir');
  expect(cambios).toBe(0);
  await page
    .getByLabel('Confirmar nueva contraseña', { exact: true })
    .fill('Mi-clave-personal-2026');
  await page.getByRole('button', { name: 'Guardar contraseña', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Contraseña actualizada');
  expect(cambios).toBe(1);
  expect(consultasRestringidas).toBe(0);
  await page.getByRole('button', { name: 'Ir al inicio de sesión', exact: true }).click();
  await page.getByLabel('Usuario', { exact: true }).fill('vendedor');
  await page.getByLabel('Contraseña', { exact: true }).fill('Mi-clave-personal-2026');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Tu papelería, lista para vender.', exact: true }),
  ).toBeVisible();
});
