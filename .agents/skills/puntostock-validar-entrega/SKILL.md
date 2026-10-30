---
name: puntostock-validar-entrega
description: Valida una entrega o un cambio de PuntoStock seleccionando compilación, formato y pruebas pertinentes, y reporta evidencia. Úsalo al pedir validación de entrega o al cerrar cambios de código, pruebas o configuración; no para explicaciones o documentación sin cambios de comportamiento.
---

# Validar una entrega de PuntoStock

## Elegir comprobaciones según el cambio

- Lee frontend/package.json, backend/pom.xml y .github/workflows/ci.yml. Son la fuente de comandos vigente; no supongas que los comandos de informes pasados siguen siendo suficientes.
- Comprueba Java, Maven y Node usados por el proceso. Respeta las versiones declaradas; si el PATH apunta a otro runtime, utiliza uno compatible disponible sin cambiar configuración global.
- Para backend, selecciona pruebas relacionadas y comprueba formato cuando corresponda. El CI actual usa mvn -B spotless:check verify desde backend; verify incluye comprobaciones que mvn test no cubre.
- Para frontend, selecciona entre npm run format:check, npm run lint, npm test y npm run build desde frontend. Ejecuta npm run test:e2e cuando afecte flujos de usuario y estén preparados sus requisitos; consulta la configuración de Playwright antes de arrancar servicios.
- Un cambio solo de documentación o skills requiere revisar sus archivos y estructura, no levantar SQL ni ejecutar toda la aplicación.

## Integración y límites

- Inspecciona SqlServerIT y scripts/Test-SqlIntegration.ps1 antes de ejecutar integración. Usa únicamente una base de pruebas aislada y desechable identificada como tal; no redirijas pruebas a la base del negocio.
- Para ventas, identidad o migraciones, diferencia cobertura unitaria de comprobaciones reales de SQL, HTTP y navegador. Un escenario omitido por falta de configuración no cuenta como aprobado.
- No ejecutes scripts de contraseñas, restauración, migración de una base existente o preparación administrativa como un chequeo rutinario. Comprueba alcance y destino antes de cualquier operación con efectos sobre datos.
- Ante un fallo de permisos o dependencias, busca una alternativa dentro del entorno permitido. No reduzcas aserciones, desactives controles de seguridad ni omitas fallos para obtener un resultado verde.
- Informa comandos ejecutados, resultado, pruebas omitidas y motivo. Atribuye los resultados a esta ejecución; no uses reportes anteriores como si fueran nuevos.
