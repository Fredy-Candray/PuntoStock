param([Parameter(Mandatory=$true)][Uri]$Url)
$ErrorActionPreference='Stop'
if($Url.Scheme -notin @('http','https')){throw 'Indica la dirección HTTP o HTTPS del sistema.'}
$taskResponse=Invoke-WebRequest $Url -UseBasicParsing
$taskRequired=@('Content-Security-Policy','X-Content-Type-Options','Referrer-Policy','X-Frame-Options')
if($Url.Scheme -eq 'https'){$taskRequired+='Strict-Transport-Security'}
$taskMissing=@();foreach($taskHeader in $taskRequired){$taskPresent=[bool]$taskResponse.Headers[$taskHeader];[pscustomobject]@{Cabecera=$taskHeader;Presente=$taskPresent};if(!$taskPresent){$taskMissing+=$taskHeader}}
if($taskMissing.Count){throw ('Faltan cabeceras: '+($taskMissing -join ', '))}
Write-Output 'Cabeceras verificadas en la respuesta real. Repite después de cada cambio de proxy o certificado.'
