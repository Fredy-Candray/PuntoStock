# Contrato HTTP vigente

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
| POST/PUT /productos y /productos/{id} | ADMIN | sku, nombre, descripcion, precioCompra, precioVenta, fotoUrl, tipo, categoria, marca, unidad, ubicacion, stockMinimo; id |
| DELETE /productos/{id} | ADMIN | Baja lógica |
| POST /ventas | Autenticado | lineas[productoId,cantidad,observaciones], metodoPago, recibido, totalEsperado, cliente, referenciaPago, solicitudId UUID |
| GET /ventas | Autenticado | fecha local opcional, pagina=0; 20 ventas por página, cantidad, paginas y totales por medio. Vendedor solo ve sus ventas |
| GET /ventas/{id} | Dueño o ADMIN | Comprobante con valores históricos, sin costos |
| GET /ventas/solicitud/{uuid} | Cuenta de la solicitud | Comprobante de reintento; 404 si aún no se registró |
| GET/POST /inventario/movimientos | ADMIN | Últimos 200 / productoId, tipo, cantidad, motivo |
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
