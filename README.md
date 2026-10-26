# PuntoStock

Base funcional de un sistema de ventas e inventario en monorepo. Backend Spring Boot 4.1.1 / Java 21 / Maven, frontend Angular 22 standalone / Angular Material y SQL Server. Versión inicial para desarrollo; consulte las limitaciones y validaciones pendientes más abajo.

## Contenido

- `database/001_schema.sql`: DDL inicial. Las ampliaciones `002_password_hash_constraint.sql`, `003_user_profile.sql` y `004_retail_pos.sql` agregan validación BCrypt, perfiles y el catálogo/cobro para papelería con servicios.
- `backend/src/main/java/sv/puntostock`: capas `api`, `service`, `repository`, `entity` y `security`.
- `frontend/src/app`: componentes Login, Dashboard, POS, Productos e Inventario; servicios HTTP, interceptor y guards.
- `docs/arquitectura.md`: reglas, contratos de API, decisiones y plan de validación.
- `docs/git.md`: flujo local y conexión con GitHub.
- `docs/interfaz.md`: rediseño visual, validaciones en navegador e instrucciones para copiar la clave local del administrador.

## Configuración local (PowerShell)

**Prueba local realizada:** SQL Server `localhost\SQLEXPRESS`, base separada `PuntoStock`, interfaz `http://127.0.0.1:14200` y API en el puerto 18080. Consulte `docs/prueba-local.md` para reproducir el arranque, consultar los usuarios de prueba y revisar las 41 comprobaciones de integración. Las claves están protegidas localmente y excluidas de Git.

Requisitos: JDK 21, Maven >=3.6.3, Node 22.22.3+ o 24.15+ compatible con Angular 22, npm y SQL Server accesible. No se incluyen credenciales ni contraseñas predeterminadas.

Para alinear la instalación de Java en Windows, ejecuta `pwsh -NoProfile -File scripts/Set-Java21.ps1`. El parámetro `-SystemPath` requiere una terminal como administrador y prioriza JDK 21 en el PATH del sistema. Se conserva un respaldo local en `.cache/local`. Cierra y abre terminales e IDE después del cambio; `java -version`, `javac -version` y `mvn -version` deben mostrar Java 21. En IntelliJ selecciona el SDK `ms-21` del proyecto y ese mismo JDK para Maven. El archivo `.java-version` declara la versión mayor del proyecto.


1. Para este entorno Windows, ejecutar `scripts/Initialize-Local.ps1` y después `scripts/Migrate-Local.ps1`. La migración crea y verifica un respaldo antes de adoptar explícitamente la versión histórica 6 y actualizar. Para una base vacía en otro entorno, ejecutar Flyway V1-V7 con una cuenta de migración; la cuenta de aplicación conserva solo DML. No activar baseline automático ni editar migraciones aplicadas. Consultar `docs/estabilidad.md`.
2. Configurar las variables en la terminal del backend (sustituir los valores):

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=PuntoStock;encrypt=true;trustServerCertificate=false'
$env:DB_USER='puntostock_app'
$env:DB_PASSWORD='<clave SQL Server>'
$env:JWT_SECRET='<secreto aleatorio de al menos 32 bytes>'
$env:BOOTSTRAP_USER='administrador'
$env:BOOTSTRAP_PASSWORD='<clave inicial de 12 caracteres o más, máximo 72 bytes>'
Set-Location backend
mvn test
mvn spring-boot:run
```

El bootstrap crea un administrador únicamente si no hay usuarios. Tras crearlo, retirar las variables `BOOTSTRAP_*` para siguientes arranques. En desarrollo con certificado SQL autofirmado puede usarse `trustServerCertificate=true`; en producción configurar una cadena de confianza válida. `.env` está ignorado, pero Spring no lo carga automáticamente: exportar variables en el proceso.

3. En otra terminal, desde la raíz:

```powershell
Set-Location frontend
npm install
npm run build
npm start
```

Con `npm start`, abrir `http://127.0.0.1:14200`; el lanzador configura `/api` al puerto 18080. Un `ng serve` sin el lanzador usa los valores predeterminados 4200/8080 y no representa el entorno local configurado. Para producción servir Angular y `/api` bajo el mismo origen mediante proxy HTTPS. No se necesita CORS en este diseño.

