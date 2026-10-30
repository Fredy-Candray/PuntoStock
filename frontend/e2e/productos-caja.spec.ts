import { test, expect } from '@playwright/test';

test('compra cajas, vende caja y piezas juntas y registra entradas por caja', async ({ page }) => {
  let sesion = false;
  let producto: Record<string, unknown> | null = null;
  let alta: Record<string, unknown> | null = null;
  let venta: {
    lineas: {
      productoId: number;
      cantidad: number;
      presentacion?: string;
      unidadesPorPresentacion?: number;
    }[];
    totalEsperado: number;
  } | null = null;
  let movimiento: Record<string, unknown> | null = null;
  await page.route('**/api/**', async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname;
    const responder = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    const acceso = {
      accessToken: 'qa-caja-producto',
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
            {
              id: 'inventario',
              etiqueta: 'Inventario',
              ruta: '/inventario',
              icono: 'history',
              orden: 3,
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
        contenido: producto
          ? [
              producto,
              ...Array.from({ length: 5 }, (_, i) => ({
                ...producto,
                id: i + 2,
                nombre: 'Artículo móvil ' + (i + 2),
              })),
            ]
          : [],
        cantidad: producto ? 6 : 0,
        pagina: 0,
        paginas: 1,
      });
    if (path === '/api/productos' && req.method() === 'POST') {
      const body = req.postDataJSON();
      alta = body;
      producto = {
        ...body,
        id: 1,
        sku: 'ART-GEN-LAP-000001',
        stock: 27,
        precioCompra: 0.5,
        caja: { unidades: 12, precioCompra: 6, precioVenta: 8 },
        fotoUrl: '/assets/producto.svg',
      };
      return responder({ id: 1, sku: producto['sku'] });
    }
    if (path === '/api/productos') return responder(producto ? [producto] : []);
    if (path === '/api/inventario/movimientos/pagina')
      return responder({ contenido: [], cantidad: 0, pagina: 0, paginas: 0 });
    if (path === '/api/inventario/movimientos' && req.method() === 'POST') {
      movimiento = req.postDataJSON();
      if (producto) producto['stock'] = Number(producto['stock']) + 24;
      return responder({});
    }
    if (path === '/api/ventas' && req.method() === 'POST') {
      const body = req.postDataJSON();
      venta = body;
      if (producto) producto['stock'] = 12;
      return responder({
        id: 17,
        total: 11,
        fecha: '2026-10-29T12:00:00Z',
        vendedor: 'admin',
        metodoPago: 'EFECTIVO',
        recibido: 11,
        cambio: 0,
        cliente: '',
        referenciaPago: '',
        lineas: body.lineas.map((l: { cantidad: number; presentacion?: string }) => ({
          productoId: 1,
          sku: 'ART-GEN-LAP-000001',
          nombre: 'Lápiz',
          cantidad: l.cantidad,
          precioUnitario: l.presentacion === 'CAJA' ? 8 : 1,
          subtotal: l.cantidad * (l.presentacion === 'CAJA' ? 8 : 1),
          presentacion: l.presentacion ?? 'UNIDAD',
          unidadesPorPresentacion: l.presentacion === 'CAJA' ? 12 : 1,
          observaciones: '',
        })),
      });
    }
    return responder({}, 404);
  });
  await page.goto('/login');
  await page.getByLabel('Usuario', { exact: true }).fill('admin');
  await page.getByLabel('Contraseña', { exact: true }).fill('solo-prueba');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page.getByRole('link', { name: 'Productos', exact: true }).click();
  await page.getByRole('button', { name: 'Nuevo artículo', exact: true }).click();
  await page.getByLabel('Nombre', { exact: true }).fill('Lápiz');
  await page.getByLabel('Descripción', { exact: true }).fill('Escolar');
  await page.getByLabel('Unidad de venta', { exact: true }).selectOption('CAJA');
  await expect(page.getByLabel('Unidad de venta', { exact: true })).toHaveValue('UNIDAD');
  await page.getByLabel('Unidades por caja', { exact: true }).fill('12');
  await page.getByLabel('Precio de compra de la caja · USD', { exact: true }).fill('6');
  await expect(page.getByLabel('Precio de compra por unidad · USD', { exact: true })).toHaveValue(
    '0.5',
  );
  await page.getByLabel('Permitir también la venta de caja completa', { exact: true }).check();
  await page.getByLabel('Precio de venta de la caja · USD', { exact: true }).fill('8');
  await page.getByLabel('Precio de venta por unidad · USD', { exact: true }).fill('1');
  await page.getByLabel('Cajas iniciales', { exact: true }).fill('2');
  await page.getByLabel('Unidades sueltas iniciales', { exact: true }).fill('3');
  await expect(
    page.getByText('Total que se registrará: 27 unidades individuales.', { exact: true }),
  ).toBeVisible();
  await page.screenshot({ path: '../.cache/producto-caja-formulario.png', fullPage: true });
  await page.getByRole('button', { name: 'Crear', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Producto guardado.');
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator('.workspace-header')).toBeHidden();
  await expect(page.locator('.mobile-user-menu')).toBeVisible();
  const logoMovil = await page.locator('.sidebar-brand').boundingBox();
  const usuarioMovil = await page.locator('.mobile-user-menu').boundingBox();
  expect(Math.abs(logoMovil!.y - usuarioMovil!.y)).toBeLessThan(30);
  expect(usuarioMovil!.x + usuarioMovil!.width).toBeLessThanOrEqual(390);
  const verMovil = page.getByRole('button', { name: 'Ver Lápiz', exact: true });
  await expect(verMovil).toBeVisible();
  const accionesMovil = await verMovil.boundingBox();
  expect(accionesMovil!.x + accionesMovil!.width).toBeLessThanOrEqual(390);
  const ultimaAccion = page.getByRole('button', { name: 'Ver Artículo móvil 6', exact: true });
  await ultimaAccion.scrollIntoViewIfNeeded();
  await expect(ultimaAccion).toBeInViewport();
  expect(await page.evaluate(() => window.scrollY)).toBeGreaterThan(0);
  await verMovil.scrollIntoViewIfNeeded();
  expect(
    await page.locator('.responsive-table').evaluate((el) => el.scrollWidth <= el.clientWidth),
  ).toBe(true);
  await page.getByRole('button', { name: 'Ver Lápiz', exact: true }).click();
  const detalle = page.getByRole('dialog', { name: 'Información del artículo' });
  await expect(detalle).toBeVisible();
  const columnas = await detalle
    .locator('.producto-detalle-datos')
    .evaluate((el) => getComputedStyle(el).gridTemplateColumns.split(' ').length);
  expect(columnas).toBe(2);
  await expect(detalle).toContainText('2 caja(s) completas y 3 unidades sueltas');
  await expect(detalle.getByRole('textbox')).toHaveCount(0);
  await expect(detalle.getByRole('button', { name: 'Guardar cambios' })).toHaveCount(0);
  await page.keyboard.press('Escape');
  await expect(detalle).toHaveCount(0);
  await page.setViewportSize({ width: 1280, height: 900 });
  await expect(page.locator('.workspace-header')).toBeVisible();
  await expect(page.locator('.mobile-user-menu')).toBeHidden();
  expect(alta).toMatchObject({
    unidad: 'UNIDAD',
    precioCompra: 0.5,
    existenciasIniciales: 3,
    caja: { unidades: 12, precioCompra: 6, precioVenta: 8, cajasIniciales: 2 },
  });
  await page.getByRole('link', { name: 'Punto de venta', exact: true }).click();
  await page.setViewportSize({ width: 390, height: 844 });
  const cajaMovil = page.getByRole('button', { name: 'Agregar caja de Lápiz', exact: true });
  await expect(cajaMovil).toBeVisible();
  const limites = await cajaMovil.boundingBox();
  expect(limites!.x).toBeGreaterThanOrEqual(0);
  expect(limites!.x + limites!.width).toBeLessThanOrEqual(390);
  expect(await page.locator('.pos-table').evaluate((el) => el.scrollWidth <= el.clientWidth)).toBe(
    true,
  );
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.getByRole('button', { name: 'Agregar Lápiz', exact: true }).click();
  await page.getByRole('button', { name: 'Agregar caja de Lápiz', exact: true }).click();
  await page.getByRole('spinbutton', { name: 'Cantidad de Lápiz', exact: true }).fill('3');
  await expect(page.locator('.cart-total')).toContainText('$11.00');
  await page.getByRole('spinbutton', { name: 'Cantidad de Lápiz (caja)', exact: true }).fill('3');
  await expect(page.getByRole('button', { name: 'Confirmar cobro', exact: true })).toBeDisabled();
  await page.getByRole('spinbutton', { name: 'Cantidad de Lápiz (caja)', exact: true }).fill('1');
  await page.getByRole('button', { name: 'Exacto', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar cobro', exact: true }).click();
  await expect(page.getByText('Venta confirmada #17', { exact: true })).toBeVisible();
  await expect(page.getByText('Caja de 12 unidades', { exact: true })).toBeVisible();
  expect(venta).toMatchObject({
    totalEsperado: 11,
    lineas: [
      { productoId: 1, cantidad: 3 },
      { productoId: 1, cantidad: 1, presentacion: 'CAJA', unidadesPorPresentacion: 12 },
    ],
  });
  await page.getByRole('button', { name: 'Continuar', exact: true }).click();
  const agregarCaja = page.getByRole('button', { name: 'Agregar caja de Lápiz', exact: true });
  await expect(agregarCaja).toBeEnabled();
  await page.getByRole('button', { name: 'Agregar Lápiz', exact: true }).click();
  await expect(agregarCaja).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Agregar Lápiz', exact: true })).toBeEnabled();
  await page.getByRole('button', { name: 'Vaciar', exact: true }).click();
  await expect(agregarCaja).toBeEnabled();
  await agregarCaja.click();
  await expect(agregarCaja).toBeDisabled();
  await page.getByRole('button', { name: 'Vaciar', exact: true }).click();
  await page.getByRole('link', { name: 'Inventario', exact: true }).click();
  await page.locator('select[name="producto"]').selectOption({ label: 'Lápiz · 12 uds.' });
  await page.getByLabel('Presentación del movimiento').selectOption('CAJA');
  await page.locator('input[name="cantidad"]').fill('2');
  await page.locator('textarea[name="motivo"]').fill('Compra de dos cajas');
  await page.getByRole('button', { name: 'Registrar movimiento', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Movimiento registrado.');
  expect(movimiento).toMatchObject({
    productoId: 1,
    tipo: 'COMPRA',
    cantidad: 2,
    presentacion: 'CAJA',
    unidadesPorPresentacion: 12,
  });
});
