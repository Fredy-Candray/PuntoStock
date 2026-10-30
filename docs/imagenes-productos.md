# Imágenes locales de productos y servicios

El formulario permite elegir una imagen de la computadora con «Elegir imagen», mostrar una vista previa o quitarla con «Quitar imagen». Los cambios de imagen se descartan al cancelar el formulario sin guardar. El archivo original no se modifica. Se aceptan JPEG, PNG y WebP de hasta 10 MB en el navegador; se decodifican y se optimizan a un máximo de 1024 píxeles por lado. El envío se normaliza a PNG o JPEG con límite inferior al máximo admitido por el servidor. El guardado del producto queda bloqueado mientras se prepara el archivo o hay un error de selección recuperable.

La imagen se envía en el campo opcional `imagen` como data URL, separado de `fotoUrl`. Se reutiliza la validación de raster de `FotoPerfilValidator`: JPEG o PNG reales, MIME coherente, hasta 1 MB decodificado, dimensiones de hasta 4096 por lado y 4 megapíxeles. Se verifica la decodificación completa; no se admite SVG ni contenido que solo simule una imagen por su extensión o cabecera.

V11 crea `producto_imagenes`, con una imagen binaria por producto, MIME y revisión UUID. La imagen se guarda en la misma transacción que el producto y sus existencias iniciales; no se crean archivos públicos o rutas dependientes de la computadora. Los respaldos de la base incluyen las imágenes, por lo que no es necesario trasladar los archivos originales al restaurar en otro equipo.

`fotoUrl` conserva una ruta corta como `/api/productos/48/imagen?v=<revision>`. El catálogo no devuelve las imágenes en base64 ni carga su contenido al listar productos. El componente de imagen solo consulta las fotos de los productos renderizados y usa HttpClient con la sesión existente; libera sus URL de objeto al cambiar de imagen o salir de la vista. Si una imagen falla, muestra el icono de artículo o servicio. Productos y Punto de venta muestran miniaturas.

El GET de imagen requiere autenticación y solo entrega imágenes de productos activos, con MIME raster y `Cache-Control: no-store`. Crear o cambiar la imagen requiere ADMIN mediante los endpoints existentes de catálogo. Las URL HTTPS y los recursos estáticos anteriores siguen siendo compatibles.

Al editar sin `imagen` nueva y sin indicar `fotoUrl`, se conserva la imagen anterior. Enviar `quitarImagen: true` elimina la imagen almacenada y restablece el icono predeterminado. Un `fotoUrl` vacío también restablece el recurso predeterminado; cambiar a otra URL elimina la imagen binaria sustituida. Enviar una imagen nueva junto con `quitarImagen: true` se rechaza. La ruta protegida actual del propio producto se acepta al editar; no puede asignarse una ruta de imagen privada de otro producto como si fuera una URL externa.

## Validación del 29/10/2026

- Maven con `spotless:apply test`: 82 pruebas unitarias aprobadas. La integración SQL con `scripts/Test-SqlIntegration.ps1 -BuildDirectory .cache/producto-imagen-sql` ejecutó `verify` con 82 unitarias y 23 de integración, todas aprobadas en bases desechables.
- La nueva integración comprobó guardado y recuperación exacta de bytes PNG, imagen vinculada al producto, stock inicial sin cambios, consulta autenticada, rechazo anónimo, lectura de vendedor, prohibición de escritura del vendedor, reemplazo de revisión, eliminación y rechazo de contenido falso sin crear un producto.
- Compilación Angular y ESLint de los archivos afectados aprobados. La prueba de navegador de `e2e/productos-imagen.spec.ts`, con API simulada, comprobó selección local, archivo inválido, vista previa, persistencia tras recargar, miniatura en Punto de venta y eliminación. Se inspeccionó el selector en móvil.
- No se subieron fotos personales ni se crearon productos de prueba en la base del negocio para validar la funcionalidad.
