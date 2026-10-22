# Operación de la papelería y servicios

## Qué permite esta versión

El catálogo distingue artículos con existencias de servicios que se cobran por cantidad. Se pueden combinar en una misma venta, cobrar en efectivo, tarjeta o transferencia, calcular el cambio, imprimir un comprobante y recuperarlo desde Ventas. Los precios, cantidades y pagos se validan en el servidor. Los servicios conservan su costo estimado al venderse para calcular la ganancia bruta.

## Preparar el catálogo

En **Productos**, crea artículos para juguetes, carteras, audífonos, útiles y papelería. Configura categoría, marca, unidad de venta, ubicación y stock mínimo. Una caja, paquete o resma es una unidad comercial independiente: no se convierte automáticamente en unidades sueltas. Usa códigos distintos para presentaciones con precios distintos.

Para impresiones y trabajos, utiliza **Nuevo servicio**. Ejemplos de entradas separadas:

| Servicio | Unidad | Qué diferenciar |
|---|---|---|
| Fotocopia B/N carta | Página | Color y tamaño |
| Impresión color carta | Página | Color y tipo de papel |
| Fotografía 4 × 6 | Fotografía | Tamaño y acabado |
| Escaneo de documento | Página | Formato del trabajo |
| Diseño o preparación de trabajo | Trabajo | Alcance del servicio |

Define tus precios reales; el sistema no los inventa. El precio de venta debe ser mayor que cero. Para servicios, registra el costo estimado de papel, tinta y materiales por unidad. Un servicio se vende aunque tenga cero existencias. El papel y la tinta no se consumen automáticamente de otro artículo; ese control requiere una receta de materiales, todavía no implementada.

Las imágenes son opcionales y tienen un recurso predeterminado. No se almacenan los archivos ni las fotografías de los clientes: solo el concepto, cantidad y observaciones del trabajo.

## Registrar existencias

En **Inventario**, registra las entradas iniciales y compras de artículos. Cada venta descuenta sus existencias y registra el movimiento dentro de la misma transacción. No se permiten movimientos de inventario para servicios. Para convertir un artículo existente en servicio, sus existencias deben estar en cero.

## Cobrar

1. En **Punto de venta**, busca por nombre, categoría, marca o código. Con un lector que funciona como teclado, enfoca la búsqueda, escanea y termina con Enter.
2. Agrega artículos y servicios. Escribe directamente cantidades como 50 fotocopias o 12 fotografías. Añade observaciones del trabajo si hacen falta.
3. Revisa el total. En efectivo, introduce lo recibido o usa **Exacto**. El sistema calcula el cambio y rechaza un importe insuficiente.
4. Con tarjeta o transferencia, realiza/verifica primero el pago externo y registra su referencia opcional. PuntoStock registra el medio de pago; no ejecuta cargos bancarios.
5. Confirma el cobro. El servidor vuelve a consultar los precios y el stock. Si cambió un precio, revisa el nuevo importe antes de volver a confirmar.
6. Imprime el comprobante o continúa con otra venta. La impresión abre el diálogo del navegador; el formato está preparado a 72 mm de ancho para papel térmico de 80 mm, y también se puede imprimir en una hoja convencional. No es un módulo de facturación fiscal.

## Si se pierde la conexión

La operación conserva un identificador y el carrito de cobro pendiente en la pestaña. **Consultar estado** recupera una venta ya registrada. **Reintentar confirmación** utiliza el mismo identificador y devuelve la venta existente sin volver a descontar stock. El servidor rechaza reutilizarlo con otro contenido.

No repitas el pago en la terminal bancaria por un fallo de conexión con PuntoStock. Si cierras completamente la pestaña y no se conserva el cobro pendiente, consulta primero el historial de Ventas antes de registrar otra operación.

## Consultar el día

En **Ventas**, el administrador consulta todas las ventas; el vendedor consulta únicamente las realizadas con su cuenta. La fecha usa la zona de El Salvador. Hay paginación, totales por medio de pago y reimpresión. Los comprobantes de ventas nuevas conservan los nombres, códigos y precios del momento de la venta. Para ventas antiguas sin esa información, se muestra el nombre/código actual y el medio de pago queda como **No registrado**.

Los totales de efectivo representan ventas en efectivo, no el saldo de una caja con fondo inicial, retiros o gastos.

El comprobante ofrece **Continuar**, **Descargar PDF** e **Imprimir comprobante**. La descarga está disponible al confirmar el cobro y al abrir una venta desde el historial. Genera `PuntoStock-V-000001.pdf` (con el número correspondiente) en el navegador, usando los datos de la venta registrada y sin volver a cobrar ni modificar inventario. Incluye cliente, vendedor, fecha de El Salvador, detalle, total, medio de pago, importe recibido, cambio y referencia cuando corresponda. Los comprobantes largos se dividen en páginas.

**Compartir PDF** entrega el archivo al menú de compartir del dispositivo: el usuario elige la aplicación y el destinatario. WhatsApp y correo aparecen si están disponibles como destinos en ese equipo. El PDF se prepara antes del clic para conservar el permiso de interacción del navegador. No se sube a un servidor público ni se envía automáticamente a ningún contacto. Si el navegador no admite compartir archivos, o bloquea la operación, se ofrece descargar el PDF para adjuntarlo manualmente. Cancelar el menú no descarga archivos ni genera otro cobro. El soporte depende del navegador y requiere un contexto seguro (HTTPS o localhost); una dirección HTTP de la red local puede necesitar HTTPS para habilitar esta función.

## Próximas funciones para una operación más completa

- Apertura, movimientos y cierre de caja con conciliación del efectivo.
- Devoluciones y anulaciones que reviertan cobro, ganancia y existencias con auditoría. Una entrada manual de inventario no anula una venta.
- Órdenes de impresión con estados, anticipos, entregas y clientes frecuentes.
- Compras y proveedores, consumo de materiales por servicio y conversión de presentaciones.
- Gestión visual de empleados, importación masiva y configuración de datos comerciales del comprobante.
- Facturación fiscal e integración bancaria, según las necesidades del negocio.

Estos puntos no se presentan como implementados en esta entrega.
