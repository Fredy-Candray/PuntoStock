ALTER TABLE movimientos_inventario ADD costo_unitario_insumo decimal(19,2) NULL;
GO
ALTER TABLE movimientos_inventario ADD CONSTRAINT ck_mov_costo_insumo CHECK(
 costo_unitario_insumo IS NULL OR (tipo='CONSUMO_PEDIDO' AND costo_unitario_insumo>=0));
