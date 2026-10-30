import { test, expect } from '@playwright/test';

test('categorías del catálogo se crean desde el producto y conservan la selección', async ({
  page,
}) => {
  let sesion = false;
  const categorias = [{ id: 1, nombre: 'General', activo: true, version: 0 }];
  const enviados: { sku: string; existenciasIniciales: number }[] = [];
  await page.route('**/api/**', async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname;
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const acceso = {
      accessToken: 'qa-categorias',
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
              id: 'productos',
              etiqueta: 'Productos',
              ruta: '/productos',
              icono: 'box',
              orden: 1,
              activo: true,
              roles: ['ADMIN'],
            },
          ],
        },
      ]);
    if (path === '/api/productos/pagina')
      return responder({ contenido: [], cantidad: 0, pagina: 0, paginas: 0 });
    if (path === '/api/productos' && req.method() === 'POST') {
      const body = req.postDataJSON();
      enviados.push(body);
      return responder({ id: 48 + enviados.length, sku: body.sku || 'ART-REG-MIA-000049' });
    }
    if (path === '/api/productos/categorias') {
      if (req.method() === 'GET') return responder(categorias);
      const body = req.postDataJSON();
      if (categorias.some((c) => c.nombre.toLowerCase() === body.nombre.toLowerCase()))
        return responder({}, 409);
      const c = { id: categorias.length + 1, nombre: body.nombre, activo: body.activo, version: 0 };
      categorias.push(c);
      return responder(c);
    }
    if (path.startsWith('/api/productos/categorias/')) {
      const id = Number(path.split('/').at(-1)),
        index = categorias.findIndex((c) => c.id === id);
      categorias[index] = { ...req.postDataJSON(), id, version: categorias[index].version + 1 };
      return responder(categorias[index]);
    }
    return responder({}, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page.getByRole('link', { name: 'Productos', exact: true }).click();
  await page.getByRole('button', { name: 'Nuevo artículo', exact: true }).click();
  const selector = page.getByLabel('Categoría', { exact: true });
  await expect(selector).toHaveValue('General');
  await page.getByLabel('Nombre', { exact: true }).fill('Mi artículo pendiente');
  await page.getByRole('button', { name: 'Administrar categorías', exact: true }).click();
  const dialogo = page.getByRole('dialog', { name: 'Categorías de productos y servicios' });
  await dialogo.getByLabel('Nueva categoría', { exact: true }).fill('Regalos');
  await dialogo.getByRole('button', { name: 'Crear categoría', exact: true }).click();
  await expect(dialogo.getByText('Regalos', { exact: true })).toBeVisible();
  await dialogo.getByRole('button', { name: 'Cerrar categorías', exact: true }).click();
  await expect(selector).toHaveValue('Regalos');
  await expect(page.getByLabel('Nombre', { exact: true })).toHaveValue('Mi artículo pendiente');
  await page.getByRole('button', { name: 'Categorías', exact: true }).click();
  await dialogo.getByLabel('Nueva categoría', { exact: true }).fill('regalos');
  await dialogo.getByRole('button', { name: 'Crear categoría', exact: true }).click();
  await expect(dialogo.getByRole('alert')).toContainText('Ya existe');
  await dialogo.getByRole('button', { name: 'Editar categoría Regalos', exact: true }).click();
  await dialogo.getByLabel('Editar categoría', { exact: true }).fill('Regalos especiales');
  await dialogo.getByRole('button', { name: 'Guardar categoría', exact: true }).click();
  await expect(dialogo.getByText('Regalos especiales', { exact: true })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(dialogo).not.toBeVisible();
  await expect(selector).toHaveValue('Regalos especiales');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: 'Categorías', exact: true }).click();
  await expect(dialogo).toBeVisible();
  expect((await dialogo.boundingBox())!.width).toBeLessThanOrEqual(390);
  await dialogo.screenshot({ path: '../.cache/categorias-productos-movil.png' });
  await dialogo.getByRole('button', { name: 'Cerrar categorías', exact: true }).click();
  const auto = page.getByLabel('Generar código automáticamente', { exact: true });
  const codigo = page.getByLabel('Código SKU / código de barras', { exact: true });
  await expect(auto).toBeChecked();
  await expect(codigo).toBeDisabled();
  await page.getByLabel('Descripción', { exact: true }).fill('Artículo de prueba');
  await page.getByLabel('Precio de compra por unidad · USD', { exact: true }).fill('1');
  await page.getByLabel('Precio de venta por unidad · USD', { exact: true }).fill('2');
  await page.getByRole('spinbutton', { name: /^Existencias iniciales/ }).fill('10');
  await page.getByRole('button', { name: 'Crear', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('ART-REG-MIA-000049');
  expect(enviados[0].sku).toBe('');
  expect(enviados[0].existenciasIniciales).toBe(10);
  await page.getByRole('button', { name: 'Nuevo artículo', exact: true }).click();
  await auto.uncheck();
  await expect(codigo).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Crear', exact: true })).toBeDisabled();
  await codigo.fill('7501234567890');
  await page.getByLabel('Nombre', { exact: true }).fill('Producto con barras');
  await page.getByLabel('Descripción', { exact: true }).fill('Prueba manual');
  await page.getByLabel('Precio de venta por unidad · USD', { exact: true }).fill('2');
  await page.getByRole('button', { name: 'Crear', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('7501234567890');
  expect(enviados[1].sku).toBe('7501234567890');
});
