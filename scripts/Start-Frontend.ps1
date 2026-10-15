param([string]$NodeExecutable='node',[int]$Port=14200,[int]$ApiPort=18080)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$bundled=Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
if($NodeExecutable -eq 'node' -and (Test-Path $bundled)){$NodeExecutable=$bundled}
$proxy=Join-Path $root '.cache/local/proxy.json'
@{'/api'=@{target="http://127.0.0.1:$ApiPort";secure=$false;changeOrigin=$true}} | ConvertTo-Json -Depth 3 | Set-Content $proxy -Encoding utf8
Push-Location (Join-Path $root 'frontend')
try { & $NodeExecutable node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1 --port $Port --proxy-config $proxy; if($LASTEXITCODE -ne 0){throw 'Falló el arranque de Angular.'} }
finally {Pop-Location}
