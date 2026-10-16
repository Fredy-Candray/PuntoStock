param([switch]$SystemPath)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$jdk=Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory |
 Where-Object Name -Match '(^|-)21\.' | Select-Object -First 1
if(!$jdk -or !(Test-Path (Join-Path $jdk.FullName 'bin/javac.exe'))){throw 'No se encontró un JDK 21 completo en .jdks.'}
$jdkHome=$jdk.FullName
$bin=Join-Path $jdkHome 'bin'
$scope=if($SystemPath){'Machine'}else{'User'}
if($SystemPath){
 $principal=[Security.Principal.WindowsPrincipal]::new([Security.Principal.WindowsIdentity]::GetCurrent())
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){throw 'Ejecuta -SystemPath con permisos de administrador.'}
}
$backupDir=Join-Path $root '.cache/local'
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$backupFile=Join-Path $backupDir "java-environment-$($scope.ToLower()).json"
$oldPath=[Environment]::GetEnvironmentVariable('Path',$scope)
if(!(Test-Path $backupFile)){
 @{Scope=$scope;JAVA_HOME=[Environment]::GetEnvironmentVariable('JAVA_HOME',$scope);Path=$oldPath} |
  ConvertTo-Json | Set-Content -LiteralPath $backupFile
}
$remaining=@($oldPath -split ';' | Where-Object {$_ -and $_.TrimEnd('\') -ine $bin.TrimEnd('\')})
[Environment]::SetEnvironmentVariable('JAVA_HOME',$jdkHome,$scope)
[Environment]::SetEnvironmentVariable('Path',(@($bin)+$remaining -join ';'),$scope)
$env:JAVA_HOME=$jdkHome
$env:Path="$bin;$env:Path"
Write-Output "Java 21 configurado para $scope. Cierra y abre las terminales y el IDE para cargar el entorno actualizado."
