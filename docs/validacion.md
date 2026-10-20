# Validación de la entrega

Fecha: 15 de octubre de 2026.

## Ejecutado

- Backend compilado con Spring Boot 4.1.1. Las seis pruebas unitarias se ejecutaron de nuevo con Java 21.0.11 y finalizaron con BUILD SUCCESS. En este Windows se aplicó al proceso de prueba `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=../.cache/local/no-unix-sockets`, el mismo fallback TCP usado por el lanzador local. El comando fue `mvn -o -B -Dmaven.repo.local=../.cache/m2 test`, desde `backend` con `JAVA_HOME` apuntando al JDK 21.
- Seis pruebas de `OperacionServiceTest`: seis aprobadas, cero errores, cero omitidas. Cubren total y ganancia, costos históricos, salida de stock, rechazo por stock insuficiente, duplicados, signo de movimientos y límites UTC del reporte local.
- Frontend compilado con Node 24.19.0 y Angular 22; generación de bundles finalizada con las cinco rutas de funcionalidad.
- Configuraciones JSON analizadas y `git diff --cached --check` sin errores.

Las dependencias se descargaron con cachés locales ignoradas por Git. La instalación de frontend usó `--ignore-scripts`; el build pudo usar los binarios opcionales distribuidos por las dependencias.

## Integración local verificada

Con autorización del usuario se creó `PuntoStock` en `localhost\SQLEXPRESS`. Se aplicó el DDL y Hibernate validó el esquema. La API está ejecutándose con Java 21.0.11; el frontend con Node 24 y Angular 22, en puertos 18080 y 14200 respectivamente.

`scripts/Test-Local.ps1` aprobó 41 comprobaciones HTTP/SQL: login, RBAC, ocultación de costos al vendedor, CRUD administrativo, stock, total, ganancia histórica, sobreventa, concurrencia real (HTTP 200/409 para dos ventas de la última unidad), rollback completo de venta/detalle/movimiento/stock ante un fallo SQL intencional, desactivación y firma JWT inválida. El trigger temporal se retiró al finalizar y se conservaron dos productos de demostración. El detalle está en `prueba-local-resultados.json`.

Se verificó HTTP 200 para el HTML de Angular y login más consulta de catálogo a través del proxy del frontend. Esto verifica la comunicación frontend/API; el recorrido visual completo de las pantallas queda para la prueba manual del usuario. La cuenta SQL de la aplicación no es sysadmin, no puede crear tablas y no tiene acceso a la otra base de negocio de la instancia. Se comprobó además que no quedó ningún trigger temporal de prueba.

El build y las pruebas unitarias no constituyen una certificación de despliegue productivo. Las limitaciones funcionales de esta fase se detallan en el README.

## Rediseño de interfaz · 16 de octubre de 2026

Se compiló en producción el nuevo login, navegación, resumen, POS, productos e inventario. Doce comprobaciones de Chrome verificaron ingreso real, errores de credenciales, estados de formularios, carrito, datos del resumen, signo de una merma con petición interceptada y adaptación a móvil sin errores JavaScript. Las capturas de escritorio y móvil se revisaron localmente. La prueba no registró ventas ni modificó inventario. Consulte `interfaz.md`.

Se agregó un cambio interactivo de contraseña para el administrador local, con entrada oculta, BCrypt y sincronización de la copia DPAPI. La migración `002_password_hash_constraint.sql` se aplicó a PuntoStock y se comprobó que rechaza con error SQL 547 una actualización de texto inválido; la prueba se ejecutó dentro de una transacción revertida. La contraseña elegida para la prueba local se verificó mediante login HTTP y navegador, sin registrar su valor en el repositorio.

Perfil de usuario (20 de octubre de 2026): compilación Angular correcta y 16 pruebas Java aprobadas. Prueba local HTTP/Chrome con una cuenta temporal VENDEDOR: edición persistente de datos, foto y portada con recorte, actualización del avatar, rechazo de correo e imagen inválidos, aislamiento del perfil administrador, RBAC, menú superior, Escape/clic exterior, claro/oscuro, móvil y eliminación del acceso guardado al cerrar sesión. La cuenta temporal y sus imágenes se eliminaron al terminar; no se modificó el perfil del usuario real.
