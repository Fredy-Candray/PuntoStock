-- El stock de productos desglosados siempre representa piezas individuales.
ALTER TABLE productos ADD unidades_por_caja int NULL, precio_compra_caja decimal(19,2) NULL, precio_venta_caja decimal(19,2) NULL;
GO
ALTER TABLE productos ADD CONSTRAINT ck_producto_caja CHECK (
 (unidades_por_caja IS NULL AND precio_compra_caja IS NULL AND precio_venta_caja IS NULL)
 OR (unidades_por_caja IS NOT NULL AND unidades_por_caja BETWEEN 2 AND 1000000
 AND precio_compra_caja IS NOT NULL AND precio_compra_caja>=0
 AND (precio_venta_caja IS NULL OR precio_venta_caja>0) AND tipo='PRODUCTO' AND unidad='UNIDAD'));
GO
ALTER TABLE detalle_venta ADD presentacion varchar(10) NOT NULL CONSTRAINT df_detalle_presentacion DEFAULT 'UNIDAD',
 unidades_por_presentacion int NOT NULL CONSTRAINT df_detalle_unidades_presentacion DEFAULT 1;
GO
ALTER TABLE detalle_venta ADD CONSTRAINT ck_detalle_presentacion CHECK (
 (presentacion='UNIDAD' AND unidades_por_presentacion=1) OR
 (presentacion='CAJA' AND unidades_por_presentacion BETWEEN 2 AND 1000000));
GO
ALTER TABLE detalle_venta DROP CONSTRAINT uq_detalle;
GO
ALTER TABLE detalle_venta ADD CONSTRAINT uq_detalle UNIQUE(venta_id,producto_id,presentacion);
GO
