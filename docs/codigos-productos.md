# Códigos automáticos de artículos y servicios

El formulario de alta activa «Generar código automáticamente» de forma predeterminada. Al crear un artículo o servicio, un `sku` vacío u omitido solicita la generación en el servidor. Si se desactiva la opción, se puede escribir o escanear un código manual. La respuesta de alta y edición conserva `id` y agrega `sku` para mostrar el código guardado.

Formato: `ART-PAP-CUA-000048`.

- `ART` identifica un artículo; `SRV`, un servicio.
- Los siguientes segmentos usan hasta tres caracteres alfanuméricos de categoría y nombre al crearlo, sin espacios ni tildes. Por ejemplo, Papelería/Cuaderno produce `PAP-CUA`.
- El número es el identificador de ingreso asignado por SQL Server al producto, con un mínimo de seis dígitos. Es global, no se reinicia por día ni por categoría y puede tener saltos por operaciones canceladas. No se calcula como cantidad de productos más uno.

La asignación final ocurre dentro de la misma transacción que crea el producto y registra las existencias iniciales. Una clave temporal aleatoria permite obtener el identificador sin exponer un producto incompleto: se reemplaza antes de confirmar. No hay cambios de esquema para esta funcionalidad. La restricción única existente protege contra duplicados y los códigos automáticos de altas simultáneas tienen distintos identificadores. Si un código manual anterior ya ocupa la propuesta automática, se busca una variante con sufijo `-2`, `-3`, etc., con límite de intentos; un conflicto concurrente se rechaza por la restricción de SQL.

Los códigos existentes no se cambian. Un código asignado no se recalcula al renombrar el producto o la categoría, porque puede estar impreso o usarse en comprobantes y búsquedas. Al editar, el campo debe conservar un código no vacío; un administrador aún puede modificarlo explícitamente. Este SKU interno no constituye por sí mismo un código EAN/UPC del fabricante.
