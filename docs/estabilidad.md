# Revisión de estabilidad - 26 de octubre de 2026

Esta entrega responde al catálogo H01-H20 de los informes aportados. Los cambios de código no certifican por sí solos una instalación de producción. La evidencia de cada ejecución se conserva en los reportes de pruebas, no en una cifra histórica copiada al README.

## Identidad y sesiones

El acceso JWT dura 15 minutos y se conserva únicamente en memoria. La renovación usa un secreto aleatorio, rotado en cada uso, en una cookie `HttpOnly; SameSite=Strict; Secure` en producción. SQL solo guarda su hash, la cuenta, su versión de acceso y el vencimiento absoluto. Cerrar sesión elimina ese registro e invalida inmediatamente los accesos de esa sesión. La renovación sin recordar dura como máximo ocho horas y su cookie no persiste al cerrar el navegador; recordando, el máximo absoluto es siete días, sin extensión indefinida.

Los cambios de contraseña, estado o rol conservan la revocación global mediante `authVersion`. Los tokens anteriores a esta entrega requieren iniciar sesión otra vez. No se migran secretos de `localStorage`; se eliminan las entradas antiguas. El carrito pendiente permanece en `sessionStorage` para resolver cobros inciertos.

Las operaciones de login, renovación y salida requieren `X-PuntoStock: 1`, además de la cookie en los dos últimos casos. La cookie es SameSite Strict y no se habilita CORS. Esto protege esas rutas contra formularios de otro origen; las demás mutaciones usan Bearer y no aceptan la cookie como autenticación. Mantener esas tres condiciones al cambiar el proxy. Las pruebas incluyen rechazo sin cabecera y revocación tras logout.

Los límites por IP del socket y por cuenta son temporales, acotados y devuelven 429. No se confía en X-Forwarded-For enviado por el visitante. Con múltiples réplicas debe agregarse un limitador compartido en el proxy: la memoria de cada proceso no se comparte. Un usuario inexistente también atraviesa una comparación BCrypt del mismo costo.

La consulta de autenticación excluye foto, portada y datos de recuperación. La revocación se consulta en cada petición; no se cachea un usuario activo durante minutos. El menú sí se agrupa y cachea hasta un minuto en el cliente, con recarga explícita al editarlo; nunca reemplaza los permisos de la API.

## Recuperación por correo

El cambio de correo utiliza la misma política desde perfil y administración: trim, minúsculas y cancelación de recuperación anterior. Los tokens son de un solo uso y vencen en quince minutos.

La transacción guarda el hash y una entrada de outbox. El destinatario y enlace pendiente se cifran con AES-GCM y clave derivada con separación de contexto del secreto del servidor. Un trabajador reclama entradas mediante una operación SQL atómica, libera el bloqueo y hace SMTP fuera de la transacción. Reintenta como máximo cinco veces con espera; omite cuentas inactivas, solicitudes reemplazadas y vencidas. Borra el contenido cifrado al enviar, cancelar, agotar intentos o vencer. Las entradas antiguas se purgan.

Los logs identifican el número de solicitud y la clase de error, sin dirección, contraseña, contenido ni token. Un 200 del formulario significa que se aceptó procesar la solicitud, no que el destinatario recibió el correo. Una aceptación SMTP tampoco garantiza la bandeja de entrada: revisar no deseado y el buzón correcto. El envío SMTP no ofrece exactamente una entrega: si el proceso cae después de enviar y antes de marcar la fila, un reintento puede duplicar el mismo correo; el enlace continúa siendo de un solo uso.

## Migraciones y respaldo

Flyway mantiene scripts V1-V7 y verifica sus checksums; Hibernate conserva `ddl-auto: validate`. No modificar scripts aplicados: crear V8 y siguientes. Los SQL de `database/001-006` son antecedentes del inicializador local; las nuevas migraciones de ejecución están en `backend/src/main/resources/db/migration`.

En una instalación local existente completa hasta 006, ejecutar `scripts/Migrate-Local.ps1`: verifica la marca de entorno, crea un respaldo COPY_ONLY con CHECKSUM, ejecuta VERIFYONLY, adopta explícitamente versión 6 y aplica las posteriores. Los permisos DDL se conceden temporalmente y se retiran en finally. No se usa baseline automático en el arranque normal. La cuenta de aplicación conserva DML, no db_owner ni db_ddladmin. Las migraciones fallidas deben revisarse; no ejecutar repair o clean para ocultar discrepancias.

La suite `scripts/Test-SqlIntegration.ps1` crea bases `PuntoStock_qa_<GUID>` y las elimina al terminar. Jamás usa PuntoStock para concurrencia, fallos inducidos ni modificación de checksums. Las migraciones solo admiten PuntoStock o ese patrón estricto de nombres de prueba. La suite de CI usa una instancia SQL Server efímera.

