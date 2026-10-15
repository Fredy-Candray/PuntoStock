# Ejecutar localmente para consultar las claves generadas; no versionar ni compartir su salida.
$root=Split-Path $PSScriptRoot -Parent
$settings=Import-Clixml (Join-Path $root '.cache/local/secrets.clixml')
Write-Output "Administrador: $($settings.AdminUser)"
Write-Output ([Net.NetworkCredential]::new('', $settings.AdminPassword).Password)
Write-Output "Vendedor: $($settings.SellerUser)"
Write-Output ([Net.NetworkCredential]::new('', $settings.SellerPassword).Password)
