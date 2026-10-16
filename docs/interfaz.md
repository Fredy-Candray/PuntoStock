# Interfaz de PuntoStock

Rediseño del 16 de octubre de 2026: identidad en azul petróleo y verde, login adaptable con campos etiquetados, opción para mostrar la contraseña y errores específicos para credenciales o conexión. Las pantallas autenticadas comparten navegación lateral, encabezado de cuenta, botones, tablas, estados vacíos y formularios consistentes.

El POS organiza el catálogo y el carrito en áreas separadas; permite sumar, restar y quitar unidades y muestra el total como estimación hasta la respuesta del servidor. Productos incorpora búsqueda y un formulario de alta/edición desplegable. Inventario muestra nombres de producto, fechas en horario de El Salvador y el sentido de cada movimiento. En compras y salidas se ingresa una cantidad positiva; el formulario calcula el signo según el tipo. El ajuste manual conserva el delta con signo.

El resumen consulta productos, movimientos y ganancia en la API. Los indicadores de existencias muestran el inventario actual; la fecha seleccionada corresponde únicamente a la ganancia. No se agregaron cifras ficticias ni se cambió el cálculo financiero del backend.

## Validación visual y funcional

Se compiló Angular en producción. Se revisaron capturas de escritorio (1440 px) y móvil (390 px), generadas localmente en `.cache/ui-preview`, fuera de Git.

Se ejecutaron doce comprobaciones con Chrome y Playwright: login vacío deshabilitado; login móvil sin desbordamiento horizontal; mostrar/ocultar contraseña; mensaje de credenciales incorrectas; ingreso real del administrador y lectura del resumen; agregar y quitar del carrito; formulario vacío de producto deshabilitado; indicación de salida en inventario; conversión de una cantidad positiva al delta negativo de una merma; inventario móvil sin desbordamiento; y ausencia de errores JavaScript. La solicitud de merma se interceptó en el navegador para verificar su cuerpo sin enviarla al servidor. Las pruebas de interfaz no registraron ventas ni modificaron inventario.

## Acceso local

Si el login rechaza la clave escrita, puede copiarse la contraseña guardada del administrador sin imprimirla. Desde `frontend`:

```powershell
pwsh -NoProfile -File ..\scripts\Copy-LocalAdminPassword.ps1
```

Entrar con `admin.local` y pegar la clave con Ctrl+V. El secreto permanece cifrado con DPAPI en `.cache/local/secrets.clixml`; no se incorpora al frontend ni al repositorio. El login diferencia un HTTP 401 de los errores de conexión.
