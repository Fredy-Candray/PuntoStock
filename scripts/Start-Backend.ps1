param([int]$Port=18080)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$settings=Import-Clixml (Join-Path $root '.cache/local/secrets.clixml')
function Plain($value){[Net.NetworkCredential]::new('', $value).Password}
$env:DB_URL="jdbc:sqlserver://localhost:$($settings.Port);databaseName=PuntoStock;encrypt=true;trustServerCertificate=true"
$env:DB_USER=$settings.DbUser
$env:DB_PASSWORD=Plain $settings.DbPassword
$env:JWT_SECRET=Plain $settings.JwtSecret
$env:BOOTSTRAP_USER=$settings.AdminUser
$env:BOOTSTRAP_PASSWORD=Plain $settings.AdminPassword
$env:SERVER_ADDRESS='127.0.0.1'
$env:SERVER_PORT="$Port"
$jdk=Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue | Where-Object Name -Match '(^|-)21\.' | Select-Object -First 1
if($jdk){$env:JAVA_HOME=$jdk.FullName}
Push-Location (Join-Path $root 'backend')
try {
 # En este Windows el socket AF_UNIX interno del JDK falla al conectar.
 # Una ruta sin directorio provoca el fallback TCP previsto por PipeImpl.
 & mvn -B '-Dmaven.repo.local=../.cache/m2' '-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=../.cache/local/no-unix-sockets' spring-boot:run
 if($LASTEXITCODE -ne 0){throw 'Falló el arranque de Spring Boot.'}
}
finally {Pop-Location; 'DB_PASSWORD','JWT_SECRET','BOOTSTRAP_PASSWORD' | ForEach-Object {Remove-Item "Env:$_" -ErrorAction SilentlyContinue}}
