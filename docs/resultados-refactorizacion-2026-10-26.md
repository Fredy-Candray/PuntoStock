# Validación de la reorganización autorizada

Fecha: 26 de octubre de 2026. Complementa la validación de estabilidad de esta fecha; no sustituye sus límites de despliegue.

Se separaron catálogo, inventario, ventas y perfil. Los contratos validados están en application. VentaService conserva la transacción de venta; InventarioService exige una transacción existente para registrar sus movimientos. Se conservaron las entradas, validaciones, importes históricos y permisos. El arranque local limpia clases compiladas anteriores para evitar cargar controladores retirados.

## Resultados ejecutados

| Comprobación | Resultado |
|---|---|
| Unitarias Java | 62 aprobadas, sin fallos ni omisiones |
| Integración con SQL Server | 14 aprobadas, sin fallos ni omisiones |
| Contrato HTTP anterior | 32 rutas y sus permisos conservados |
| Navegador con API simulada | 3 aprobadas: comprobante, respuesta perdida y reconfirmación de precio |
| Cobertura de líneas VentaService | 142/156 (91,0 %) |
| Cobertura de líneas RecuperacionService | 50/53 (94,3 %) |
| Umbral JaCoCo | Aprobado, mínimo 75 % en ambos servicios |
| Bases SQL temporales al finalizar | 0 |
| Esquema local | Versión 7, sin migraciones adicionales en esta reorganización |
| Permisos DDL de la cuenta de aplicación | Retirados |

Las pruebas HTTP incluyen catálogo, ingreso de existencias, venta, consulta de comprobante tras cambio de precio, historial, reporte, baja lógica y actualización de perfil que invalida recuperaciones anteriores. La suite SQL también cubre concurrencia, rollback e idempotencia.

Evidencia local excluida de Git: `.cache/refactor-verificacion/surefire-reports`, `failsafe-reports`, `site/jacoco` y `.cache/revision/refactor-sql.log` (BUILD SUCCESS). Los contratos de regresión quedan versionados en `backend/src/test/resources/contracts/rutas.json`.

La suite frontend anterior registró seis pruebas unitarias aprobadas. En esta reorganización del backend no se modificó el comportamiento visual. Docker, HTTPS del dominio, entrega efectiva por Gmail y ejecución remota del workflow conservan sus requisitos de validación propios; estos resultados no los certifican.
