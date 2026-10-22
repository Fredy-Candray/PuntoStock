param([string]$GmailUser,[string]$PublicUrl='http://127.0.0.1:14200')
$ErrorActionPreference='Stop'
if(!$GmailUser){$GmailUser=Read-Host 'Dirección Gmail que enviará los enlaces'}
if($GmailUser -notmatch '^[^\s@]+@gmail\.com$'){throw 'Indica una dirección Gmail válida.'}
$mailPublicUri=$null
if(![Uri]::TryCreate($PublicUrl,[UriKind]::Absolute,[ref]$mailPublicUri) -or $mailPublicUri.Scheme -notin @('http','https') -or $mailPublicUri.UserInfo -or $mailPublicUri.Fragment -or $mailPublicUri.Query){throw 'PublicUrl debe ser la URL HTTP/HTTPS de confianza del sistema, sin credenciales, consulta ni fragmento.'}
$mailSecret=Read-Host 'Contraseña de aplicación de Gmail (no la contraseña normal)' -AsSecureString
if($mailSecret.Length -eq 0){throw 'La contraseña de aplicación no puede estar vacía.'}
$mailRoot=Split-Path $PSScriptRoot -Parent
$mailDir=Join-Path $mailRoot '.cache/local'
New-Item -ItemType Directory -Force $mailDir | Out-Null
[pscustomobject]@{Username=$GmailUser.Trim();Password=$mailSecret;PublicUrl=$PublicUrl.TrimEnd('/')} | Export-Clixml (Join-Path $mailDir 'mail.clixml')
Write-Output 'Configuración de Gmail guardada con DPAPI para este usuario de Windows. Reinicia el backend para activarla. No se envió ningún correo.'
