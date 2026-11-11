param([Parameter(Mandatory=$true)][string]$Direccion,[ValidateRange(1024,65535)][int]$Puerto=14201)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$ip=$null
if(![Net.IPAddress]::TryParse($Direccion,[ref]$ip) -or $ip.AddressFamily -ne 'InterNetwork'){throw 'Usa una IPv4 privada de esta computadora.'}
$b=$ip.GetAddressBytes()
if(!($b[0] -eq 10 -or ($b[0] -eq 172 -and $b[1] -ge 16 -and $b[1] -le 31) -or ($b[0] -eq 192 -and $b[1] -eq 168))){throw 'La dirección debe pertenecer a una red privada.'}
if(!(Get-NetIPAddress -AddressFamily IPv4 -IPAddress $Direccion -ErrorAction SilentlyContinue)){throw 'La IP no pertenece a esta computadora.'}
try {
 $health=Invoke-WebRequest 'http://127.0.0.1:18080/actuator/health/readiness' -UseBasicParsing -TimeoutSec 10
 if($health.StatusCode -ne 200){throw 'API no disponible'}
}catch{throw 'Inicia primero el backend local de PuntoStock en el puerto 18080.'}
$url="http://${Direccion}:$Puerto/"
try{$existing=Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 3}catch{$existing=$null}
if($existing){
 if($existing.Content -match '<title>PuntoStock</title>'){Write-Output "PuntoStock ya está disponible: $url";return}
 throw 'Ese puerto responde con otra aplicación.'
}
$node=Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
if(!(Test-Path -LiteralPath $node)){$node='node'}
$proxy=Join-Path $root '.cache/local/proxy-wifi.json'
New-Item -ItemType Directory -Force (Split-Path $proxy) | Out-Null
$json=@{'/api'=@{target='http://127.0.0.1:18080';secure=$false;changeOrigin=$true}} | ConvertTo-Json -Depth 3
[IO.File]::WriteAllText($proxy,$json,[Text.UTF8Encoding]::new($false))
$env:NG_CLI_ANALYTICS='false'
$env:npm_config_offline='true'
$apk=Join-Path $root '.cache/android-local/PuntoStock-local.apk'
if(Test-Path -LiteralPath $apk){
 $download=Join-Path $root 'frontend/public/apk-local'
 New-Item -ItemType Directory -Force $download | Out-Null
 Copy-Item -LiteralPath $apk -Destination (Join-Path $download 'PuntoStock-local.apk') -Force
 Write-Output "APK para descargar en el teléfono: ${url}apk-local/PuntoStock-local.apk"
}
Write-Output "Dirección para el teléfono: $url"
Write-Output 'Se atenderá solo en esta IP privada. Mantén esta terminal abierta mientras pruebas.'
Push-Location (Join-Path $root 'frontend')
try{
 & $node node_modules/@angular/cli/bin/ng.js serve --host $Direccion --port $Puerto --proxy-config $proxy --poll 2000 --hmr=false --live-reload=false
 if($LASTEXITCODE -ne 0){throw 'Falló el servidor local Wi-Fi.'}
}finally{Pop-Location}
