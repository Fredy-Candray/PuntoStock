# Repara únicamente un hash inválido de la cuenta demo; preserva contraseñas BCrypt válidas.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$settings=Import-Clixml (Join-Path $root '.cache/local/secrets.clixml')
$jdk=Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory | Where-Object Name -Match '(^|-)21\.' | Select-Object -First 1
if(!$jdk){throw 'Se requiere un JDK 21 instalado en .jdks.'}
$crypto=Get-ChildItem (Join-Path $root '.cache/m2/org/springframework/security/spring-security-crypto') -Filter '*.jar' -Recurse | Sort-Object FullName -Descending | Select-Object -First 1
if(!$crypto){throw 'No se encontró la dependencia spring-security-crypto en la caché local.'}
$start=[Diagnostics.ProcessStartInfo]::new()
$start.FileName=Join-Path $jdk.FullName 'bin/java.exe'
$start.UseShellExecute=$false;$start.CreateNoWindow=$true
$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
foreach($argument in @('--class-path',$crypto.FullName,(Join-Path $PSScriptRoot 'HashLocalPassword.java'))){[void]$start.ArgumentList.Add($argument)}
if($settings.PSObject.Properties['LocalShortPasswordAllowed'] -and $settings.LocalShortPasswordAllowed){[void]$start.ArgumentList.Add('--local-min8')}
$process=[Diagnostics.Process]::Start($start)
$process.StandardInput.WriteLine([Net.NetworkCredential]::new('', $settings.AdminPassword).Password)
$process.StandardInput.Close()
$hash=$process.StandardOutput.ReadToEnd().Trim()
$errorText=$process.StandardError.ReadToEnd();$process.WaitForExit()
if($process.ExitCode -ne 0 -or $hash -notmatch '^\$2[aby]\$12\$.{53}$'){throw "No se pudo generar BCrypt: $errorText"}
$connection=[System.Data.SqlClient.SqlConnection]::new("Server=$($settings.Instance);Database=PuntoStock;Integrated Security=True;TrustServerCertificate=True")
try {
 $connection.Open();$cmd=$connection.CreateCommand()
 $cmd.CommandText="SELECT COUNT(*) FROM sys.extended_properties WHERE class=0 AND name=N'PuntoStockLocal' AND CAST(value AS nvarchar(10))=N'1'"
 if([int]$cmd.ExecuteScalar() -ne 1){throw 'La base no está identificada como entorno local de PuntoStock.'}
 $cmd.CommandText=@'
UPDATE dbo.usuarios SET password_hash=@hash
WHERE username=@username AND
 (LEN(password_hash)<>60 OR LEFT(password_hash,4) NOT IN ('$2a$','$2b$','$2y$'))
'@
 [void]$cmd.Parameters.Add('@hash',[System.Data.SqlDbType]::VarChar,100);$cmd.Parameters['@hash'].Value=$hash
 [void]$cmd.Parameters.Add('@username',[System.Data.SqlDbType]::NVarChar,80);$cmd.Parameters['@username'].Value=$settings.AdminUser
 $count=$cmd.ExecuteNonQuery()
 if($count -ne 1){throw 'No se encontró un hash inválido que reparar; no se cambió una contraseña BCrypt válida.'}
 Write-Output 'Hash del administrador local reparado. La clave corresponde a Show-LocalCredentials.ps1.'
} finally {$connection.Dispose();$process.Dispose()}
