# Caja compartida

Una sola caja para la tienda. Administrador abre, registra entradas o salidas y cierra; las ventas de todos los vendedores se asignan automáticamente a la caja abierta. El servidor rechaza ventas nuevas sin apertura, incluidos tarjeta y transferencia. Preparar el carrito sigue permitido. Consultar o reintentar una venta ya confirmada conserva su resultado incluso después del cierre.

La pantalla presenta cada apertura como **Caja del día · fecha**, usando la fecha de apertura en hora de El Salvador. El número interno se conserva como referencia secundaria en el historial; no se reinicia ni se modifica en la base de datos. Dos aperturas del mismo día se distinguen por su hora y referencia.

## Uso

1. Entrar como administrador en **Caja** y contar el fondo inicial de efectivo. Puede ser cero. Confirmar la apertura.
2. Registrar ventas en Punto de venta como de costumbre. Se conserva el mismo comprobante y el control de stock. El efectivo suma el total vendido (recibido menos cambio); tarjeta y transferencia se muestran aparte y no aumentan el efectivo físico.
3. Registrar **Entrada** para un aporte o **Salida** para un retiro/pago desde caja, siempre con motivo. Son importes positivos; el sistema determina el signo. Una salida no puede superar el efectivo esperado. Las ventas no se ingresan otra vez como movimientos manuales.
4. Terminar los cobros, actualizar el resumen, contar únicamente el efectivo físico y confirmar el cierre. Diferencia = contado − esperado: positivo es sobrante, negativo es faltante. Una diferencia requiere una observación. Si cambió el saldo o entraron ventas desde la última consulta, se rechaza ese cierre y se pide revisar el resumen actualizado.
5. Consultar el cierre en el historial. No hay edición, borrado ni reapertura de un cierre. Para continuar, abrir una caja nueva con el efectivo físico que realmente permanece en la tienda; no se traslada automáticamente el saldo anterior.

No hay cierre automático a medianoche: la apertura es un turno y puede cruzar de día. Fechas almacenadas en UTC y mostradas en hora de El Salvador. Solo USD y dos decimales. Aperturas, conteos y movimientos manuales admiten hasta 999.999.999,99 USD.

## Protección de operaciones

Una fila de control fija y bloqueada dentro de la transacción serializa aperturas, movimientos, ventas y cierres. Una restricción SQL impide dos cajas abiertas. La venta, su vínculo a caja, detalles e inventario se confirman o revierten juntos.

Apertura, movimiento y cierre usan UUID de solicitud. Repetir el mismo contenido devuelve el resultado registrado; usar la solicitud con otros datos se rechaza. La pantalla conserva solicitudes inciertas en sessionStorage, por usuario, y permite reintentar la misma operación tras recargar. No cerrar esa pestaña ni borrar sus datos mientras una operación esté pendiente. El historial permite revisar lo confirmado.

El estado mínimo (abierta e ID) está disponible a usuarios autenticados para el POS. Los saldos, movimientos, historial y operaciones de caja requieren ADMIN en el servidor, aunque se altere el menú del navegador.

## Instalación y datos anteriores

La migración nueva es **V8__caja.sql**; `database/008_caja.sql` es su copia de referencia. No editar V1-V7 ni aplicar V8 manualmente después de Flyway.

En el entorno Windows local, detener el backend, ejecutar desde la raíz `./scripts/Migrate-Local.ps1` y arrancarlo después. El script respalda y verifica antes de migrar, aplica Flyway y retira los permisos de migración temporales. La inicialización de una instalación nueva sigue siendo Initialize-Local, Migrate-Local y arranque. En otras instalaciones, usar la cuenta de migración y el procedimiento de despliegue habitual.

V8 crea cajas, control y movimientos; añade `ventas.caja_id` nullable e índices, y agrega Caja al menú sin sustituir sus opciones existentes. Las ventas históricas quedan con caja nula: se consultan en Ventas y no se atribuyen retroactivamente a una apertura. Los respaldos de versión 7 se conservan válidos, pero deben actualizarse con Flyway antes de ejecutar este código; no contienen la función de caja.

Una salida de efectivo no es un reembolso de venta ni contabiliza gastos automáticamente. Esta función no incorpora anulaciones financieras, impuestos, facturación fiscal, conciliación bancaria ni varias cajas/vendedores independientes.
