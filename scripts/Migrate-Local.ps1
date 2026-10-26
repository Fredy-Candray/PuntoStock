param([string]$Instance='localhost\SQLEXPRESS',[switch]$ValidateOnly)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskSettings=Import-Clixml (Join-Path $taskRoot '.cache/local/secrets.clixml')
if($taskSettings.Instance -ne $Instance -or $taskSettings.Database -ne 'PuntoStock' -or $taskSettings.DbUser -ne 'puntostock_local_app'){throw 'La configuración no corresponde a PuntoStock local.'}
$taskDb=New-Object System.Data.SqlClient.SqlConnection("Server=$Instance;Database=PuntoStock;Integrated Security=True;TrustServerCertificate=True;Connect Timeout=10")
$taskDb.Open()
function Query-Migration([string]$Text){$taskCmd=$taskDb.CreateCommand();$taskCmd.CommandText=$Text;$taskCmd.CommandTimeout=120;return $taskCmd.ExecuteScalar()}
$taskGranted=$false
try {
 if([int](Query-Migration "SELECT COUNT(*) FROM sys.extended_properties WHERE class=0 AND name=N'PuntoStockLocal' AND CAST(value AS nvarchar(10))=N'1'") -ne 1){throw 'La base no está identificada como PuntoStock local.'}
 # Solo se adopta como versión 6 un esquema con las tablas, columnas y restricciones históricas presentes.
 if([int](Query-Migration "SELECT COUNT(*) FROM sys.tables WHERE name IN ('usuarios','roles','usuario_roles','productos','ventas','detalle_venta','movimientos_inventario','menu_configuracion')") -ne 8){throw 'Esquema histórico incompleto; no se hizo baseline.'}
 if([int](Query-Migration "SELECT CASE WHEN COL_LENGTH('usuarios','recuperacion_hash') IS NOT NULL AND COL_LENGTH('usuarios','auth_version') IS NOT NULL AND COL_LENGTH('ventas','solicitud_id') IS NOT NULL AND COL_LENGTH('detalle_venta','nombre_producto') IS NOT NULL AND COL_LENGTH('productos','stock_minimo') IS NOT NULL THEN 1 ELSE 0 END") -ne 1){throw 'Faltan columnas históricas; no se hizo baseline.'}
 if([int](Query-Migration "SELECT COUNT(*) FROM sys.check_constraints WHERE name IN ('ck_usuarios_password_bcrypt','ck_mov_stock','ck_mov_tipo') AND is_disabled=0 AND is_not_trusted=0") -ne 3){throw 'Faltan restricciones históricas habilitadas; no se hizo baseline.'}
 if([int](Query-Migration "SELECT COUNT(*) FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.ventas') AND name='uq_ventas_solicitud' AND is_unique=1 AND is_disabled=0") -ne 1){throw 'Falta la unicidad de solicitud de venta; no se hizo baseline.'}
 if([int](Query-Migration "SELECT COUNT(*) FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID('dbo.detalle_venta') AND name='uq_detalle'") -ne 1){throw 'Falta la unicidad de producto por venta; no se hizo baseline.'}
 if($ValidateOnly){Write-Output 'Prevalidación del esquema local aprobada; no se modificó la base.';return}
 $taskBackupDir=Join-Path $taskRoot '.cache/backups'
 New-Item -ItemType Directory -Path $taskBackupDir -Force | Out-Null
 $taskBackup=Join-Path $taskBackupDir ('PuntoStock-pre-estabilidad-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.bak')
 $taskServerBackupDir=[string](Query-Migration "SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS nvarchar(4000))")
 if(!$taskServerBackupDir){throw 'No se identificó la carpeta de respaldos de SQL Server.'}
 $taskServerBackup=[IO.Path]::GetFullPath((Join-Path $taskServerBackupDir ([IO.Path]::GetFileName($taskBackup))))
 if(!$taskServerBackup.StartsWith([IO.Path]::GetFullPath($taskServerBackupDir),[StringComparison]::OrdinalIgnoreCase)){throw 'Ruta de respaldo fuera del directorio SQL.'}
 $taskSafePath=$taskServerBackup.Replace("'","''")
 [void](Query-Migration "BACKUP DATABASE [PuntoStock] TO DISK=N'$taskSafePath' WITH COPY_ONLY,CHECKSUM,INIT")
 [void](Query-Migration "RESTORE VERIFYONLY FROM DISK=N'$taskSafePath' WITH CHECKSUM")
 Copy-Item -LiteralPath $taskServerBackup -Destination $taskBackup
 if((Get-FileHash -LiteralPath $taskServerBackup).Hash -ne (Get-FileHash -LiteralPath $taskBackup).Hash){throw 'La copia local del respaldo no coincide.'}
 if([int](Query-Migration "SELECT IS_ROLEMEMBER('db_ddladmin','puntostock_local_app')") -eq 1){throw 'El usuario ya tiene privilegios de migración inesperados. Revísalos antes de continuar.'}
 [void](Query-Migration 'ALTER ROLE db_ddladmin ADD MEMBER [puntostock_local_app]; GRANT REFERENCES ON SCHEMA::dbo TO [puntostock_local_app];')
 $taskGranted=$true
 $env:FLYWAY_URL="jdbc:sqlserver://127.0.0.1:$($taskSettings.Port);databaseName=PuntoStock;encrypt=true;trustServerCertificate=true"
 $env:FLYWAY_USER=$taskSettings.DbUser
 $env:FLYWAY_PASSWORD=[Net.NetworkCredential]::new('', $taskSettings.DbPassword).Password
 $env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-21.0.11'
 Push-Location (Join-Path $taskRoot 'backend')
 try{& mvn.cmd -B '-Dmaven.repo.local=../.cache/m2' '-Dflyway.locations=filesystem:src/main/resources/db/migration' '-Dflyway.baselineOnMigrate=true' '-Dflyway.baselineVersion=6' flyway:migrate;if($LASTEXITCODE -ne 0){throw 'La migración falló. Se conserva el respaldo; no iniciar la nueva versión.'}}finally{Pop-Location}
 Write-Output 'Migraciones aplicadas; respaldo verificado. No se borraron ventas, productos ni usuarios.'
}finally {
 if($taskGranted){[void](Query-Migration 'ALTER ROLE db_ddladmin DROP MEMBER [puntostock_local_app]; REVOKE REFERENCES ON SCHEMA::dbo FROM [puntostock_local_app];')}
 Remove-Item Env:FLYWAY_PASSWORD -ErrorAction SilentlyContinue
 Remove-Item Env:FLYWAY_USER -ErrorAction SilentlyContinue
 Remove-Item Env:FLYWAY_URL -ErrorAction SilentlyContinue
 $taskDb.Dispose()
}
