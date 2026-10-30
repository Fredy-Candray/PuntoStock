---
name: puntostock-documentar-proyecto
description: Documenta arquitectura, API, decisiones, hallazgos o mejoras de PuntoStock, o consolida informes del proyecto sin duplicar ideas. Úsalo para documentación técnica solicitada, no para modificar funcionalidades.
---

# Documentar PuntoStock

- Identifica destinatario, alcance y formato pedidos; si no se indica formato, usa Markdown en docs con nombre descriptivo. Si se pide PDF, utiliza la habilidad PDF disponible para generar y revisar visualmente el resultado.
- Inspecciona manifiestos, código y configuración actuales. Describe arquitectura, patrones y comportamientos con evidencia de archivos; no atribuyas patrones solo por utilizar un framework.
- Distingue estado implementado, resultado realmente ejecutado, evidencia histórica y propuesta pendiente. No presentes las recomendaciones de informes anteriores como carencias actuales sin contrastarlas.
- Cuando consolides revisiones, agrupa cada problema una sola vez con evidencia, impacto, prioridad, acción y criterio de aceptación. Referencia sus identificadores desde la hoja de ruta en lugar de repetir las recomendaciones.
- Resuelve discrepancias con el código cuando sea posible y deja constancia breve de la corrección. Si no puede verificarse una afirmación, indícalo; no inventes resultados ni plazos.
- Mantén una distinción entre operación local, CI y despliegue productivo. Un contenedor usado por CI no demuestra que toda la aplicación se despliegue con Docker.
- Incluye versiones observadas, fecha de revisión y fuentes suficientes para reproducir la lectura. Para afirmar requisitos fiscales o legales actuales consulta fuentes oficiales; si no se estudian, delimita ese alcance.
- No publiques credenciales, tokens, datos de clientes ni contenido de archivos locales de secretos. No ejecutes operaciones de negocio para documentarlas.
