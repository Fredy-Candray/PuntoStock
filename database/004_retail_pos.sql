SET XACT_ABORT ON;
IF DB_NAME() <> N'PuntoStock' THROW 51000, 'Esta migración solo admite PuntoStock.', 1;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.productos','tipo') IS NULL ALTER TABLE dbo.productos ADD tipo varchar(20) NOT NULL CONSTRAINT df_productos_tipo DEFAULT 'PRODUCTO';
IF COL_LENGTH('dbo.productos','categoria') IS NULL ALTER TABLE dbo.productos ADD categoria nvarchar(80) NOT NULL CONSTRAINT df_productos_categoria DEFAULT N'General';
IF COL_LENGTH('dbo.productos','marca') IS NULL ALTER TABLE dbo.productos ADD marca nvarchar(80) NOT NULL CONSTRAINT df_productos_marca DEFAULT N'';
IF COL_LENGTH('dbo.productos','unidad') IS NULL ALTER TABLE dbo.productos ADD unidad varchar(20) NOT NULL CONSTRAINT df_productos_unidad DEFAULT 'UNIDAD';
IF COL_LENGTH('dbo.productos','ubicacion') IS NULL ALTER TABLE dbo.productos ADD ubicacion nvarchar(80) NOT NULL CONSTRAINT df_productos_ubicacion DEFAULT N'';
IF COL_LENGTH('dbo.productos','stock_minimo') IS NULL ALTER TABLE dbo.productos ADD stock_minimo int NOT NULL CONSTRAINT df_productos_minimo DEFAULT 5;
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='ck_producto_tipo') EXEC(N'ALTER TABLE dbo.productos ADD CONSTRAINT ck_producto_tipo CHECK(tipo IN (''PRODUCTO'',''SERVICIO'') AND stock_minimo>=0)');
-- Los cobros anteriores no tenían un medio de pago: no se inventa ese dato.
IF COL_LENGTH('dbo.ventas','metodo_pago') IS NULL ALTER TABLE dbo.ventas ADD metodo_pago varchar(20) NOT NULL CONSTRAINT df_ventas_metodo DEFAULT 'SIN_REGISTRO';
IF COL_LENGTH('dbo.ventas','recibido') IS NULL ALTER TABLE dbo.ventas ADD recibido decimal(19,2) NULL;
IF COL_LENGTH('dbo.ventas','cambio') IS NULL ALTER TABLE dbo.ventas ADD cambio decimal(19,2) NULL;
IF COL_LENGTH('dbo.ventas','cliente') IS NULL ALTER TABLE dbo.ventas ADD cliente nvarchar(160) NULL;
IF COL_LENGTH('dbo.ventas','referencia_pago') IS NULL ALTER TABLE dbo.ventas ADD referencia_pago nvarchar(80) NULL;
IF COL_LENGTH('dbo.ventas','solicitud_id') IS NULL ALTER TABLE dbo.ventas ADD solicitud_id uniqueidentifier NULL;
IF COL_LENGTH('dbo.ventas','firma_solicitud') IS NULL ALTER TABLE dbo.ventas ADD firma_solicitud varchar(64) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.ventas') AND name='uq_ventas_solicitud') EXEC(N'CREATE UNIQUE INDEX uq_ventas_solicitud ON dbo.ventas(usuario_id,solicitud_id) WHERE solicitud_id IS NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='ck_venta_cobro') EXEC(N'ALTER TABLE dbo.ventas ADD CONSTRAINT ck_venta_cobro CHECK(
 (metodo_pago=''SIN_REGISTRO'' AND recibido IS NULL AND cambio IS NULL) OR
 (metodo_pago=''EFECTIVO'' AND recibido IS NOT NULL AND cambio IS NOT NULL AND recibido>=total AND cambio>=0 AND recibido=total+cambio) OR
 (metodo_pago IN (''TARJETA'',''TRANSFERENCIA'') AND recibido IS NOT NULL AND cambio IS NOT NULL AND recibido=total AND cambio=0))');
IF COL_LENGTH('dbo.detalle_venta','nombre_producto') IS NULL ALTER TABLE dbo.detalle_venta ADD nombre_producto nvarchar(160) NULL;
IF COL_LENGTH('dbo.detalle_venta','sku_producto') IS NULL ALTER TABLE dbo.detalle_venta ADD sku_producto nvarchar(80) NULL;
IF COL_LENGTH('dbo.detalle_venta','observaciones') IS NULL ALTER TABLE dbo.detalle_venta ADD observaciones nvarchar(200) NULL;
COMMIT;
