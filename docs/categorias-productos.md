# Categorías de productos y servicios

Las categorías del catálogo se guardan en `categorias_productos`, separadas de cualquier clasificación de recordatorios o calendario. El administrador puede crearlas, renombrarlas, desactivarlas y reactivarlas desde Productos o el botón junto al selector del formulario. La lista se consulta al servidor, no se define en Angular. Una categoría creada desde el formulario queda seleccionada sin perder los demás campos del producto.

La migración V9 incorpora las categorías existentes de todos los productos, incluidos los inactivos, y agrega las opciones iniciales que aún no existan. No cambia existencias, ventas ni movimientos. Conserva el contrato por nombre de categoría de la API y de los filtros existentes. La columna `productos.categoria` referencia la clave única `categorias_productos.nombre`; SQL Server propaga un renombrado mediante `ON UPDATE CASCADE`. La categoría tiene además un identificador estable para administrarla y una versión para rechazar ediciones desactualizadas.

No se borran categorías. Las inactivas no se ofrecen para nuevas asignaciones, pero un artículo que ya la utiliza puede conservarla al editar otros datos. Una categoría desconocida o inactiva para un producto nuevo se rechaza en el servidor. El nombre es obligatorio, se recortan espacios exteriores y se comprueba unicidad tanto en servicio como en SQL. Solo ADMIN puede consultar o administrar este catálogo de categorías.

El campo de categoría continúa siendo compartido entre artículos y servicios del negocio. Su tabla específica evita confundirlas con categorías de otros módulos. Desactivar una categoría no oculta ni desactiva los productos relacionados ni altera los comprobantes históricos.
