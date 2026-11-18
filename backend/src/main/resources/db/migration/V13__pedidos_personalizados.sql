CREATE TABLE pedidos (
 id bigint IDENTITY PRIMARY KEY,
 solicitud_id uniqueidentifier NOT NULL UNIQUE,
 huella varchar(64) NOT NULL,
 cliente nvarchar(160) NOT NULL,
 descripcion nvarchar(500) NOT NULL,
 servicios nvarchar(1000) NOT NULL,
 cantidad int NOT NULL CHECK(cantidad>0),
 precio_unitario decimal(19,2) NOT NULL CHECK(precio_unitario>0),
 total decimal(19,2) NOT NULL,
 entrega date NOT NULL,
 estado varchar(12) NOT NULL CHECK(estado IN ('PENDIENTE','LISTO','ENTREGADO')),
 fecha datetime2(6) NOT NULL,
 usuario_id bigint NOT NULL REFERENCES usuarios(id),
 CONSTRAINT ck_pedido_total CHECK(total=precio_unitario*cantidad)
);
CREATE TABLE pedido_abonos (
 id bigint IDENTITY PRIMARY KEY,
 pedido_id bigint NOT NULL REFERENCES pedidos(id),
 caja_id bigint NOT NULL REFERENCES cajas(id),
 usuario_id bigint NOT NULL REFERENCES usuarios(id),
 solicitud_id uniqueidentifier NOT NULL UNIQUE,
 fecha datetime2(6) NOT NULL,
 monto decimal(19,2) NOT NULL CHECK(monto>0),
 saldo decimal(19,2) NOT NULL CHECK(saldo>=0),
 metodo_pago varchar(20) NOT NULL CHECK(metodo_pago IN ('EFECTIVO','TARJETA','TRANSFERENCIA'))
);
CREATE INDEX ix_pedido_abonos_pedido ON pedido_abonos(pedido_id,id);
CREATE INDEX ix_pedido_abonos_caja ON pedido_abonos(caja_id,id) INCLUDE(monto,metodo_pago);
ALTER TABLE movimientos_inventario ADD pedido_id bigint NULL REFERENCES pedidos(id),
 solicitud_consumo uniqueidentifier NULL,
 nombre_insumo nvarchar(160) NULL, unidad_insumo nvarchar(80) NULL;
GO
CREATE UNIQUE INDEX uq_solicitud_consumo ON movimientos_inventario(pedido_id,solicitud_consumo)
 WHERE solicitud_consumo IS NOT NULL;
CREATE INDEX ix_mov_pedido ON movimientos_inventario(pedido_id,id) WHERE pedido_id IS NOT NULL;
ALTER TABLE movimientos_inventario DROP CONSTRAINT ck_mov_tipo;
ALTER TABLE movimientos_inventario ADD CONSTRAINT ck_mov_tipo CHECK(
 (tipo='CONSUMO_PEDIDO' AND cantidad<0 AND venta_id IS NULL AND pedido_id IS NOT NULL
  AND solicitud_consumo IS NOT NULL AND nombre_insumo IS NOT NULL AND unidad_insumo IS NOT NULL)
 OR (pedido_id IS NULL AND solicitud_consumo IS NULL AND nombre_insumo IS NULL AND unidad_insumo IS NULL AND (
 (tipo IN ('COMPRA','DEVOLUCION_CLIENTE') AND cantidad>0 AND venta_id IS NULL) OR
 (tipo IN ('MERMA','DEVOLUCION_PROVEEDOR') AND cantidad<0 AND venta_id IS NULL) OR
 (tipo='VENTA' AND cantidad<0 AND venta_id IS NOT NULL) OR
 (tipo='AJUSTE' AND cantidad<>0 AND venta_id IS NULL))));
UPDATE menu_configuracion SET contenido=JSON_MODIFY(contenido,'append $.secciones',
 JSON_QUERY(N'{"id":"pedidos-personalizados","titulo":"Pedidos","orden":4,"activo":true,"opciones":[{"id":"pedidos","etiqueta":"Pedidos personalizados","ruta":"/pedidos","icono":"receipt","orden":1,"activo":true,"roles":["ADMIN","VENDEDOR"]}]}')),
 version=version+1 WHERE id=1;
