# Validación de caja compartida — 27 de octubre de 2026

## Pruebas ejecutadas

- Backend: **63 pruebas unitarias** y **19 de integración con SQL Server**, todas aprobadas sin omisiones. Salida: `.cache/caja-verificacion/surefire-reports` y `failsafe-reports`; log `.cache/revision/caja-sql.log` con BUILD SUCCESS. Se conserva el umbral de cobertura de los servicios críticos anteriores.
- Frontend: formato, ESLint, tipos TypeScript, seis pruebas unitarias y compilación Angular aprobados; auditoría npm sin vulnerabilidades reportadas. Log: `.cache/revision/caja-frontend.log`.
- Navegador: cinco escenarios aprobados en `.cache/revision/caja-e2e.log`: cuatro de POS y uno del ciclo de caja. La comprobación final tras ajustar el reinicio de formularios y la casilla de confirmación agotó el tiempo de inicio del trabajador en Windows; se repitió el mismo escenario, con las mismas aserciones, directamente en Playwright y pasó en escritorio y móvil (`.cache/revision/caja-browser-directo.log`). Todas las llamadas API de navegador fueron simuladas.
- La integración SQL prueba efectivo neto de cambio, separación de tarjeta/transferencia, aperturas simultáneas, venta concurrente con cierre, salidas excesivas, cierre desactualizado, cierre con diferencia, idempotencia, autorización y validación HTTP. También conserva las pruebas previas de stock, rollback, comprobantes, sesiones y recuperación.
- Actualización desde versión 7 comprobada en SQL aislado: conserva una venta histórica con caja nula y las opciones personalizadas del menú, y agrega Caja. El contrato de las 32 rutas anteriores permanece; se agregan siete rutas propias de caja.

## Activación local

Se detuvo el backend, se generó y verificó un respaldo previo mediante Migrate-Local y se aplicó V8 con Flyway. La comparación anterior/posterior conservó cantidad de ventas, total vendido, detalles, productos, stock total, usuarios y movimientos de inventario. Evidencia: `.cache/revision/caja-activacion.log` y archivos de conciliación locales excluidos de Git.

El esquema quedó en versión 8, la cuenta de aplicación sin privilegios DDL temporales y cero bases QA restantes. Salud, disponibilidad e interfaz devolvieron HTTP 200. No se crearon aperturas ni ventas de prueba en la base del negocio.

Windows/OneDrive bloqueaba la limpieza de directorios de recursos compilados anteriores. Start-Backend ahora deriva una carpeta `.cache/backend-run-<huella>` del contenido de las fuentes de producción y del pom. Reutiliza solo compilaciones de las mismas fuentes, evita clases retiradas y no necesita borrar directorios anteriores al arrancar. El arranque local de esta entrega se verificó con esa estrategia.

Los cambios de caja permanecen locales hasta solicitar su publicación. Los respaldos de versión 7 anteriores siguen siendo respaldos válidos de aquella fecha, pero no incluyen caja: el otro equipo debe aplicar V8 con el código correspondiente o restaurar una copia posterior a esta migración. Ver [uso de Caja](caja.md).
