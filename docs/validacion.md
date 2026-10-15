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
