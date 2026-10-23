param([string]$Instance='localhost\SQLEXPRESS')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$localDir=Join-Path $root '.cache/local'
New-Item -ItemType Directory -Force $localDir | Out-Null
$secretFile=Join-Path $localDir 'secrets.clixml'
function Random-Secret { 'Ps!'+[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(30)) }
function Plain($value) { [Net.NetworkCredential]::new('', $value).Password }
if(Test-Path $secretFile){$settings=Import-Clixml $secretFile}else{
 $settings=[pscustomobject]@{Instance=$Instance;Database='PuntoStock';DbUser='puntostock_local_app';DbPassword=(ConvertTo-SecureString (Random-Secret) -AsPlainText -Force);JwtSecret=(ConvertTo-SecureString (Random-Secret) -AsPlainText -Force);AdminUser='admin.local';AdminPassword=(ConvertTo-SecureString (Random-Secret) -AsPlainText -Force);SellerUser='vendedor.local';SellerPassword=(ConvertTo-SecureString (Random-Secret) -AsPlainText -Force)}
 $settings | Export-Clixml $secretFile
}
if($settings.Instance -ne $Instance){throw 'La configuración local pertenece a otra instancia.'}
$connection=[System.Data.SqlClient.SqlConnection]::new("Server=$Instance;Database=master;Integrated Security=True;TrustServerCertificate=True;Connect Timeout=10")
$connection.Open()
function Sql([string]$text){$cmd=$connection.CreateCommand();$cmd.CommandTimeout=60;$cmd.CommandText=$text;[void]$cmd.ExecuteNonQuery()}
try {
 $cmd=$connection.CreateCommand();$cmd.CommandText="SELECT DB_ID(N'PuntoStock')";$exists=$cmd.ExecuteScalar()
 if($exists -is [DBNull]){
  Sql 'CREATE DATABASE [PuntoStock]'
  Sql "USE [PuntoStock]; EXEC sys.sp_addextendedproperty @name=N'PuntoStockLocal',@value=N'1';"
 }
 $connection.ChangeDatabase('PuntoStock')
 $cmd=$connection.CreateCommand();$cmd.CommandText="SELECT COUNT(*) FROM sys.extended_properties WHERE class=0 AND name=N'PuntoStockLocal' AND CAST(value AS nvarchar(10))=N'1'"
 if([int]$cmd.ExecuteScalar() -ne 1){throw 'La base existente no está identificada como entorno local de PuntoStock. No se modificó su contenido.'}
 $cmd=$connection.CreateCommand();$cmd.CommandText="SELECT COUNT(*) FROM sys.tables WHERE name='productos'"
 if([int]$cmd.ExecuteScalar() -eq 0){Sql (Get-Content (Join-Path $root 'database/001_schema.sql') -Raw)}
 Sql (Get-Content (Join-Path $root 'database/002_password_hash_constraint.sql') -Raw)
  Sql (Get-Content (Join-Path $root 'database/003_user_profile.sql') -Raw)
  Sql (Get-Content (Join-Path $root 'database/004_retail_pos.sql') -Raw)
 Sql (Get-Content (Join-Path $root 'database/005_password_recovery.sql') -Raw)
 Sql (Get-Content (Join-Path $root 'database/006_menu_administrable.sql') -Raw)
 $connection.ChangeDatabase('master')
 $password=(Plain $settings.DbPassword).Replace("'","''")
 Sql "IF SUSER_ID(N'puntostock_local_app') IS NULL CREATE LOGIN [puntostock_local_app] WITH PASSWORD=N'$password', CHECK_POLICY=ON, DEFAULT_DATABASE=[PuntoStock];"
 $connection.ChangeDatabase('PuntoStock')
 Sql "IF USER_ID(N'puntostock_local_app') IS NULL CREATE USER [puntostock_local_app] FOR LOGIN [puntostock_local_app]; GRANT SELECT,INSERT,UPDATE,DELETE ON SCHEMA::dbo TO [puntostock_local_app];"
 $cmd=$connection.CreateCommand();$cmd.CommandText="SELECT local_tcp_port FROM sys.dm_exec_connections WHERE session_id=@@SPID";$port=$cmd.ExecuteScalar()
 if($port -is [DBNull]){
  $cmd=$connection.CreateCommand();$cmd.CommandText="EXEC xp_readerrorlog 0,1,N'Server is listening on'";$adapter=[System.Data.SqlClient.SqlDataAdapter]::new($cmd);$table=[System.Data.DataTable]::new();[void]$adapter.Fill($table)
  $line=$table.Rows | Where-Object { $_.Text -match '<ipv4> (\d+)' } | Select-Object -First 1
  if(!$line -or $line.Text -notmatch '<ipv4> (\d+)'){throw 'No se encontró puerto TCP activo.'};$port=[int]$Matches[1]
 }
 $settings | Add-Member -NotePropertyName Port -NotePropertyValue ([int]$port) -Force
 $settings | Export-Clixml $secretFile
 Write-Output "Entorno preparado: $Instance / PuntoStock / puerto $port. Secretos protegidos con DPAPI para tu usuario Windows."
} finally {$connection.Dispose()}
