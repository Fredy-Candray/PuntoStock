param([string]$NodeExecutable='node',[int]$Port=14200,[int]$ApiPort=18080)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
try {
 $existing=Invoke-WebRequest "http://127.0.0.1:$Port/" -UseBasicParsing -TimeoutSec 3
} catch {$existing=$null}
if($existing){
 if($existing.StatusCode -eq 200 -and $existing.Content -match '<title>PuntoStock</title>'){
  Write-Output "PuntoStock ya está iniciado: http://127.0.0.1:$Port/"
  return
 }
 throw "El puerto $Port está ocupado por otro servidor. Use -Port para elegir otro."
}
$bundled=Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
if($NodeExecutable -eq 'node' -and (Test-Path $bundled)){$NodeExecutable=$bundled}
$nodeVersionText=& $NodeExecutable --version
if($LASTEXITCODE -ne 0){throw 'No se pudo ejecutar Node.js.'}
$nodeVersion=[version]$nodeVersionText.Trim().TrimStart('v')
$compatible=($nodeVersion.Major -eq 22 -and $nodeVersion -ge [version]'22.22.3') -or ($nodeVersion.Major -eq 24 -and $nodeVersion -ge [version]'24.15.0') -or $nodeVersion.Major -eq 26
if(!$compatible){throw "Node $nodeVersion no es compatible. Use Node 22.22.3+, 24.15+ o 26 con -NodeExecutable."}
Write-Output "PuntoStock usará Node $nodeVersion en http://127.0.0.1:$Port/"
$localDir=Join-Path $root '.cache/local'
New-Item -ItemType Directory -Force $localDir | Out-Null
$proxy=Join-Path $root '.cache/local/proxy.json'
$proxyJson=@{'/api'=@{target="http://127.0.0.1:$ApiPort";secure=$false;changeOrigin=$true}} | ConvertTo-Json -Depth 3
[IO.File]::WriteAllText($proxy,$proxyJson,[Text.UTF8Encoding]::new($false))
Push-Location (Join-Path $root 'frontend')
try { & $NodeExecutable node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1 --port $Port --proxy-config $proxy --poll 2000; if($LASTEXITCODE -ne 0){throw 'Falló el arranque de Angular.'} }
finally {Pop-Location}
