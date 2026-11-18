import { expect, test } from '@playwright/test';

test('muestra el encabezado del pedido al guardar desde un formulario desplazado', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1099, height: 625 });
  const pedido = {
    id: 21,
    cliente: 'Cliente de prueba',
    descripcion: 'Invitaciones de boda',
    servicios: 'Diseño y recorte',
    cantidad: 100,
    precioUnitario: 1.25,
    total: 125,
    saldo: 74.97,
    entrega: '2026-12-01',
    estado: 'PENDIENTE',
  };
  let cobrado = false;
  await page.route('**/api/**', async (route) => {
    const req = route.request();
    const path = new URL(req.url()).pathname;
    const responder = (json: unknown) => route.fulfill({ json });
    if (path === '/api/auth/renovar')
      return responder({ accessToken: 'qa', username: 'qa', roles: ['VENDEDOR'], expiresIn: 900 });
    if (path === '/api/auth/me')
      return responder({
        id: 2,
        username: 'qa',
        nombre: 'Vendedor',
        roles: ['VENDEDOR'],
        activo: true,
      });
    if (path === '/api/auth/me/menu')
      return responder([
        {
          id: 'pedidos',
          titulo: 'Pedidos',
          activo: true,
          orden: 1,
          opciones: [
            {
              id: 'pedidos',
              etiqueta: 'Pedidos personalizados',
              ruta: '/pedidos',
              icono: 'receipt',
              orden: 1,
              activo: true,
              roles: ['VENDEDOR'],
            },
          ],
        },
      ]);
    if (path === '/api/productos') return responder([]);
    if (path === '/api/pedidos/21' || path === '/api/pedidos/21/estado') {
      if (req.method() === 'PUT' && path.endsWith('/estado')) {
        expect(req.postDataJSON()).toEqual({
          estado: 'CANCELADO',
          ultimoAbono: 42,
          ultimoConsumo: 8,
        });
        pedido.estado = 'CANCELADO';
      } else if (req.method() === 'PUT') {
        expect(req.postDataJSON()).not.toHaveProperty('anticipo');
        pedido.cliente = req.postDataJSON().cliente;
      }
      return responder({
        pedido,
        materiales: [{ id: 8, nombre: 'Sobre', unidad: 'unidad', cantidad: 3 }],
        abonos: [{ id: 42, monto: 50.03, metodoPago: 'EFECTIVO' }],
      });
    }
    if (path === '/api/pedidos' && req.method() === 'GET') {
      if (cobrado) await new Promise((resolve) => setTimeout(resolve, 350));
      return responder({ contenido: cobrado ? [pedido] : [], pagina: 0, paginas: cobrado ? 1 : 0 });
    }
    if (path === '/api/pedidos' && req.method() === 'POST') {
      expect(req.postDataJSON()).toMatchObject({
        cliente: pedido.cliente,
        descripcion: pedido.descripcion,
        cantidad: 100,
        precioUnitario: 1.25,
        anticipo: { monto: 50.03, metodoPago: 'EFECTIVO' },
      });
      cobrado = true;
      return responder({
        pedido,
        materiales: [],
        abonos: [
          {
            id: 42,
            fecha: '2026-11-13T14:00:00Z',
            monto: 50.03,
            saldo: 74.97,
            metodoPago: 'EFECTIVO',
          },
        ],
      });
    }
    return route.fulfill({ status: 404, json: {} });
  });
  await page.goto('/pedidos');
  await page.getByRole('button', { name: 'Nuevo pedido' }).click();
  await expect(page.getByRole('heading', { name: 'Pedidos registrados' })).toHaveCount(0);
  await page.getByLabel('Cliente', { exact: true }).fill(pedido.cliente);
  await page.getByLabel('Fecha de entrega').fill(pedido.entrega);
  await page.getByLabel('Descripción del pedido').fill(pedido.descripcion);
  await page.getByLabel('Servicios incluidos').fill(pedido.servicios);
  await page.getByLabel('Cantidad', { exact: true }).fill('100');
  await page.getByLabel('Precio por invitación').fill('1.25');
  await page.getByLabel('Anticipo recibido').fill('50.03');
  await page.getByRole('button', { name: 'Confirmar pedido y anticipo' }).click();
  const titulo = page.getByRole('heading', { name: 'Pedido #21 · Cliente de prueba' });
  await expect(page.getByRole('heading', { name: 'Pedidos registrados' })).toHaveCount(0);
  await expect(titulo).toBeInViewport({ ratio: 1 });
  const barra = await page.locator('.workspace-header').boundingBox();
  expect(barra).not.toBeNull();
  expect((await titulo.boundingBox())!.y).toBeGreaterThanOrEqual(barra!.y + barra!.height);
  await expect(page.locator('dd').filter({ hasText: '$74.97' })).toBeVisible();
  await expect(page.locator('dt', { hasText: 'Anticipo inicial' }).locator('+ dd')).toHaveText(
    '$50.03',
  );
  expect(await page.locator('body').evaluate((body) => body.scrollWidth <= innerWidth)).toBe(true);
  const descarga = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Descargar PDF' }).click();
  expect((await descarga).suggestedFilename()).toBe('Abono-42-Pedido-21.pdf');
  await page.getByRole('button', { name: 'Volver al historial' }).click();
  await expect(page.getByRole('button', { name: 'Ver pedido' })).toBeVisible();
  await expect(titulo).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 740 });
  await page.getByRole('button', { name: 'Editar pedido' }).click();
  await expect(page.getByLabel('Anticipo recibido')).toHaveCount(0);
  await page.getByLabel('Cliente', { exact: true }).fill('Cliente corregido');
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
  await expect(page.getByRole('heading', { name: 'Pedido #21 · Cliente corregido' })).toBeVisible();
  await page.getByRole('button', { name: 'Volver al historial' }).click();
  await page.getByRole('button', { name: 'Cancelar pedido' }).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo).toContainText('$50.03');
  await expect(dialogo).toContainText('Sobre · 3 unidad');
  await dialogo.getByRole('button', { name: 'Volver' }).click();
  await expect(dialogo).toHaveCount(0);
  await page.getByRole('button', { name: 'Cancelar pedido' }).click();
  await dialogo.getByRole('button', { name: 'Confirmar cancelación' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Pedido cancelado.' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Registrar abono' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Volver al historial' }).click();
  await expect(page.getByRole('button', { name: 'Editar pedido' })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Cancelar pedido' })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Ver pedido' })).toBeEnabled();
});
