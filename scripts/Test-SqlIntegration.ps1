param([string]$Instance='localhost\SQLEXPRESS',[string]$BuildDirectory='')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskBuildArgs=@()
if($BuildDirectory){
 if($BuildDirectory -notmatch '^\.cache/[a-zA-Z0-9_-]+$'){throw 'La salida de verificación debe estar en una subcarpeta de .cache.'}
 $taskBuildArgs=@("-Dpuntostock.build-dir=../$BuildDirectory")
}
$taskSettings=Import-Clixml (Join-Path $taskRoot '.cache/local/secrets.clixml')
if($taskSettings.Instance -ne $Instance){throw 'La instancia no coincide con la configuración local.'}
$taskName='PuntoStock_qa_'+[Guid]::NewGuid().ToString('N')
if($taskName -notmatch '^PuntoStock_qa_[a-f0-9]{32}$'){throw 'Nombre de prueba inválido.'}
$taskConnection=New-Object System.Data.SqlClient.SqlConnection("Server=$Instance;Database=master;Integrated Security=True;TrustServerCertificate=True;Connect Timeout=10")
$taskConnection.Open()
function Invoke-TestSql([string]$Text){$taskCommand=$taskConnection.CreateCommand();$taskCommand.CommandText=$Text;$taskCommand.CommandTimeout=60;[void]$taskCommand.ExecuteNonQuery()}
$taskCreated=$false
$taskBaselineCreated=$false
$taskBaselineName='PuntoStock_qa_'+[Guid]::NewGuid().ToString('N')
try {
 Invoke-TestSql "CREATE DATABASE [$taskName]";$taskCreated=$true
 $taskConnection.ChangeDatabase($taskName)
 # Privilegios de migración solo en la base desechable, nunca en PuntoStock.
 if($taskSettings.DbUser -ne 'puntostock_local_app'){throw 'Usuario de pruebas inesperado.'}
 Invoke-TestSql 'CREATE USER [puntostock_local_app] FOR LOGIN [puntostock_local_app]; ALTER ROLE db_owner ADD MEMBER [puntostock_local_app];'
 $taskConnection.ChangeDatabase('master')
 Invoke-TestSql "CREATE DATABASE [$taskBaselineName]";$taskBaselineCreated=$true
 $taskConnection.ChangeDatabase($taskBaselineName)
 Invoke-TestSql 'CREATE USER [puntostock_local_app] FOR LOGIN [puntostock_local_app]; ALTER ROLE db_owner ADD MEMBER [puntostock_local_app];'
 $env:SQL_BASELINE_URL="jdbc:sqlserver://127.0.0.1:$($taskSettings.Port);databaseName=$taskBaselineName;encrypt=true;trustServerCertificate=true"
 $env:SQL_TEST_URL="jdbc:sqlserver://127.0.0.1:$($taskSettings.Port);databaseName=$taskName;encrypt=true;trustServerCertificate=true"
 $env:SQL_TEST_USER=$taskSettings.DbUser
 $env:SQL_TEST_PASSWORD=[Net.NetworkCredential]::new('', $taskSettings.DbPassword).Password
 $env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-21.0.11'
 Push-Location (Join-Path $taskRoot 'backend')
 try {& mvn.cmd -B '-Dmaven.repo.local=../.cache/m2' @taskBuildArgs verify; if($LASTEXITCODE -ne 0){throw 'Fallaron las pruebas SQL.'}}finally{Pop-Location}
 Write-Output 'Integración SQL completada en una base aislada. La base del negocio no se modificó.'
}finally {
 Remove-Item Env:SQL_TEST_PASSWORD -ErrorAction SilentlyContinue
 Remove-Item Env:SQL_TEST_USER -ErrorAction SilentlyContinue
 Remove-Item Env:SQL_TEST_URL -ErrorAction SilentlyContinue
 Remove-Item Env:SQL_BASELINE_URL -ErrorAction SilentlyContinue
 if($taskCreated -and $taskName -match '^PuntoStock_qa_[a-f0-9]{32}$'){
  $taskConnection.ChangeDatabase('master')
  Invoke-TestSql "ALTER DATABASE [$taskName] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$taskName]"
 }
 if($taskBaselineCreated -and $taskBaselineName -match '^PuntoStock_qa_[a-f0-9]{32}$'){$taskConnection.ChangeDatabase('master');Invoke-TestSql "ALTER DATABASE [$taskBaselineName] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$taskBaselineName]"}
 $taskConnection.Dispose()
}
