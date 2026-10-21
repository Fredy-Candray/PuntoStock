# Entorno local en SQL Server

PuntoStock usa la instancia **localhost\SQLEXPRESS** y una base separada **PuntoStock**. La preparación y las pruebas solo escriben en PuntoStock; no se modifican otras bases ni la configuración de las instancias SQL.

## Arranque

Ejecutar desde la raíz del repositorio con PowerShell 7. Los comandos permanecen en primer plano para ver sus logs; abrir una terminal para cada servidor.

```powershell
# Preparación inicial o comprobación de la configuración guardada:
./scripts/Initialize-Local.ps1

# Terminal del backend:
./scripts/Start-Backend.ps1

# Otra terminal, frontend:
./scripts/Start-Frontend.ps1
```

Si la terminal está dentro de `backend`, ejecutar `./start-local.cmd`. Dentro de `frontend`, usar `npm start` o `./start-local.cmd`. Los lanzadores reconocen un servidor que ya está activo y avisan en lugar de intentar ocupar de nuevo su puerto.

El lanzador del backend selecciona el JDK 21 instalado y carga la conexión SQL y los secretos locales. Un `mvn spring-boot:run` directo usa el JAVA_HOME de la terminal: si apunta a Java 17, no puede ejecutar las clases compiladas para Java 21. Para trabajar desde el IDE, seleccionar también JDK 21 como SDK del proyecto y JVM de Maven. El frontend selecciona el Node compatible de Codex; un `ng serve` directo todavía utiliza el Node del PATH de la terminal.

Abrir [PuntoStock local](http://127.0.0.1:14200). La API usa `http://127.0.0.1:18080/api`. Estos puertos evitan el 4200 ocupado por otra aplicación. Ambos servidores se enlazan solo a loopback y pueden detenerse con Ctrl+C en su terminal.

Se utiliza el JDK 21 instalado en `.jdks` y el Node 24 de las dependencias locales de Codex si está disponible. `Start-Frontend.ps1 -NodeExecutable ruta/al/node.exe` permite elegir otro Node compatible. Los puertos pueden indicarse con `-Port`; para el frontend el puerto de la API se configura con `-ApiPort`.

El JDK 21 de este Windows falla al conectar el socket AF_UNIX que usa su selector. El lanzador del backend configura una ruta sin directorio para ese socket temporal, activando el fallback TCP previsto por la implementación `PipeImpl` del JDK. Es un ajuste del proceso local; no cambia el JDK del sistema ni la aplicación de producción. El selector se verificó por separado antes de arrancar Tomcat.

## Usuarios y claves

Administrador: `admin.local`. Vendedor de prueba: `vendedor.local`, creado por la suite de integración.

Las contraseñas aleatorias, la cuenta SQL y el secreto JWT se guardan cifrados con DPAPI en `.cache/local/secrets.clixml`, excluido de Git y utilizable solo por el usuario de Windows que lo generó. Para consultar las claves de acceso a la interfaz:

```powershell
./scripts/Show-LocalCredentials.ps1
```

El campo SQL `usuarios.password_hash` contiene un hash BCrypt, no la contraseña para escribir en el login. La migración `database/002_password_hash_constraint.sql` impide guardar texto o hashes con formato inválido. Para cambiar la clave de la cuenta local sin mostrarla ni escribirla en SQL:

```powershell
./scripts/Set-LocalAdminPassword.ps1
```

Solicita la nueva contraseña y su confirmación de forma oculta, genera BCrypt, actualiza únicamente el administrador configurado y sincroniza la copia DPAPI. Requiere 12 caracteres por defecto; para una prueba local con al menos ocho puede indicarse `-AllowShortLocalPassword`. La API normal de creación de usuarios conserva su mínimo de 12 caracteres.

Si una base antigua contiene un hash inválido, `./scripts/Repair-LocalAdmin.ps1` lo repara usando la clave cifrada ya guardada, antes de aplicar la migración. No sobrescribe contraseñas BCrypt válidas. Consultar o copiar la clave después con los scripts locales de credenciales.

La cuenta SQL `puntostock_local_app` recibe únicamente SELECT, INSERT, UPDATE y DELETE en el esquema dbo de PuntoStock. La conexión usa TLS con certificado confiado explícitamente para esta prueba local. No se crean permisos de administrador SQL para la aplicación. El bootstrap de Spring crea el administrador solo si no hay usuarios.

El puerto SQL de SQLEXPRESS es dinámico. Después de reiniciar la instancia, ejecutar otra vez `Initialize-Local.ps1` antes de arrancar para actualizarlo. No borrar el archivo de secretos: contiene las claves que corresponden a los usuarios creados.

## Prueba de integración

Con los servidores iniciados:

```powershell
./scripts/Test-Local.ps1
```

La suite usa HTTP real y verifica también los registros persistidos en SQL Server. Crea productos con SKU `LOCAL-` y `RACE-`, entradas y ventas de demostración. Conserva esos datos para revisar el POS y el dashboard. Las ejecuciones siguientes generan SKU nuevos.

Incluye permisos, ocultación de costos, cálculo histórico de ganancia, concurrencia sobre la última unidad, revocación por desactivación y firma JWT inválida. Para probar rollback instala temporalmente un trigger que falla únicamente al vender el producto de esa ejecución y lo retira dentro de `finally`. No ejecutar simultáneamente con otra corrida de la suite. El reporte sin secretos se guarda en `docs/prueba-local-resultados.json`.

### Mantener sesión iniciada
En el login, marca **Mantener sesión iniciada** para conservar el acceso en el mismo navegador durante un máximo de 7 días, incluso después de cerrarlo. Sin marcarla, el acceso dura 15 minutos y se guarda solo para la pestaña. El vencimiento lo determina el JWT emitido por el backend; no se almacena la contraseña. Cerrar sesión elimina ambos almacenamientos. Usa siempre la misma dirección (localhost y 127.0.0.1 tienen almacenamientos diferentes).

### Tema claro y oscuro
El botón de luna/sol al pie del menú lateral alterna el tema. En móvil está junto al botón de cerrar sesión; en el login aparece en la esquina superior derecha. La preferencia se guarda en el navegador y se conserva al recargar o volver a abrir el sistema. Cambiar de tema no modifica la sesión ni los datos.

### Perfil y menú de usuario
Haz clic en tu nombre o avatar, en la esquina superior derecha, para abrir **Perfil** o **Cerrar sesión**. El perfil permite editar nombre, correo y teléfono, y subir, recortar, ampliar o quitar foto y portada. El usuario de acceso y los roles se consultan sin modificarlos desde este formulario. Cada petición usa la identidad autenticada; no acepta el ID de otro usuario.

Antes de desplegar esta versión sobre una base existente, aplica `database/003_user_profile.sql` con una cuenta de administración de PuntoStock. El iniciador de instalación local también la incluye. La migración agrega columnas opcionales, conserva credenciales y roles y no inventa fechas de alta para cuentas existentes. Las cuentas nuevas registran su fecha en UTC. La imagen seleccionada se recorta en el navegador y el backend valida el contenido JPEG/PNG (hasta 1 MB y 4 megapíxeles) antes de guardarlo en SQL Server.

### Papelería y servicios
Consulta `docs/operacion-papeleria.md` para preparar el catálogo, registrar existencias, cobrar, imprimir comprobantes y recuperar ventas pendientes. La actualización requiere `database/004_retail_pos.sql` antes de iniciar el backend nuevo. No modifica importes históricos ni inventa medios de pago de ventas anteriores.
