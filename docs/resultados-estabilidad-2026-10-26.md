# Evidencia de estabilidad - 26 de octubre de 2026

Ejecución local en Windows, Java 21, Node 24.22.1 y SQL Server Express 2022. Es una fotografía de esta revisión, no un contador permanente de calidad. Cambios locales sin publicar en GitHub.

| Comprobación | Resultado |
|---|---|
| Unitarias backend | 61 aprobadas, cero fallos/errores/omisiones |
| Integración SQL | 11 aprobadas en bases descartables, cero fallos/errores/omisiones |
| Unitarias frontend | 6 aprobadas |
| Navegador | 3 aprobadas: cobro, respuesta perdida/recarga, reconfirmación de precio |
| Formato, ESLint y tipos | Aprobados |
| Compilación Angular | Aprobada; advertencias CommonJS de dependencias opcionales de PDF |
| Auditoría npm | Cero vulnerabilidades reportadas tras los parches de Angular/Vite |
| Cobertura crítica | Umbral 75% de líneas aprobado en venta y recuperación |
| Migración desde cero | V1-V7 aprobadas en base vacía aislada |
| Migración existente | Baseline 6 y V7 conservan datos; checksum alterado se detecta |
| Respaldo local | COPY_ONLY/CHECKSUM, VERIFYONLY y copia coincidente |
| Restauración aislada | DBCC CHECKDB y conciliación de ventas/detalles/total/stock aprobadas |
| Disponibilidad local | Interfaz, health y readiness responden HTTP 200 |

La prueba SQL de outbox utiliza un emisor simulado y la tabla real: rollback no deja mensajes, envío ocurre después del commit, el contenido cifrado se borra al finalizar y una solicitud reemplazada no se envía. No acredita recepción en el buzón de un usuario.

Las pruebas SQL también comprueban la última unidad concurrente, UUID idéntico/diferente, rollback de venta/detalle/stock, precios históricos, 401/403, revocación, CSRF, páginas y filtros, y que la autenticación no consulta fotos ni portadas. La base del negocio no se usa para estos casos.

Los tres escenarios de navegador usan una API simulada. En conjunto con SQL ServerIT aportan evidencia de UI y persistencia, pero no son una prueba de carga ni de un despliegue HTTPS.

Cobertura observada: OperacionService 151/194 líneas, RecuperacionService 50/53, Sesiones 80/83. El umbral se aplica a los dos primeros servicios; la cobertura no sustituye los casos de concurrencia y rollback.

No se ejecutó el workflow remoto ni se construyeron contenedores: Docker no está instalado en este equipo. HTTPS/HSTS, alertas externas y objetivos RPO/RTO dependen del entorno de operación. Consultar [seguimiento de hallazgos](hallazgos-resolucion.md) para los pendientes y la refactorización bloqueada por revisión automática.
