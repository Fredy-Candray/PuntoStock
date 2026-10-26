param([string]$BaseUrl='http://127.0.0.1:18080/api',[switch]$PermitirDatosReales)
$ErrorActionPreference='Stop'
if(!$PermitirDatosReales){& (Join-Path $PSScriptRoot 'Test-SqlIntegration.ps1');return}
$root=Split-Path $PSScriptRoot -Parent
$settings=Import-Clixml (Join-Path $root '.cache/local/secrets.clixml')
function Plain($value){[Net.NetworkCredential]::new('', $value).Password}
$checks=[Collections.Generic.List[string]]::new()
function Check([bool]$ok,[string]$label){if(!$ok){throw "FALLÓ: $label"};$checks.Add($label);Write-Output "OK: $label"}
function Request([string]$method,[string]$route,$body=$null,[string]$token=''){
 $requestOptions=@{Uri="$BaseUrl$route";Method=$method;SkipHttpErrorCheck=$true;ContentType='application/json';TimeoutSec=30}
 $requestOptions.Headers=@{'X-PuntoStock'='1'}
 if($token){$requestOptions.Headers.Authorization="Bearer $token"}
 if($null -ne $body){$requestOptions.Body=ConvertTo-Json -InputObject $body -Depth 10 -Compress}
 $response=Invoke-WebRequest @requestOptions
 $data=if($response.Content){try{ConvertFrom-Json $response.Content}catch{$null}}else{$null}
 [pscustomobject]@{Status=[int]$response.StatusCode;Data=$data}
}
function Must($response,[int]$status,[string]$label){Check ($response.Status -eq $status) "$label (HTTP $($response.Status))"}
$connection=[System.Data.SqlClient.SqlConnection]::new("Server=$($settings.Instance);Database=PuntoStock;Integrated Security=True;TrustServerCertificate=True")
$connection.Open()
function Scalar([string]$sql){$cmd=$connection.CreateCommand();$cmd.CommandText=$sql;$cmd.ExecuteScalar()}
function Execute([string]$sql){$cmd=$connection.CreateCommand();$cmd.CommandText=$sql;[void]$cmd.ExecuteNonQuery()}
$triggerCreated=$false
try {
 Check ((Scalar "SELECT COUNT(*) FROM sys.extended_properties WHERE class=0 AND name=N'PuntoStockLocal' AND CAST(value AS nvarchar(10))=N'1'") -eq 1) 'Base identificada como entorno local del proyecto'
 Must (Request GET '/productos') 401 'API exige autenticación'
 Must (Request POST '/auth/login' @{username=$settings.AdminUser;password='incorrecta'}) 401 'Login rechaza clave inválida'
 $login=Request POST '/auth/login' @{username=$settings.AdminUser;password=(Plain $settings.AdminPassword)}
 Must $login 200 'Login administrador'
 $admin=$login.Data.accessToken
 Check ($login.Data.roles -contains 'ADMIN') 'Administrador recibe rol ADMIN'
 $users=Request GET '/usuarios' $null $admin
 Must $users 200 'Administrador consulta usuarios'
 $seller=$users.Data | Where-Object username -eq $settings.SellerUser | Select-Object -First 1
 if(!$seller){
  $created=Request POST '/usuarios' @{username=$settings.SellerUser;password=(Plain $settings.SellerPassword);rol='VENDEDOR'} $admin
  Must $created 200 'Alta de vendedor';$sellerId=$created.Data.id
 }else{$sellerId=$seller.id}
 $login=Request POST '/auth/login' @{username=$settings.SellerUser;password=(Plain $settings.SellerPassword)}
 Must $login 200 'Login vendedor';$sellerToken=$login.Data.accessToken
 $day=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow,'Central America Standard Time').ToString('yyyy-MM-dd')
 Must (Request GET "/reportes/ganancia?fecha=$day" $null $sellerToken) 403 'Vendedor no consulta ganancias'
 Must (Request GET '/usuarios' $null $sellerToken) 403 'Vendedor no administra usuarios'
 Must (Request GET '/inventario/movimientos' $null $sellerToken) 403 'Vendedor no consulta auditoría administrativa'
 $run=[DateTime]::UtcNow.ToString('yyyyMMddHHmmss')
 $product=@{sku="LOCAL-$run";nombre='Café de prueba local';descripcion='Producto de demostración creado por la prueba local';precioCompra=2.50;precioVenta=4.00;fotoUrl='/assets/producto-demo.svg'}
 Must (Request POST '/productos' $product $sellerToken) 403 'Vendedor no crea productos'
 $created=Request POST '/productos' $product $admin;Must $created 200 'Administrador crea producto';$id=[long]$created.Data.id
 Must (Request POST '/productos' $product $admin) 409 'SKU duplicado rechazado'
 Must (Request PUT "/productos/$id" $product $sellerToken) 403 'Vendedor no cambia precios'
 Must (Request DELETE "/productos/$id" $null $sellerToken) 403 'Vendedor no elimina productos'
 $entry=@{productoId=$id;tipo='COMPRA';cantidad=7;motivo="Prueba local $run"}
 Must (Request POST '/inventario/movimientos' $entry $sellerToken) 403 'Vendedor no ajusta inventario'
 Must (Request POST '/inventario/movimientos' $entry $admin) 200 'Entrada de siete unidades'
 $catalog=Request GET '/productos' $null $sellerToken;Must $catalog 200 'Vendedor consulta catálogo'
 $visible=$catalog.Data | Where-Object id -eq $id
 Check ($visible.stock -eq 7 -and $visible.PSObject.Properties.Name -notcontains 'precioCompra') 'Stock visible y costo oculto al vendedor'
 $before=(Request GET "/reportes/ganancia?fecha=$day" $null $admin).Data.ganancia
 $sale=Request POST '/ventas' @{lineas=@(@{productoId=$id;cantidad=2})} $sellerToken
 Must $sale 200 'Venta de dos unidades';Check ($sale.Data.total -eq 8) 'Total confirmado de ocho dólares'
 Check ((Scalar "SELECT stock FROM productos WHERE id=$id") -eq 5) 'Venta descuenta stock automáticamente'
 Check ((Scalar "SELECT ganancia FROM ventas WHERE id=$($sale.Data.id)") -eq 3) 'Ganancia de venta de tres dólares'
 Check ((Scalar "SELECT COUNT(*) FROM movimientos_inventario WHERE venta_id=$($sale.Data.id) AND cantidad=-2 AND stock_anterior=7 AND stock_posterior=5") -eq 1) 'Salida auditada con stock anterior y posterior'
 $product.precioCompra=3.50
 Must (Request PUT "/productos/$id" $product $admin) 200 'Administrador cambia costo'
 Check ((Scalar "SELECT costo_unitario FROM detalle_venta WHERE venta_id=$($sale.Data.id)") -eq 2.50) 'Costo histórico permanece tras cambiar catálogo'
 $after=(Request GET "/reportes/ganancia?fecha=$day" $null $admin).Data.ganancia
 Check (($after-$before) -eq 3) 'Reporte diario mantiene ganancia histórica'
 Must (Request POST '/ventas' @{lineas=@(@{productoId=$id;cantidad=6})} $sellerToken) 409 'Sobreventa rechazada'
 Must (Request POST '/ventas' @{lineas=@(@{productoId=$id;cantidad=1},@{productoId=$id;cantidad=1})} $sellerToken) 400 'Líneas duplicadas rechazadas'
 Must (Request POST '/inventario/movimientos' @{productoId=$id;tipo='MERMA';cantidad=1;motivo='Signo inválido'} $admin) 400 'Movimiento con signo inválido rechazado'
 Must (Request POST '/inventario/movimientos' @{productoId=$id;tipo='AJUSTE';cantidad=-6;motivo='Stock negativo'} $admin) 409 'Ajuste no permite stock negativo'
 # Concurrencia real: dos conexiones HTTP compiten por la última unidad.
 $product.sku="RACE-$run";$product.nombre='Última unidad de prueba'
 $race=Request POST '/productos' $product $admin;Must $race 200 'Producto para concurrencia';$raceId=[long]$race.Data.id
 Must (Request POST '/inventario/movimientos' @{productoId=$raceId;tipo='COMPRA';cantidad=1;motivo='Prueba de concurrencia'} $admin) 200 'Carga de última unidad'
 $client=[Net.Http.HttpClient]::new();$client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$sellerToken)
 try {
  $json=ConvertTo-Json @{lineas=@(@{productoId=$raceId;cantidad=1})} -Depth 5 -Compress
  $a=$client.PostAsync("$BaseUrl/ventas",[Net.Http.StringContent]::new($json,[Text.Encoding]::UTF8,'application/json'))
  $b=$client.PostAsync("$BaseUrl/ventas",[Net.Http.StringContent]::new($json,[Text.Encoding]::UTF8,'application/json'))
  $statuses=@([int]$a.GetAwaiter().GetResult().StatusCode,[int]$b.GetAwaiter().GetResult().StatusCode) | Sort-Object
  Check (($statuses -join ',') -eq '200,409') 'Concurrencia: una venta confirmada y otra rechazada'
  Check ((Scalar "SELECT stock FROM productos WHERE id=$raceId") -eq 0) 'Concurrencia conserva stock cero'
 } finally {$client.Dispose()}
 # Fallo controlado solo para el producto de esta ejecución; se retira siempre.
 if((Scalar "SELECT COUNT(*) FROM sys.triggers WHERE name='tr_PuntoStockLocal_Rollback'") -ne 0){throw 'Existe un trigger de prueba previo: revisar antes de continuar.'}
 Execute "CREATE TRIGGER dbo.tr_PuntoStockLocal_Rollback ON dbo.movimientos_inventario AFTER INSERT AS BEGIN SET NOCOUNT ON; IF EXISTS(SELECT 1 FROM inserted WHERE producto_id=$id AND tipo='VENTA') THROW 51000,'Fallo intencional para verificar rollback local',1; END"
 $triggerCreated=$true
 $salesBefore=Scalar 'SELECT COUNT(*) FROM ventas';$detailsBefore=Scalar 'SELECT COUNT(*) FROM detalle_venta';$movBefore=Scalar 'SELECT COUNT(*) FROM movimientos_inventario'
 $failed=Request POST '/ventas' @{lineas=@(@{productoId=$id;cantidad=1})} $sellerToken
 Check ($failed.Status -ge 400) 'Fallo SQL controlado rechazó venta'
 Check ((Scalar 'SELECT COUNT(*) FROM ventas') -eq $salesBefore -and (Scalar 'SELECT COUNT(*) FROM detalle_venta') -eq $detailsBefore -and (Scalar 'SELECT COUNT(*) FROM movimientos_inventario') -eq $movBefore -and (Scalar "SELECT stock FROM productos WHERE id=$id") -eq 5) 'Rollback revierte venta, detalle, movimiento y stock'
 Execute 'DROP TRIGGER dbo.tr_PuntoStockLocal_Rollback';$triggerCreated=$false
 Must (Request PUT "/usuarios/$sellerId/activo?valor=false" $null $admin) 200 'Desactivación de vendedor'
 try {Must (Request GET '/productos' $null $sellerToken) 401 'Token de usuario desactivado se rechaza'} finally {[void](Request PUT "/usuarios/$sellerId/activo?valor=true" $null $admin)}
 # Alterar un byte de firma conservando formato JWT.
 $parts=$sellerToken.Split('.');$parts[2]=($(if($parts[2][0] -eq 'A'){'B'}else{'A'}))+$parts[2].Substring(1)
 Must (Request GET '/productos' $null ($parts -join '.')) 401 'Firma JWT alterada rechazada'
 $report=@{fecha=[DateTimeOffset]::Now.ToString('o');instancia=$settings.Instance;base='PuntoStock';checks=$checks.ToArray();productoDemo=$id;productoConcurrencia=$raceId;resultado='APROBADO'}
 $reportJson=($report | ConvertTo-Json -Depth 5).Replace("`r`n","`n")+"`n"
 [IO.File]::WriteAllText((Join-Path $root 'docs/prueba-local-resultados.json'),$reportJson,[Text.UTF8Encoding]::new($false))
 Write-Output "Prueba local finalizada: $($checks.Count) comprobaciones aprobadas. Datos demo conservados."
} finally {if($triggerCreated){Execute 'DROP TRIGGER dbo.tr_PuntoStockLocal_Rollback'};$connection.Dispose()}
