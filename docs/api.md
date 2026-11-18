# Contrato HTTP vigente

El restablecimiento local de vendedores y el cambio obligatorio de contraseña se describen en [Restablecer clave de vendedores](restablecer-clave-vendedores.md). Agrega `POST /api/usuarios/{id}/restablecer-clave` (ADMIN) y `POST /api/auth/cambiar-clave-inicial` (sesión limitada). Login y renovación incluyen el indicador `cambioClavePendiente`.

Mismo origen que la interfaz. Prefijo `/api`. Los importes son USD, dos decimales y cantidades enteras. Los costos solo se incluyen para ADMIN. Nunca se devuelven hashes, renovaciones ni tokens de recuperación en consultas de perfil o usuarios.

| Ruta | Acceso | Entrada / salida |
|---|---|---|
| POST /auth/login | Pública, X-PuntoStock: 1 | username, password, rememberMe opcional; accessToken, expiresIn=900, roles, username y cookie HttpOnly |
| POST /auth/renovar | Cookie y X-PuntoStock: 1 | Rota renovación y entrega nuevo acceso corto |
| POST /auth/salir | Cookie y X-PuntoStock: 1 | Revoca esa sesión y borra la cookie |
| POST /auth/recuperacion | Pública limitada | username y correo; mensaje genérico de procesamiento |
| POST /auth/restablecer | Pública limitada | username, token, password; mensaje de confirmación |
| GET/PUT /auth/me | Autenticado | Perfil propio / nombre, correo y teléfono |
| PUT /auth/me/foto y /portada | Autenticado | imagen Base64 validada; perfil actualizado |
| GET /auth/me/menu | Autenticado | Secciones y opciones autorizadas |
| GET /productos | Autenticado | Lista de compatibilidad; costo solo ADMIN |
| GET /productos/pagina | Autenticado | buscar, tipo, categoria, pagina=0, tamano=30 (máx. 100) |
| POST/PUT /productos y /productos/{id} | ADMIN | sku opcional al crear (vacío u omitido genera código), obligatorio al editar; nombre, descripcion, precioCompra, precioVenta, fotoUrl, tipo, categoria, marca, unidad, ubicacion, stockMinimo; existenciasIniciales opcional al crear (0–1000000); caja opcional con unidades, precioCompra, precioVenta opcional y cajasIniciales; imagen opcional data URL JPEG/PNG (máx. 1 MB decodificado), quitarImagen opcional; respuesta id y sku definitivo |
| DELETE /productos/{id} | ADMIN | Baja lógica |
| GET /productos/{id}/imagen | Autenticado | Imagen raster del producto activo en bytes, Content-Type JPEG/PNG y Cache-Control no-store; 404 si no tiene imagen o está inactivo |
| GET /productos/categorias | ADMIN | Categorías del catálogo: id, nombre, activo, version; incluye inactivas para administración |
| POST /productos/categorias | ADMIN | nombre (1–80 caracteres), activo; crea una categoría de artículos y servicios |
| PUT /productos/categorias/{id} | ADMIN | nombre, activo, version vigente; renombra o cambia estado; 409 para nombre duplicado o versión anterior |
| POST /ventas | Autenticado | lineas[productoId,cantidad,observaciones,presentacion opcional UNIDAD/CAJA,unidadesPorPresentacion esperadas opcional], metodoPago, recibido, totalEsperado, cliente, referenciaPago, solicitudId UUID; admite una línea por producto y presentación |
| GET /ventas | Autenticado | fecha local opcional, pagina=0; 6 ventas por página, cantidad, paginas y totales por medio. Vendedor solo ve sus ventas |
| GET /ventas/{id} | Dueño o ADMIN | Comprobante con valores históricos, sin costos; cada línea incluye presentacion y unidadesPorPresentacion históricas |
| GET /ventas/solicitud/{uuid} | Cuenta de la solicitud | Comprobante de reintento; 404 si aún no se registró |
| GET/POST /inventario/movimientos | ADMIN | Últimos 200 / productoId, tipo, cantidad, motivo, presentacion opcional UNIDAD/CAJA y unidadesPorPresentacion esperadas; el historial guarda cantidades convertidas a unidades individuales |
| GET /inventario/movimientos/pagina | ADMIN | desde/hasta locales inclusivos, productoId, pagina, tamano; permite recorrer todo el historial |
| GET /reportes/ganancia | ADMIN | fecha requerida; fecha, ganancia bruta y alcance |
| GET/POST /usuarios | ADMIN | Lista compatible sin imágenes / alta |
| GET /usuarios/pagina | ADMIN | buscar, activo, rol, pagina, tamano; proyección sin fotos ni hashes |
| GET/PUT /usuarios/{id} | ADMIN | Detalle explícito / username, password opcional al editar, rol, nombre, correo, telefono, activo |
| PUT /usuarios/{id}/activo | ADMIN | valor booleano; protege autodesactivación |
| DELETE /usuarios/{id} | ADMIN | Desactivación lógica |
| GET/PUT /menu-admin | ADMIN | Configuración / version y secciones; conflicto 409 ante versión vieja |

Las páginas nuevas usan `{contenido,cantidad,pagina,paginas}`; el historial de venta conserva `{ventas,cantidad,pagina,paginas,total,efectivo,tarjeta,transferencia,sinRegistro}`. Un comprobante tiene id, total, fecha ISO con offset UTC, vendedor, metodoPago, recibido, cambio, cliente, referenciaPago y lineas[productoId,sku,nombre,cantidad,precioUnitario,subtotal,observaciones].

