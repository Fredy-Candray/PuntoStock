param([Parameter(Mandatory=$true)][string]$Backup,[string]$Instance='localhost\SQLEXPRESS')
$ErrorActionPreference='Stop'
$taskBackup=(Resolve-Path -LiteralPath $Backup).Path
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskAllowed=[IO.Path]::GetFullPath((Join-Path $taskRoot '.cache/backups'))+[IO.Path]::DirectorySeparatorChar
if(!$taskBackup.StartsWith($taskAllowed,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetExtension($taskBackup) -ne '.bak'){throw 'Solo se admite un respaldo local de PuntoStock dentro de .cache/backups.'}
$taskName='PuntoStock_qa_'+[Guid]::NewGuid().ToString('N')
$taskDb=New-Object System.Data.SqlClient.SqlConnection("Server=$Instance;Database=master;Integrated Security=True;TrustServerCertificate=True;Connect Timeout=10")
$taskDb.Open();$taskCreated=$false
function Restore-Query([string]$Text){$taskCommand=$taskDb.CreateCommand();$taskCommand.CommandTimeout=180;$taskCommand.CommandText=$Text;return $taskCommand.ExecuteScalar()}
try {
 $taskServerBackupDir=[string](Restore-Query "SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS nvarchar(4000))")
 $taskSqlBackup=[IO.Path]::GetFullPath((Join-Path $taskServerBackupDir ([IO.Path]::GetFileName($taskBackup))))
 if(!$taskSqlBackup.StartsWith([IO.Path]::GetFullPath($taskServerBackupDir),[StringComparison]::OrdinalIgnoreCase) -or !(Test-Path -LiteralPath $taskSqlBackup)){throw 'No se encontró la copia del respaldo en el directorio SQL.'}
 if((Get-FileHash -LiteralPath $taskSqlBackup).Hash -ne (Get-FileHash -LiteralPath $taskBackup).Hash){throw 'Las copias local y SQL no coinciden.'}
 $taskDir=[string](Restore-Query "SELECT CAST(SERVERPROPERTY('InstanceDefaultDataPath') AS nvarchar(4000))")
 if(!$taskDir){throw 'No se identificó el directorio de datos de SQL Server.'}
 $taskCommand=$taskDb.CreateCommand();$taskCommand.CommandText="RESTORE FILELISTONLY FROM DISK=N'$($taskSqlBackup.Replace("'","''"))'"
 $taskAdapter=New-Object System.Data.SqlClient.SqlDataAdapter($taskCommand);$taskTable=New-Object System.Data.DataTable;[void]$taskAdapter.Fill($taskTable)
 if($taskTable.Rows.Count -ne 2){throw 'Se esperaba un respaldo de un archivo de datos y un log.'}
 $taskMoves=@();foreach($taskRow in $taskTable.Rows){$taskExtension=if($taskRow.Type -eq 'L'){'.ldf'}else{'.mdf'};$taskTarget=[IO.Path]::GetFullPath((Join-Path $taskDir ($taskName+$taskExtension)));if(!$taskTarget.StartsWith([IO.Path]::GetFullPath($taskDir),[StringComparison]::OrdinalIgnoreCase)){throw 'Ruta de restauración fuera del directorio SQL.'};$taskMoves+="MOVE N'$($taskRow.LogicalName.Replace("'","''"))' TO N'$($taskTarget.Replace("'","''"))'"}
 [void](Restore-Query "RESTORE DATABASE [$taskName] FROM DISK=N'$($taskSqlBackup.Replace("'","''"))' WITH CHECKSUM,$($taskMoves -join ',')")
 $taskCreated=$true;$taskDb.ChangeDatabase($taskName)
 [void](Restore-Query "DBCC CHECKDB (N'$taskName') WITH NO_INFOMSGS")
 $taskComparison=New-Object System.Data.SqlClient.SqlCommand("SELECT CASE WHEN (SELECT COUNT(*) FROM ventas)=(SELECT COUNT(*) FROM PuntoStock.dbo.ventas) AND (SELECT COUNT(*) FROM detalle_venta)=(SELECT COUNT(*) FROM PuntoStock.dbo.detalle_venta) AND (SELECT COALESCE(SUM(total),0) FROM ventas)=(SELECT COALESCE(SUM(total),0) FROM PuntoStock.dbo.ventas) AND NOT EXISTS(SELECT id,stock FROM productos EXCEPT SELECT id,stock FROM PuntoStock.dbo.productos) AND NOT EXISTS(SELECT id,stock FROM PuntoStock.dbo.productos EXCEPT SELECT id,stock FROM productos) THEN 1 ELSE 0 END",$taskDb)
 $taskMatch=[int]$taskComparison.ExecuteScalar()
 if($taskMatch -ne 1){throw 'El respaldo es íntegro, pero no coincide con el estado actual. Si hubo operaciones posteriores al respaldo, genera uno nuevo con el servicio detenido.'}
 Write-Output 'Restauración aislada aprobada: DBCC CHECKDB, ventas, detalles, total vendido y stock coinciden con PuntoStock.'
}finally{
 if($taskCreated -and $taskName -match '^PuntoStock_qa_[a-f0-9]{32}$'){$taskDb.ChangeDatabase('master');[void](Restore-Query "ALTER DATABASE [$taskName] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$taskName]")}
 $taskDb.Dispose()
}
