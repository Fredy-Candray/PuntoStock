import { test, expect } from '@playwright/test';

test('selección local muestra vista previa, persiste y permite quitar la imagen', async ({
  page,
}) => {
  let sesion = false,
    foto = '',
    producto: Record<string, unknown> | null = null;
  const entradas: Record<string, unknown>[] = [];
  let peticionesImagen = 0;
  await page.route('**/api/**', async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname;
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const acceso = {
      accessToken: 'qa-producto-imagen',
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
            {
              id: 'pos',
              etiqueta: 'Punto de venta',
              ruta: '/pos',
              icono: 'cart',
              orden: 2,
              activo: true,
              roles: ['ADMIN'],
            },
          ],
        },
      ]);
    if (path === '/api/caja/estado') return responder({ abierta: true, id: 1 });
    if (path === '/api/productos/categorias')
      return responder([{ id: 1, nombre: 'General', activo: true, version: 0 }]);
    if (path === '/api/productos/pagina')
      return responder({
        contenido: producto ? [producto] : [],
        cantidad: producto ? 1 : 0,
        pagina: 0,
        paginas: 1,
      });
    if (
      (path === '/api/productos' && req.method() === 'POST') ||
      (path === '/api/productos/1' && req.method() === 'PUT')
    ) {
      const body = req.postDataJSON();
      entradas.push(body);
      if (body.imagen) foto = body.imagen;
      if (body.quitarImagen) foto = '';
      producto = {
        ...body,
        id: 1,
        sku: 'ART-GEN-IMA-000001',
        stock: 4,
        fotoUrl: foto ? '/api/productos/1/imagen?v=qa' : '/assets/producto.svg',
      };
      delete producto['imagen'];
      delete producto['quitarImagen'];
      return responder({ id: 1, sku: producto['sku'] });
    }
    if (path === '/api/productos/1/imagen') {
      expect(req.headers()['authorization']).toBe('Bearer qa-producto-imagen');
      peticionesImagen++;
      return foto
        ? route.fulfill({
            status: 200,
            contentType: 'image/png',
            body: Buffer.from(foto.split(',')[1], 'base64'),
          })
        : responder({}, 404);
    }
    if (path === '/api/productos') return responder(producto ? [producto] : []);
    return responder({}, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page.getByRole('link', { name: 'Productos', exact: true }).click();
  await page.getByRole('button', { name: 'Nuevo artículo', exact: true }).click();
  const archivo = page.locator('input[type="file"]');
  await archivo.setInputFiles({
    name: 'texto.svg',
    mimeType: 'image/svg+xml',
    buffer: Buffer.from('<svg/>'),
  });
  await expect(page.getByRole('alert')).toContainText('JPG, PNG o WebP');
  await expect(page.getByRole('button', { name: 'Crear', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Quitar imagen', exact: true }).click();
  const png = await page.evaluate(() => {
    const c = document.createElement('canvas');
    c.width = 8;
    c.height = 8;
    const ctx = c.getContext('2d')!;
    ctx.fillStyle = '#087f76';
    ctx.fillRect(0, 0, 8, 8);
    return c.toDataURL('image/png');
  });
  await archivo.setInputFiles({
    name: 'producto.png',
    mimeType: 'image/png',
    buffer: Buffer.from(png.split(',')[1], 'base64'),
  });
  await expect(
    page.getByRole('img', { name: 'Vista previa del producto', exact: true }),
  ).toBeVisible();
  await page.getByLabel('Nombre', { exact: true }).fill('Imagen local');
  await page.getByLabel('Descripción', { exact: true }).fill('Prueba de imagen');
  await page.getByLabel('Precio de venta por unidad · USD', { exact: true }).fill('2');
  await page.getByLabel('Existencias iniciales', { exact: true }).fill('4');
  await page.getByRole('button', { name: 'Crear', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Producto guardado.');
  expect(entradas[0]['imagen']).toMatch(/^data:image\/png;base64,/);
  await expect(
    page.getByRole('img', { name: 'Imagen de Imagen local', exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole('img', { name: 'Imagen de Imagen local', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: 'Punto de venta', exact: true }).click();
  await expect(
    page.getByRole('img', { name: 'Imagen de Imagen local', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: 'Productos', exact: true }).click();
  await page.getByRole('button', { name: 'Editar Imagen local', exact: true }).click();
  await expect(
    page.getByRole('img', { name: 'Vista previa del producto', exact: true }),
  ).toBeVisible();
  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'dark'));
  const campo = page.locator('.producto-imagen-campo');
  const ayuda = page.locator('#producto-imagen-ayuda');
  const acciones = page.locator('.producto-imagen-acciones');
  const fila = await acciones.boundingBox(),
    texto = await ayuda.boundingBox();
  expect(texto!.y).toBeGreaterThanOrEqual(fila!.y + fila!.height + 8);
  await expect(campo.getByRole('button', { name: 'Elegir imagen', exact: true })).toBeVisible();
  await expect(campo.getByRole('button', { name: 'Cancelar selección', exact: true })).toHaveCount(
    0,
  );
  await campo.screenshot({ path: '../.cache/producto-imagen-escritorio.png' });
  await page.setViewportSize({ width: 390, height: 844 });
  const filaMovil = await acciones.boundingBox(),
    textoMovil = await ayuda.boundingBox();
  expect(textoMovil!.y).toBeGreaterThanOrEqual(filaMovil!.y + filaMovil!.height + 8);
  await page
    .locator('.producto-imagen-campo')
    .screenshot({ path: '../.cache/producto-imagen-movil.png' });
  await page.getByRole('button', { name: 'Quitar imagen', exact: true }).click();
  await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Producto guardado.');
  expect(entradas[1]['quitarImagen']).toBe(true);
  expect(entradas[1]['imagen']).toBeUndefined();
  await expect(page.getByRole('img', { name: 'Imagen de Imagen local', exact: true })).toHaveCount(
    0,
  );
  expect(peticionesImagen).toBeGreaterThanOrEqual(4);
});
