import { test, expect, Page } from '@playwright/test';
async function sistema(page: Page, modo: 'normal' | 'incierto' | 'precio' = 'normal') {
  let sesion = false,
    ventas = 0,
    ticket: unknown = null,
    precio = 4;
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const acceso = {
      accessToken: 'token-e2e',
      expiresIn: 900,
      roles: ['VENDEDOR'],
      username: 'ana',
    };
    if (path === '/api/auth/login') {
      sesion = true;
      return responder(acceso);
    }
    if (path === '/api/auth/renovar')
      return responder(sesion ? acceso : { message: 'Sin sesión' }, sesion ? 200 : 401);
    if (path === '/api/auth/salir') {
      sesion = false;
      return responder({});
    }
    if (path === '/api/auth/me')
      return responder({
        id: 1,
        username: 'ana',
        nombre: 'Ana',
        roles: ['VENDEDOR'],
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
              id: 'pos',
              etiqueta: 'Punto de venta',
              ruta: '/pos',
              icono: 'cart',
              activo: true,
              orden: 1,
              roles: ['VENDEDOR'],
            },
          ],
        },
      ]);
    if (path === '/api/productos')
      return responder([
        {
          id: 1,
          sku: 'CUA-01',
          nombre: 'Cuaderno',
          descripcion: 'Escolar',
          precioVenta: precio,
          stock: 5,
          fotoUrl: '/assets/producto.svg',
          tipo: 'PRODUCTO',
          categoria: 'Papelería',
          marca: '',
          unidad: 'UNIDAD',
          ubicacion: '',
          stockMinimo: 1,
        },
      ]);
    if (path.startsWith('/api/ventas/solicitud/'))
      return responder(ticket ?? { message: 'No encontrado' }, ticket ? 200 : 404);
    if (path === '/api/ventas' && route.request().method() === 'POST') {
      ventas++;
      if (modo === 'precio' && ventas === 1) {
        precio = 5;
        return responder(
          { message: 'Los precios cambiaron. Revisa el total actualizado antes de cobrar.' },
          409,
        );
      }
      ticket = {
        id: 17,
        total: precio,
        fecha: '2026-10-26T12:00:00Z',
        vendedor: 'ana',
        metodoPago: 'EFECTIVO',
        recibido: precio,
        cambio: 0,
        cliente: '',
        referenciaPago: '',
        lineas: [
          {
            productoId: 1,
            sku: 'CUA-01',
            nombre: 'Cuaderno',
            cantidad: 1,
            precioUnitario: precio,
            subtotal: precio,
            observaciones: '',
          },
        ],
      };
      if (modo === 'incierto') return route.abort('failed');
      return responder(ticket);
    }
    return responder({ message: 'Ruta de prueba no prevista' }, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('ana');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Tu papelería, lista para vender.' }),
  ).toBeVisible();
  return { ventas: () => ventas };
}
test('login y cobro muestran comprobante', async ({ page }) => {
  const s = await sistema(page);
  await page.getByRole('button', { name: 'Agregar Cuaderno' }).click();
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar cobro' }).click();
  await expect(page.getByText('Venta confirmada #17')).toBeVisible();
  expect(s.ventas()).toBe(1);
});
test('respuesta perdida conserva UUID y recupera la venta tras recargar', async ({ page }) => {
  const s = await sistema(page, 'incierto');
  await page.getByRole('button', { name: 'Agregar Cuaderno' }).click();
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar cobro' }).click();
  await expect(page.getByText('Venta pendiente de confirmación', { exact: true })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Consultar estado' }).click();
  await expect(page.getByText('Venta confirmada #17')).toBeVisible();
  expect(s.ventas()).toBe(1);
});
test('un cambio de precio exige confirmar de nuevo', async ({ page }) => {
  const s = await sistema(page, 'precio');
  await page.getByRole('button', { name: 'Agregar Cuaderno' }).click();
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar cobro' }).click();
  await expect(page.getByText(/Los precios cambiaron/)).toBeVisible();
  expect(s.ventas()).toBe(1);
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar cobro' }).click();
  await expect(page.getByText('Venta confirmada #17')).toBeVisible();
  expect(s.ventas()).toBe(2);
});
