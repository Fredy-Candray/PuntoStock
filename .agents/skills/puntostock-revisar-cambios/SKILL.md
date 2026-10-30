---
name: puntostock-revisar-cambios
description: Revisa código o diferencias de PuntoStock para detectar errores, regresiones y riesgos de permisos o integridad. Úsalo cuando se pida una revisión técnica de cambios; no lo actives para preguntas generales sobre el proyecto.
---

# Revisar cambios de PuntoStock

- Determina el alcance solicitado y consulta el diff disponible, los archivos afectados y sus pruebas. Conserva cambios ajenos a la petición.
- Usa el código actual como evidencia. Los informes PDF y resultados históricos describen una revisión pasada, no demuestran el estado presente.
- En ventas e inventario verifica atomicidad entre venta, detalle, stock y movimientos; bloqueo consistente de productos; precios históricos; importes calculados en servidor e idempotencia ante reintentos. Un servicio no debe descontar stock como si fuera un artículo.
- En cambios de identidad o autorización comprueba controles del servidor, acceso al recurso de otro usuario, invalidación de sesiones y ausencia de costos o credenciales en respuestas no autorizadas. El menú y los guards no sustituyen autorización de API.
- Comprueba que migraciones nuevas y contratos cliente-servidor sean compatibles con el cambio. No supongas que mocks prueban restricciones o rollback de SQL Server.
- Reporta únicamente hallazgos accionables con prioridad, archivo y línea, condición que los provoca y consecuencia. Distingue un riesgo pendiente de comprobar de un error demostrado.
- Si se pidió solo revisión, no implementes correcciones. Indica las comprobaciones hechas y las limitaciones. Si no hay hallazgos, dilo sin inventar problemas.