La restauración se ensaya con `scripts/Test-BackupRestore.ps1 -Backup <ruta>`. Solo admite .bak de `.cache/backups`, comprueba su hash contra la copia del directorio de respaldos de SQL Server, restaura a una base nueva, ejecuta DBCC CHECKDB y compara ventas, detalles, total vendido y stock con la base detenida. El respaldo se genera en la carpeta de SQL Server y se copia al proyecto: no se amplían permisos de OneDrive para que el servicio SQL acceda a carpetas personales. VERIFYONLY no sustituye esta restauración. Acordar frecuencia, copia externa, retención y objetivos RPO/RTO según el uso real del negocio.

## Datos, contratos y capacidad

Productos, usuarios y movimientos tienen rutas de página y filtros en servidor. Las pantallas administrativas las utilizan y permiten consultar movimientos antiguos. Las rutas de lista se mantienen para compatibilidad con dashboard, selector de inventario y POS; en esas pantallas aún se carga el catálogo completo. Antes de un catálogo masivo, medir y trasladar esas consultas a selección por código, búsqueda y resúmenes agregados. No atribuir una mejora de latencia medida sin comparar el mismo volumen y entorno.

El historial de ventas conserva sus páginas de veinte y sustituye cinco consultas de totales por una agrupación por medio de pago. Se carga el vendedor con EntityGraph; no se hace fetch join de una colección paginada. Productos y movimientos tienen DTO de salida; los costos no se serializan para vendedor. El error HTTP incluye código, mensaje, campos e identificador, preservando `message` para clientes existentes.

El catálogo tiene un servicio propio; el coordinador de ventas conserva una transacción única y orden de bloqueo. Los cambios de existencias pasan por `Producto.aplicarMovimiento`. Se centraliza la zona de negocio y se inyecta reloj en venta y recuperación. Los nombres de pago y tipos mantienen los valores SQL existentes; no se cambian datos históricos por una refactorización de nombres.

## Operación y despliegue

`/actuator/health`, `/liveness` y `/readiness` no exponen detalles. Readiness incluye la base. Los identificadores de correlación son generados por el servidor y se devuelven como X-Request-ID. El perfil prod ofrece logs JSON; no registrar cuerpos de login, cookies, JWT o enlaces. Revisar métricas de error/latencia, bloqueos y estado FALLIDO de outbox sin publicar credenciales ni destinatarios.

Java 21 y Node 24 son las versiones de referencia. El lanzador local usa el Node compatible del runtime; el CI instala las versiones declaradas. PowerShell es una facilidad local, no una dependencia del código Java/Angular. Dockerfiles y compose describen una alternativa de despliegue; no sustituyen configurar la base, migraciones y HTTPS. Docker no está instalado en el equipo de revisión: los contenedores deben validarse en un host Docker antes de producción.

El proxy Nginx incluye CSP, nosniff, política de referer, bloqueo de marcos y permisos de navegador. TLS y HSTS deben configurarse en el terminador HTTPS real; no anunciar HSTS desde un entorno HTTP de pruebas. Ejecutar `Test-SecurityHeaders.ps1 -Url https://<dominio>` y comprobar cookies Secure tras el despliegue. Los valores por defecto de Spring también se verifican en integración HTTP.

Para rotar JWT_SECRET: detener instancias y trabajador, cancelar las recuperaciones pendientes (el outbox cifrado depende de la clave), generar un nuevo secreto criptográfico de al menos 32 bytes en el gestor de secretos, borrar sesiones, actualizar todas las instancias y arrancar. Todos deben iniciar sesión y solicitar enlaces nuevos. No guardar secretos en Git ni archivos públicos. Una transición sin cerrar sesiones necesita un diseño de llaves con identificador, no dos secretos probados indiscriminadamente.

Dependabot revisa npm, Maven y Actions. CI ejecuta análisis estático, formato, pruebas, compilación y auditoría npm. CodeQL y dependency-review se habilitan para repositorios públicos; en uno privado requieren la disponibilidad y habilitación del producto de GitHub correspondiente. No afirmar que un workflow ya pasó sin la ejecución remota.

## Límites financieros

Ganancia significa **ganancia bruta**, con el costo histórico de cada línea: (precio vendido - costo capturado) por cantidad. No es utilidad neta ni incluye gastos, impuestos, descuentos o reembolsos. Una devolución manual de inventario no revierte la venta ni el dinero. La interfaz lo expresa.

Impuestos/documentos fiscales, devoluciones financieras, cierre de caja, compras y valoración FIFO/promedio son capacidades nuevas. Requieren reglas comerciales y, cuando aplique, revisión contable/fiscal; no se implementan como si fueran correcciones mecánicas de seguridad.
