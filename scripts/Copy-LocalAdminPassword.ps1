# Copia la clave al portapapeles local sin imprimirla en la terminal.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$settings=Import-Clixml (Join-Path $root '.cache/local/secrets.clixml')
Set-Clipboard -Value ([Net.NetworkCredential]::new('', $settings.AdminPassword).Password)
Write-Output "Contraseña de $($settings.AdminUser) copiada. Pégala con Ctrl+V en el campo Contraseña."
