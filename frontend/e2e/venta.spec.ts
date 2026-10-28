import { test, expect, Page } from '@playwright/test';
async function sistema(
  page: Page,
  modo: 'normal' | 'incierto' | 'precio' | 'caja-cerrada' | 'paginacion' = 'normal',
) {
  let sesion = false,
    ventas = 0,
    ticket: unknown = null,
    precio = 4,
    cantidadProductos = 14;
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
    if (path === '/api/caja/estado')
      return responder({
        abierta: modo !== 'caja-cerrada',
        id: modo === 'caja-cerrada' ? null : 1,
      });
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
    if (path === '/api/productos' && modo === 'paginacion')
      return responder(
        Array.from({ length: cantidadProductos }, (_, i) => ({
          id: i + 1,
          sku: 'COD-' + String(i + 1).padStart(2, '0'),
          nombre: 'Artículo ' + String(i + 1).padStart(2, '0'),
          descripcion: 'Catálogo de prueba',
          precioVenta: 4,
          stock: i >= 12 ? 0 : 20,
          fotoUrl: '/assets/producto.svg',
          tipo: i >= 12 ? 'SERVICIO' : 'PRODUCTO',
          categoria: i >= 12 ? 'Servicios' : 'Papelería',
          marca: '',
          unidad: i >= 12 ? 'PAGINA' : 'UNIDAD',
          ubicacion: '',
          stockMinimo: 1,
        })),
      );
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
  return {
    ventas: () => ventas,
    limitarCatalogo: (n: number) => {
      cantidadProductos = n;
    },
  };
}
test('login y cobro muestran comprobante', async ({ page }) => {
  const s = await sistema(page);
  await page.getByRole('button', { name: 'Agregar Cuaderno' }).click();
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar cobro' }).click();
  await expect(page.getByText('Venta confirmada #17')).toBeVisible();
  expect(s.ventas()).toBe(1);
});
test('el aviso de código inexistente se quita al buscar o agregar un artículo válido', async ({
  page,
}) => {
  await sistema(page);
  const buscador = page.getByLabel('Buscar o escanear código');
  const aviso = page.getByText('No se encontró un artículo o servicio con ese código exacto.', {
    exact: true,
  });
  await buscador.fill('no-existe');
  await buscador.press('Enter');
  await expect(aviso).toBeVisible();
  await buscador.fill('Cua');
  await expect(aviso).not.toBeVisible();
  await expect(page.getByRole('button', { name: 'Agregar Cuaderno', exact: true })).toBeVisible();
  await buscador.press('Enter');
  await expect(aviso).toBeVisible();
  await page.getByRole('button', { name: 'Agregar Cuaderno', exact: true }).click();
  await expect(aviso).not.toBeVisible();
  await expect(page.getByRole('button', { name: 'Quitar Cuaderno', exact: true })).toBeVisible();
  await buscador.fill('CUA-01');
  await buscador.press('Enter');
  await expect(
    page.getByRole('spinbutton', { name: 'Cantidad de Cuaderno', exact: true }),
  ).toHaveValue('2');
  await expect(aviso).not.toBeVisible();
  // Un aviso de stock sigue vigente al cambiar de búsqueda.
  for (let i = 0; i < 4; i++) {
    await buscador.fill('CUA-01');
    await buscador.press('Enter');
  }
  await expect(
    page.getByText('No hay existencias suficientes de Cuaderno.', { exact: true }),
  ).toBeVisible();
  await buscador.fill('Cuaderno');
  await expect(
    page.getByText('No hay existencias suficientes de Cuaderno.', { exact: true }),
  ).toBeVisible();
});
test('caja cerrada permite preparar carrito pero no confirmar cobro', async ({ page }) => {
  const s = await sistema(page, 'caja-cerrada');
  await expect(page.getByText(/La caja está cerrada/)).toBeVisible();
  await page.getByRole('button', { name: 'Agregar Cuaderno' }).click();
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Confirmar cobro' })).toBeDisabled();
  expect(s.ventas()).toBe(0);
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

test('catálogo de seis por página conserva carrito, filtros y escaneo global', async ({ page }) => {
  const s = await sistema(page, 'paginacion');
  const nav = page.getByRole('navigation', { name: 'Paginación del catálogo' });
  const filas = page.locator('.pos-table tbody tr');
  await expect(filas).toHaveCount(6);
  await expect(nav).toContainText('Página 1 de 3');
  await expect(nav.getByRole('button', { name: 'Anterior' })).toBeDisabled();
  await page.getByRole('button', { name: 'Agregar Artículo 01', exact: true }).click();
  await nav.getByRole('button', { name: 'Siguiente' }).click();
  await expect(nav).toContainText('Página 2 de 3');
  await expect(filas).toHaveCount(6);
  await expect(
    page.getByRole('button', { name: 'Agregar Artículo 07', exact: true }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Quitar Artículo 01', exact: true })).toBeVisible();
  await nav.getByRole('button', { name: 'Siguiente' }).click();
  await expect(filas).toHaveCount(2);
  await expect(nav.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  await page.getByLabel('Tipo de catálogo').selectOption('SERVICIO');
  await expect(nav).toContainText('Página 1 de 1');
  await expect(filas).toHaveCount(2);
  await page.getByLabel('Categoría del catálogo').selectOption('Servicios');
  await page.getByLabel('Buscar o escanear código').fill('Artículo 14');
  await expect(filas).toHaveCount(1);
  await expect(
    page.getByRole('button', { name: 'Agregar Artículo 14', exact: true }),
  ).toBeVisible();
  await page.getByLabel('Buscar o escanear código').fill('');
  await page.getByLabel('Categoría del catálogo').selectOption('');
  await page.getByLabel('Tipo de catálogo').selectOption('TODOS');
  await expect(filas).toHaveCount(6);
  await nav.getByRole('button', { name: 'Siguiente' }).click();
  await nav.getByRole('button', { name: 'Siguiente' }).click();
  s.limitarCatalogo(2);
  await page.getByRole('button', { name: 'Actualizar', exact: true }).click();
  await expect(nav).toContainText('Página 1 de 1');
  await expect(filas).toHaveCount(2);
  s.limitarCatalogo(14);
  await page.getByRole('button', { name: 'Actualizar', exact: true }).click();
  await expect(filas).toHaveCount(6);
  await page.getByLabel('Buscar o escanear código').fill('COD-14');
  await page.getByLabel('Buscar o escanear código').press('Enter');
  await expect(page.getByRole('button', { name: 'Quitar Artículo 14', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Quitar Artículo 01', exact: true })).toBeVisible();
  await expect(nav).toContainText('Página 1 de 3');
  await page.getByLabel('Buscar o escanear código').fill('no-existe-en-catalogo');
  await expect(page.getByRole('heading', { name: 'No hay coincidencias' })).toBeVisible();
  await expect(nav.getByRole('button', { name: 'Anterior' })).toBeDisabled();
  await expect(nav.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  expect(s.ventas()).toBe(0);
});