El UUID de venta debe mantenerse al reintentar. El mismo contenido devuelve la venta ya confirmada; otro contenido con ese UUID produce 409. Tras 409 por precios, actualizar y pedir confirmación explícita; tras timeout o desconexión, consultar estado o reintentar la misma solicitud, sin crear otro cobro.

Errores: `{code,message,campos,requestId}`. 400 entrada inválida, 401 acceso inválido, 403 permiso/CSRF, 404 recurso no visible, 409 conflicto, 429 límite temporal (Retry-After), 503 servicio no configurado. Un fallo inesperado no publica SQL ni mensajes de infraestructura; correlacionar con X-Request-ID. El servidor no confirma la existencia de una cuenta en recuperación.


## Caja compartida (V8)

| Método y ruta | Permiso | Entrada / resultado |
|---|---|---|
| GET /api/caja/estado | Autenticado | abierta, id y pendienteDeCierre, sin saldos |
| GET /api/caja?pagina=0 | ADMIN | Página de 20 resúmenes, más reciente primero |
| GET /api/caja/{id} | ADMIN | Resumen, totales por medio de pago y cierre |
| GET /api/caja/{id}/movimientos?pagina=0 | ADMIN | Página de 20 registros de ventas, abonos y movimientos manuales |
| POST /api/caja/abrir | ADMIN | inicial, nota, solicitudId (UUID obligatorio) |
| POST /api/caja/{id}/movimientos | ADMIN | tipo ENTRADA/SALIDA, monto positivo, motivo, solicitudId |
| POST /api/caja/{id}/cerrar | ADMIN | contado, nota, solicitudId, esperado, ventas y abonos del resumen consultado |

Esperado = inicial + ventas y abonos en efectivo + entradas − salidas. Los cobros con tarjeta y transferencia se muestran por separado. Las notas admiten hasta 500 caracteres. El motivo de un movimiento y la nota de un cierre con diferencia son obligatorios. Los importes de entrada de caja admiten 9 enteros y 2 decimales. Cada movimiento requiere al menos 0.01 y un UUID para proteger reintentos. Un cierre con saldo, número de ventas o número de abonos desactualizados devuelve 409. Las nuevas ventas POST /api/ventas requieren una caja abierta. Las ventas previas con caja nula conservan su historial.

Ver reglas operativas y límites en [Caja](caja.md).

## Pedidos personalizados

El módulo está disponible para administradores y vendedores. Las operaciones usan la migración V13.

- `GET /api/pedidos?pagina=0` devuelve seis pedidos por página. `GET /api/pedidos/{id}` devuelve el pedido, sus abonos y los materiales consumidos.
- `POST /api/pedidos` recibe cliente, descripción, servicios, cantidad, precio unitario, fecha de entrega y un anticipo con monto, medio de pago y UUID de solicitud. El servidor calcula el total y registra el pedido y el anticipo juntos con una caja abierta.
- `POST /api/pedidos/{id}/abonos` recibe monto, medio de pago y UUID de solicitud. Admite cobros hasta el saldo pendiente. El recibo conserva el saldo al momento del abono.
- `POST /api/pedidos/{id}/materiales` recibe producto, cantidad en la unidad de inventario y UUID de solicitud. Registra una salida por consumo en el historial de inventario.
- `PUT /api/pedidos/{id}` edita los datos del pedido pendiente, sin modificar los abonos ni materiales. El total debe cubrir lo ya abonado.
- `PUT /api/pedidos/{id}/estado` permite marcar el pedido como listo o entregado. El saldo puede seguir pendiente después de la entrega. Para cancelar un pedido pendiente o listo, envía `estado: CANCELADO`, `ultimoAbono` y `ultimoConsumo` con los identificadores más recientes mostrados en la confirmación (o `null` si no hay registros). La cancelación conserva el historial y bloquea nuevos abonos y consumos.

Caja incluye los abonos por medio de pago y muestra el número de abonos por separado. El cierre compara ese número junto con el efectivo esperado y el número de ventas. Los pedidos y abonos se consultan en Pedidos.

### Cuentas

- `GET /api/cuentas?desde=AAAA-MM-DD&hasta=AAAA-MM-DD&pagina=0` devuelve los saldos actuales, las deudas y cobros pendientes, y los movimientos y el resultado estimado del período.
- `POST /api/cuentas` crea una cuenta personal o del negocio con saldo inicial. `POST /api/cuentas/movimientos` registra aportes, retiros, traspasos, entradas, gastos, compras y pagos a proveedores. Ambas operaciones admiten reintento con el mismo UUID y los mismos datos.
- Solo el rol administrador puede consultar o modificar cuentas. El arqueo sigue en Caja. Los traspasos entre Caja y una cuenta del negocio registran el cambio en ambos historiales dentro de la misma transacción. El saldo fuera de Caja requiere registrar los depósitos y liquidaciones efectivamente recibidos.
- Una compra registra un producto, unidades recibidas, importe y pago inicial. Incorpora las unidades y actualiza el costo promedio. Los pagos posteriores se aplican al saldo sin modificar inventario.
- El resultado estima el margen de mostrador más pedidos entregados y otros ingresos del negocio, menos costos de pedidos y gastos del negocio. Los entregados históricos sin fecha real se indican aparte. Saldos por cobrar y pagar corresponden a la fecha de consulta.

Desde V14, cada consumo guarda el precio de compra unitario vigente al registrarlo. El detalle muestra el costo unitario, el subtotal por consumo y el costo acumulado de materiales a administradores. Los consumos anteriores sin costo registrado se excluyen del total.
