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

Abrir [PuntoStock local](http://127.0.0.1:14200). La API usa `http://127.0.0.1:18080/api`. Estos puertos evitan el 4200 ocupado por otra aplicación. Ambos servidores se enlazan solo a loopback y pueden detenerse con Ctrl+C en su terminal.

Se utiliza el JDK 21 instalado en `.jdks` y el Node 24 de las dependencias locales de Codex si está disponible. `Start-Frontend.ps1 -NodeExecutable ruta/al/node.exe` permite elegir otro Node compatible. Los puertos pueden indicarse con `-Port`; para el frontend el puerto de la API se configura con `-ApiPort`.

El JDK 21 de este Windows falla al conectar el socket AF_UNIX que usa su selector. El lanzador del backend configura una ruta sin directorio para ese socket temporal, activando el fallback TCP previsto por la implementación `PipeImpl` del JDK. Es un ajuste del proceso local; no cambia el JDK del sistema ni la aplicación de producción. El selector se verificó por separado antes de arrancar Tomcat.

## Usuarios y claves

Administrador: `admin.local`. Vendedor de prueba: `vendedor.local`, creado por la suite de integración.

Las contraseñas aleatorias, la cuenta SQL y el secreto JWT se guardan cifrados con DPAPI en `.cache/local/secrets.clixml`, excluido de Git y utilizable solo por el usuario de Windows que lo generó. Para consultar las claves de acceso a la interfaz:

```powershell
./scripts/Show-LocalCredentials.ps1
```

La cuenta SQL `puntostock_local_app` recibe únicamente SELECT, INSERT, UPDATE y DELETE en el esquema dbo de PuntoStock. La conexión usa TLS con certificado confiado explícitamente para esta prueba local. No se crean permisos de administrador SQL para la aplicación. El bootstrap de Spring crea el administrador solo si no hay usuarios.

El puerto SQL de SQLEXPRESS es dinámico. Después de reiniciar la instancia, ejecutar otra vez `Initialize-Local.ps1` antes de arrancar para actualizarlo. No borrar el archivo de secretos: contiene las claves que corresponden a los usuarios creados.

## Prueba de integración

Con los servidores iniciados:

```powershell
./scripts/Test-Local.ps1
```

La suite usa HTTP real y verifica también los registros persistidos en SQL Server. Crea productos con SKU `LOCAL-` y `RACE-`, entradas y ventas de demostración. Conserva esos datos para revisar el POS y el dashboard. Las ejecuciones siguientes generan SKU nuevos.

Incluye permisos, ocultación de costos, cálculo histórico de ganancia, concurrencia sobre la última unidad, revocación por desactivación y firma JWT inválida. Para probar rollback instala temporalmente un trigger que falla únicamente al vender el producto de esa ejecución y lo retira dentro de `finally`. No ejecutar simultáneamente con otra corrida de la suite. El reporte sin secretos se guarda en `docs/prueba-local-resultados.json`.
