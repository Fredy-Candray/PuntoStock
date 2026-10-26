# Arquitectura vigente

Monorepo Angular / Spring Boot Java 21 / SQL Server. El backend conserva un monolito por capas: HTTP → controladores y entradas validadas → servicios → repositorios y JDBC → SQL Server. Las entidades no se serializan directamente como respuesta. `open-in-view=false` exige resolver relaciones dentro de una transacción.

## Datos y consistencia

Usuario N:M Rol; Usuario 1:N Venta y Movimiento; Venta 1:N DetalleVenta; Producto 1:N DetalleVenta y Movimiento. Menú JSON versionado, sesiones revocables y outbox de correo completan el modelo. Producto y usuario se desactivan; no se borran ventas ni movimientos históricos mediante API.

VentaService es el coordinador transaccional de venta. Bloquea la cuenta y los productos por ID ordenado, verifica el UUID y su firma, calcula importes con BigDecimal, captura precio/costo/nombre/SKU y guarda venta, detalles, stock y bitácora de forma atómica. Un servicio se cobra por cantidad, pero no descuenta stock. Nunca introducir un segundo descuento desde triggers.

El índice único por usuario y solicitud refuerza la idempotencia; una repetición idéntica devuelve el comprobante original y una distinta devuelve 409. El carrito pendiente de navegador mantiene el mismo UUID tras una respuesta incierta. Consultar la solicitud antes de volver a cobrar. El totalEsperado exige reconfirmación si cambió el catálogo.

`Producto.aplicarMovimiento` valida rango; stockAnterior + delta = stockPosterior. SQL también refuerza stock no negativo, tipos/signos y unicidad de producto en una venta. Los reportes usan el intervalo semiabierto del día de America/El_Salvador convertido a UTC. Las pruebas con reloj fijo cubren el límite de 06:00 UTC. Se conserva el mapeo histórico de datetime2 para no desplazar registros existentes sin una migración temporal específica.

## Identidad y correo

JWT de quince minutos en memoria, renovación revocable en cookie HttpOnly rotada, roles y authVersion consultados desde SQL. No se mantienen Bearer de siete días en almacenamiento web. La consulta de autenticación excluye imágenes. Ver diseño de CSRF, límites y rotación en [estabilidad](estabilidad.md).

El perfil y administración comparten normalización de correo e invalidación de recuperación. El token aleatorio se guarda como hash. El outbox cifrado comparte la transacción de la solicitud; un trabajador envía SMTP fuera de ella y reintenta sin bloquear al usuario.

## Frontend y consultas

`main.ts` solo arranca la aplicación; `app.routes.ts` define rutas lazy y `app.component.ts` el marco visual. AuthService mantiene la sesión, ApiService las operaciones y MenuService la navegación cacheada con invalidación. Perfil y menú ofrecen reintento visible. Los guards no otorgan permisos de API.

Las pantallas administrativas paginan y filtran en servidor. ConsultasService concentra las proyecciones de lectura. El historial ya paginaba; sus totales ahora se agrupan en una consulta. El catálogo completo sigue disponible por compatibilidad para POS, resumen y selectores; medir ese uso antes de ampliar a catálogos masivos. No se afirma una mejora de rendimiento medida sin una comparación reproducible.

## Contratos, ejecución y evidencia

- [Contrato de API](api.md): fuente vigente de rutas, cuerpos, permisos, páginas y errores.
- [Estabilidad](estabilidad.md): seguridad, migraciones, respaldo, despliegue y límites.
- [Validación](validacion.md): comandos y alcance de la evidencia, separando antecedentes de pruebas actuales.
- Flyway V1-V7 es la fuente de migraciones de ejecución. Hibernate valida, no modifica el esquema.
- Catálogo, inventario, ventas y perfil tienen servicios y controladores separados. Los contratos validados residen en application. InventarioService participa obligatoriamente en la transacción de VentaService al registrar movimientos de una venta. Una prueba compara las 32 rutas y sus permisos con el contrato previo. No se afirma una arquitectura hexagonal ni microservicios.

La ganancia reportada es bruta y usa costo histórico de catálogo. Una devolución manual de stock no es reembolso ni anulación financiera. No se han definido impuestos, FIFO/promedio, cierres de caja o documentos fiscales.
