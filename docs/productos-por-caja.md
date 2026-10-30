# Compra por caja y venta por piezas o caja completa

El stock del producto desglosado siempre representa unidades individuales. En Productos, seleccionar CAJA como unidad abre la configuración de compra por caja y fija UNIDAD como base de venta e inventario. También se puede elegir directamente la forma de compra. Los productos existentes que ya se vendían exclusivamente como caja mantienen su comportamiento mientras no se conviertan explícitamente.

La configuración persistida del producto incluye unidades por caja (2–1000000), costo de compra de la caja y precio de venta de caja opcional. La ausencia de este último deshabilita la venta por caja completa. El costo por pieza se calcula en servidor dividiendo el costo de la caja entre su contenido, con redondeo HALF_UP a dos decimales, acorde con los importes existentes del sistema. El precio de venta por pieza lo define el administrador; el precio de venta de caja es independiente y puede incorporar descuento.

Ejemplo: 2 cajas de 12 piezas a $6 cada caja, más 3 piezas sueltas, generan 27 unidades iniciales y costo unitario $0.50. El servidor ignora un costo unitario distinto enviado por el cliente cuando se proporciona la configuración de caja. El registro inicial de existencias sigue siendo un único movimiento dentro de la transacción de alta. Al editar no se aceptan cajas iniciales ni nuevas existencias iniciales. No se reinterpreta stock de una unidad anterior diferente mientras tenga saldo.

La API de producto acepta `caja: {unidades, precioCompra, precioVenta, cajasIniciales}` o null. La respuesta de catálogo incluye la configuración y excluye el costo de compra de la caja para vendedores. En Inventario se pueden registrar movimientos en unidades o cajas: el servidor valida el factor vigente y convierte antes de registrar el movimiento, conservando el mismo mecanismo transaccional de inventario. El motivo identifica el número de cajas y su contenido; el historial y el saldo final se expresan en piezas.

En Punto de venta se puede agregar la pieza o una caja completa. Dos presentaciones del mismo producto comparten las existencias y pueden coexistir en una venta. Cada presentación solo admite una línea por producto. El servidor bloquea cada producto una vez, suma el consumo combinado y valida el saldo antes de registrar el cobro. Los precios y factores se resuelven en servidor, y el factor esperado del cliente permite rechazar una caja cuyo contenido cambió antes de cobrar. No se confía en una multiplicación ni un precio calculados por el navegador.

La migración V10 agrega la configuración de caja en productos y la presentación/factor históricos en detalle_venta. Los detalles anteriores se conservan como UNIDAD, factor 1. La unicidad pasa a venta, producto y presentación. Caja y pieza comparten código de producto; el escaneo agrega una pieza, mientras que la caja completa se elige explícitamente en el punto de venta.

El comprobante de pantalla, impresión y PDF indica «Caja de N unidades» junto a la cantidad de cajas y su precio. Los detalles conservan el factor, costo y precio al cobrar aunque después cambie el catálogo. La firma de reintento incluye la presentación y el factor esperado de las cajas, preservando la firma anterior de las líneas por unidad. Un reintento confirmado no descuenta stock nuevamente.

Una venta de caja usa el costo histórico completo de la caja; una venta por piezas usa el costo unitario redondeado a centavos. Esta funcionalidad no introduce valoración por lotes, FIFO, gasto de caja automático ni reembolsos financieros.

La caja de venta representa una agrupación del número configurado de piezas disponibles. No se distingue físicamente entre cajas selladas y abiertas.

## Validación de esta entrega (29/10/2026)

- `mvn -o -B -Dmaven.repo.local=../.cache/m2 -Dpuntostock.build-dir=../.cache/producto-caja-check spotless:apply test`: 79 pruebas unitarias aprobadas; `spotless:check` posterior aprobado.
- `scripts/Test-SqlIntegration.ps1 -BuildDirectory .cache/producto-caja-sql`: `verify` aprobado en dos bases desechables, con 22 pruebas de integración. Incluye alta con conversión de costo y stock, venta mixta, reintento sin doble descuento, configuración cambiada, comprobante histórico, entradas por caja y ventas concurrentes sobre el mismo saldo. La primera ejecución detectó falta de separación GO en V10 y fue revertida; la ejecución posterior con lotes corregidos pasó completa.
- `ng build --progress=false`: compilación aprobada. Persisten las advertencias CommonJS conocidas de las dependencias de PDF.
- ESLint de los seis archivos TypeScript afectados y Prettier de los archivos modificados: aprobados.
- Prueba de navegador de `e2e/productos-caja.spec.ts` mediante el ejecutor directo de Playwright con API simulada: aprobada. Se comprobaron la selección CAJA, cálculo de costo, unidades iniciales, venta de piezas y caja, stock compartido, detalle del comprobante y movimiento posterior por caja. Los seis escenarios anteriores de `e2e/venta.spec.ts` también pasaron con el ejecutor directo. No se ejecutó toda la suite de navegador ni se registraron ventas de prueba en la base del negocio.
- V10 aplicada en la base local tras respaldo COPY_ONLY con CHECKSUM y VERIFYONLY. Se compararon antes y después los conteos de productos, ventas, detalles y movimientos, el stock total y el importe vendido; coincidieron. Los detalles previos conservaron presentación UNIDAD y factor 1. La cuenta de aplicación terminó sin permisos DDL.
- Backend, readiness y frontend respondieron HTTP 200 al terminar.
