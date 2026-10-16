param([switch]$AllowShortLocalPassword)
# Cambio interactivo de la cuenta demo local. No recibe claves en argumentos ni las imprime.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$secretFile=Join-Path $root '.cache/local/secrets.clixml'
$settings=Import-Clixml $secretFile
$secure=Read-Host "Nueva contraseña para $($settings.AdminUser)" -AsSecureString
$confirmation=Read-Host 'Confirma la contraseña' -AsSecureString
$password=[Net.NetworkCredential]::new('', $secure).Password
if($password -cne [Net.NetworkCredential]::new('', $confirmation).Password){throw 'Las contraseñas no coinciden. No se cambió la cuenta.'}
$minimum=if($AllowShortLocalPassword){8}else{12}
if($password.Length -lt $minimum -or [Text.Encoding]::UTF8.GetByteCount($password) -gt 72){throw "Se requieren al menos $minimum caracteres y máximo 72 bytes."}
$jdk=Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory | Where-Object Name -Match '(^|-)21\.' | Select-Object -First 1
$crypto=Get-ChildItem (Join-Path $root '.cache/m2/org/springframework/security/spring-security-crypto') -Filter '*.jar' -Recurse | Sort-Object FullName -Descending | Select-Object -First 1
if(!$jdk -or !$crypto){throw 'No se encontró JDK 21 o spring-security-crypto en la instalación local.'}
$start=[Diagnostics.ProcessStartInfo]::new()
$start.FileName=Join-Path $jdk.FullName 'bin/java.exe'
$start.UseShellExecute=$false;$start.CreateNoWindow=$true
$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
foreach($argument in @('--class-path',$crypto.FullName,(Join-Path $PSScriptRoot 'HashLocalPassword.java'))){[void]$start.ArgumentList.Add($argument)}
if($AllowShortLocalPassword){[void]$start.ArgumentList.Add('--local-min8')}
$process=[Diagnostics.Process]::Start($start)
$process.StandardInput.WriteLine($password);$process.StandardInput.Close()
$hash=$process.StandardOutput.ReadToEnd().Trim();$errorText=$process.StandardError.ReadToEnd();$process.WaitForExit()
if($process.ExitCode -ne 0 -or $hash -notmatch '^\$2[aby]\$12\$.{53}$'){throw "No se pudo generar BCrypt: $errorText"}
$connection=[System.Data.SqlClient.SqlConnection]::new("Server=$($settings.Instance);Database=PuntoStock;Integrated Security=True;TrustServerCertificate=True")
$backup=[IO.File]::ReadAllBytes($secretFile)
$transaction=$null;$fileChanged=$false
try {
 $connection.Open();$cmd=$connection.CreateCommand()
 $cmd.CommandText="SELECT COUNT(*) FROM sys.extended_properties WHERE class=0 AND name=N'PuntoStockLocal' AND CAST(value AS nvarchar(10))=N'1'"
 if([int]$cmd.ExecuteScalar() -ne 1){throw 'La base no está identificada como entorno local de PuntoStock.'}
 $transaction=$connection.BeginTransaction();$cmd.Transaction=$transaction
 $cmd.CommandText="UPDATE u SET password_hash=@hash FROM dbo.usuarios u WHERE username=@username AND EXISTS (SELECT 1 FROM dbo.usuario_roles ur JOIN dbo.roles r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.nombre='ADMIN')"
 [void]$cmd.Parameters.Add('@hash',[System.Data.SqlDbType]::VarChar,100);$cmd.Parameters['@hash'].Value=$hash
 [void]$cmd.Parameters.Add('@username',[System.Data.SqlDbType]::NVarChar,80);$cmd.Parameters['@username'].Value=$settings.AdminUser
 if($cmd.ExecuteNonQuery() -ne 1){throw 'No se encontró la cuenta administradora local configurada.'}
 $settings.AdminPassword=$secure
 $settings | Add-Member -NotePropertyName LocalShortPasswordAllowed -NotePropertyValue ([bool]$AllowShortLocalPassword) -Force
 $fileChanged=$true;$settings | Export-Clixml $secretFile
 $transaction.Commit()
 Write-Output "Contraseña actualizada para $($settings.AdminUser), con BCrypt y copia local cifrada."
} catch {
 if($transaction){try{$transaction.Rollback()}catch{}}
 if($fileChanged){[IO.File]::WriteAllBytes($secretFile,$backup)}
 throw
} finally {$password=$null;$process.Dispose();$connection.Dispose()}
