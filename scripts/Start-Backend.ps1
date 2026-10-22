param([int]$Port=18080)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
try {
 $existing=Invoke-WebRequest "http://127.0.0.1:$Port/api/productos" -SkipHttpErrorCheck -TimeoutSec 3
} catch {$existing=$null}
if($existing){
 if($existing.StatusCode -in @(200,401)){
  Write-Output "La API ya responde en http://127.0.0.1:$Port/api (HTTP $($existing.StatusCode)). No se inició una segunda instancia."
  return
 }
 throw "El puerto $Port ya responde con HTTP $($existing.StatusCode). Revise ese servidor o use -Port."
}
$settings=Import-Clixml (Join-Path $root '.cache/local/secrets.clixml')
function Plain($value){[Net.NetworkCredential]::new('', $value).Password}
$mailSettingsPath=Join-Path $root '.cache/local/mail.clixml'
if((Test-Path $mailSettingsPath) -and !$env:MAIL_USERNAME){
 $mailSettings=Import-Clixml $mailSettingsPath
 $env:MAIL_USERNAME=$mailSettings.Username
 $env:MAIL_PASSWORD=Plain $mailSettings.Password
 $env:APP_PUBLIC_URL=$mailSettings.PublicUrl
 $env:RECOVERY_ENABLED='true'
}
$env:DB_URL="jdbc:sqlserver://localhost:$($settings.Port);databaseName=PuntoStock;encrypt=true;trustServerCertificate=true"
$env:DB_USER=$settings.DbUser
$env:DB_PASSWORD=Plain $settings.DbPassword
$env:JWT_SECRET=Plain $settings.JwtSecret
$env:BOOTSTRAP_USER=$settings.AdminUser
$env:BOOTSTRAP_PASSWORD=Plain $settings.AdminPassword
$env:SERVER_ADDRESS='127.0.0.1'
$env:SERVER_PORT="$Port"
$jdk=Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue | Where-Object Name -Match '(^|-)21\.' | Select-Object -First 1
if(!$jdk){throw 'No se encontró JDK 21 en .jdks. Instálalo antes de iniciar el backend.'}
$env:JAVA_HOME=$jdk.FullName
$javaExecutable=Join-Path $jdk.FullName 'bin/java.exe'
if(!(Test-Path $javaExecutable)){throw 'El JDK 21 seleccionado no contiene bin/java.exe.'}
Write-Output "PuntoStock usará JDK 21: $($jdk.FullName)"
Push-Location (Join-Path $root 'backend')
try {
 # En este Windows el socket AF_UNIX interno del JDK falla al conectar.
 # Una ruta sin directorio provoca el fallback TCP previsto por PipeImpl.
 & mvn -B '-Dmaven.repo.local=../.cache/m2' '-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=../.cache/local/no-unix-sockets' spring-boot:run
 if($LASTEXITCODE -ne 0){throw 'Falló el arranque de Spring Boot.'}
}
finally {Pop-Location; 'DB_PASSWORD','JWT_SECRET','BOOTSTRAP_PASSWORD','MAIL_PASSWORD' | ForEach-Object {Remove-Item "Env:$_" -ErrorAction SilentlyContinue}}
