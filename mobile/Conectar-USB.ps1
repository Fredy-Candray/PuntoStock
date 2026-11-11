param([string]$Device='',[switch]$Instalar)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw 'No se encontró adb en el SDK Android local.'}
$deviceArgs=@();if($Device){$deviceArgs=@('-s',$Device)}
& $adb @deviceArgs get-state
if($LASTEXITCODE -ne 0){throw 'Conecta y autoriza un único teléfono USB, o indica -Device SERIAL.'}
& $adb @deviceArgs reverse tcp:14200 tcp:14200
if($LASTEXITCODE -ne 0){throw 'No se pudo crear la conexión USB.'}
if($Instalar){
 $apk=Join-Path $taskRoot '.cache/android-local/PuntoStock-local.apk'
 if(!(Test-Path -LiteralPath $apk)){throw 'Compila primero mobile/build-local.py.'}
 & $adb @deviceArgs install -r $apk
 if($LASTEXITCODE -ne 0){throw 'No se pudo instalar la APK.'}
}
Write-Output 'USB conectado. Inicia PuntoStock en la computadora y abre PuntoStock local en el teléfono.'
Write-Output 'Para desconectar: adb reverse --remove tcp:14200'