4. Entrar con el administrador inicial, crear productos, registrar entradas `COMPRA` y registrar una venta en POS. Revisar movimiento de salida y ganancia en Dashboard. La sección Administración incluye Usuarios y Administrar menú; las migraciones incluyen la configuración de menú. Consultar `docs/administracion.md`.

## Seguridad y alcance

JWT HS256 con emisor validado: acceso de quince minutos en memoria y renovación rotada mediante cookie HttpOnly, SameSite Strict y Secure en producción. SQL guarda solo el hash de la renovación. Cerrar sesión revoca el acceso de esa sesión; authVersion conserva revocación global. El cambio de correo cancela enlaces anteriores y login/recuperación tienen límites temporales. Ver `docs/estabilidad.md` para CSRF, rotación y operación.

El alta de producto parte con stock cero; las entradas y ajustes son el único camino administrativo para modificar existencias. La baja es lógica para preservar historial. Los precios se consultan en cada petición y el POS los relee después de una venta; no hay difusión instantánea entre sesiones mediante WebSocket/SSE. El servidor confirma siempre con los precios vigentes y calcula importes con `BigDecimal`.

La ganancia es **bruta de ventas**, no utilidad contable neta: suma `(precio vendido - costo capturado) × cantidad`; no incluye impuestos, descuentos, gastos ni reversos financieros de devoluciones. Los movimientos manuales de devolución afectan inventario solamente. Se conserva costo vigente de catálogo, sin FIFO ni costo promedio ponderado. Moneda asumida USD, cantidades enteras y precisión monetaria de dos decimales.

## Validación

Pruebas unitarias incluidas para cálculo histórico, stock insuficiente, líneas duplicadas, devoluciones y límites UTC del día en El Salvador. También se ejecutaron 41 comprobaciones de integración contra SQL Server: ventas concurrentes sobre la última unidad, rollback completo ante fallos y respuestas 401/403 por rol, entre otras. Hibernate validó el esquema real. El alcance y las condiciones de reproducción están en `docs/validacion.md`.

Los conteos históricos no describen una ejecución nueva. Para resultados actuales, ejecutar la suite y consultar los reportes Surefire/Failsafe/JaCoCo, Vitest y Playwright. `scripts/Test-SqlIntegration.ps1` crea bases desechables y comprueba concurrencia, rollback, idempotencia, RBAC, sesiones y migraciones; no usa ventas reales.

La recuperación de contraseña por Gmail requiere aplicar `database/005_password_recovery.sql` y configurar el remitente con `scripts/Set-LocalMail.ps1`; consultar `docs/recuperacion-contrasena.md`. Sin esa configuración, el envío permanece desactivado. Los enlaces son de un solo uso, vencen en 15 minutos e invalidan las sesiones anteriores al restablecer la contraseña.

Las correcciones y límites pendientes se detallan en `docs/estabilidad.md`. Las devoluciones financieras, impuestos y cierre de caja son capacidades nuevas que requieren reglas comerciales.

## Versiones verificadas

Referencias oficiales consultadas el 15 de octubre de 2026:

- [Spring Boot: requisitos y versión estable 4.1.1](https://docs.spring.io/spring-boot/system-requirements.html).
- [Angular: compatibilidad de Angular 22, Node y TypeScript 6](https://angular.dev/reference/versions).

Los parches Angular se resuelven en la serie 22.2, con parche de Vite verificado en el lockfile. `package-lock.json` conserva la resolución instalada y CI usa `npm ci`. El archivo `.npmrc` aplica modo compatible para evitar un fallo interno de npm 10 con los peers opcionales del builder; Angular y TypeScript están declarados explícitamente.
