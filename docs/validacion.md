# Validación de la entrega

Fecha: 15 de octubre de 2026.

## Ejecutado

- Backend compilado con Spring Boot 4.1.1. Se usó el JDK 17 instalado mediante `mvn -Dmaven.repo.local=../.cache/m2 -Djava.version=17 -B test`; el proyecto y CI están configurados para Java 21.
- Seis pruebas de `OperacionServiceTest`: seis aprobadas, cero errores, cero omitidas. Cubren total y ganancia, costos históricos, salida de stock, rechazo por stock insuficiente, duplicados, signo de movimientos y límites UTC del reporte local.
- Frontend compilado con Node 24.19.0 y Angular 22; generación de bundles finalizada con las cinco rutas de funcionalidad.
- Configuraciones JSON analizadas y `git diff --cached --check` sin errores.

Las dependencias se descargaron con cachés locales ignoradas por Git. La instalación de frontend usó `--ignore-scripts`; el build pudo usar los binarios opcionales distribuidos por las dependencias.

## Pendiente de un entorno configurado

No se proporcionó conexión ni credenciales de SQL Server. No se ejecutó el DDL ni se arrancó la API contra una base real. Quedan pendientes validación Hibernate/esquema, login y autorización HTTP, pruebas de rollback y ventas concurrentes en SQL Server, y recorrido del frontend conectado a la API. Los escenarios de aceptación están en `arquitectura.md`.

El build y las pruebas unitarias no constituyen una certificación de despliegue productivo. Las limitaciones funcionales de esta fase se detallan en el README.
