# Seguimiento de hallazgos - 26 de octubre de 2026

Referencias: H01-H20 del documento consolidado y S/D/C/O/M de la revisión unificada. No confundir código aplicado, pruebas locales y validación del despliegue real. Los resultados nuevos se leen de los reportes de la suite; los documentos originales se conservan.

| Hallazgo | Corrección aplicada | Límite o pendiente |
|---|---|---|
| H01 / S1 | Límites por IP y cuenta, 429, comparación BCrypt ficticia equivalente, límites de recuperación | En varias réplicas, compartir límite en proxy |
| H02 / S2 | JWT 15 min en memoria, cookie de renovación HttpOnly rotada, revocación por sesión y authVersion, prueba CSRF | Secure en producción requiere HTTPS real |
| H03 / S5 | Identidad.cambiarCorreo común, normalización y cancelación de enlaces | Prueba de regresión en perfil y administración |
| H04 / S4,S7 | Cabeceras API/proxy, script de comprobación y procedimiento de rotación | HTTPS/HSTS y política del dominio no se certifican sin desplegar allí |
| H05 / D2 | Proyección de autenticación sin fotos/portada y captura SQL en prueba HTTP | No se afirma una reducción de milisegundos sin medición de carga |
| H06 / D3,D4 | Menú con TTL, invalidación y agrupación de solicitudes; catálogo usa identidad ya validada | Los bloqueos/relecturas que protegen una venta no se eliminan por optimización |
| H07 / D5,D6 | Páginas y filtros en servidor, UI administrativa, totales agrupados y EntityGraph | POS, resumen y selectores conservan lista completa por compatibilidad; medir y paginar esa selección antes de catálogos masivos |
| H08 / S3 | Outbox transaccional cifrado, trabajador sin transacción SMTP, leases y reintentos acotados | Aceptación SMTP no garantiza entrega; posible duplicado del mismo enlace tras caída entre envío y confirmación |
| H09 / D1 | Flyway, checksum, baseline explícito probado, respaldo previo y privilegios DDL temporales | Nuevas migraciones deben ejecutarse con cuenta de migración, nunca repair automático |
| H10 / C1 | Servicios separados de catálogo, inventario, venta, perfil y consultas; contratos en application | Se conserva un único coordinador transaccional de venta |
| H11 / C2,C3 | Contratos en application, respuestas tipadas de sesión/productos/movimientos/reporte y errores uniformes | Persistencia conserva algunos String para mantener compatibilidad con registros históricos |
| H12 / C4,C6 | Clock en venta, recuperación y fechas del controlador; zona común; método de stock; pruebas del límite UTC | Encapsulación completa de entidades pendiente de refactorización gradual |
| H13 / C5,C7 | Rutas/raíz/arranque separados, AuthService separado, fallos visibles, Spotless/Prettier/ESLint | Compatibilidad cubierta por contrato de 32 rutas/permisos y pruebas HTTP, SQL y navegador |
| H14 / O6,M1 | Contrato actual único, guía de versiones/puertos, antecedentes identificados y secretos/respaldo ignorados | Logos alternativos se conservan como antecedentes, sin afirmar que son recursos activos |
| H15 / O1 | Integración SQL real en bases descartables y workflow CI con SQL Server | Ejecución remota se confirma en GitHub después de publicar los cambios |
| H16 / O2,O5 | Unitarias frontend, E2E de venta/respuesta incierta/precios, cobertura JaCoCo y umbral en servicios críticos | E2E usa API simulada; SQL se verifica por separado en integración |
| H17 / S6 | Parches compatibles de Angular/Vite, auditoría npm, Dependabot y workflow de análisis | CodeQL/dependency-review privados necesitan habilitación del producto de GitHub |
| H18 / O4 | Health/readiness, métricas protegidas, correlación, logs seguros y restauración aislada con conciliación | Frecuencia externa, retención, RPO/RTO y alertas necesitan decisiones operativas |
| H19 / O3,O4 | Versiones alineadas, perfiles y Dockerfiles/compose | Docker no disponible localmente; contenedores y TLS deben validarse en su host antes de producción |
| H20 / negocio | UI y documentación distinguen ganancia bruta y devolución de stock de reembolso | Impuestos, documentos fiscales, caja y costeo requieren definición comercial/contable |

## Reorganización autorizada

Se separaron los controladores de catálogo, inventario y ventas, se extrajo PerfilService y se trasladaron los contratos a application. VentaService conserva la transacción completa y delega el movimiento en InventarioService con propagación MANDATORY. Las entradas y validaciones se conservaron. Ver la [evidencia de refactorización](resultados-refactorizacion-2026-10-26.md) de esta misma fecha.

## Fuentes de evidencia

- `backend/target/surefire-reports`: unitarias ejecutadas.
- `backend/target/failsafe-reports`: SQL real; sin SQL_TEST_URL explícita, la integración queda omitida.
- `backend/target/site/jacoco`: cobertura; umbral de líneas 75% en VentaService y RecuperacionService. La validación aislada de la reorganización usa `.cache/refactor-verificacion`.
- Vitest y Playwright: pruebas de señales/sesión/menú/dinero y escenarios de navegador.
- `scripts/Test-BackupRestore.ps1`: restauración íntegra y conciliación real, no solo VERIFYONLY.
- `scripts/Test-SecurityHeaders.ps1`: prueba del dominio HTTPS, pendiente de disponer de ese despliegue.

Los cambios permanecen locales hasta que se solicite publicarlos. Archivos .cache, respaldos, credenciales y PDFs de trabajo no se incluyen en Git.
