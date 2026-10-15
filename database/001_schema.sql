-- Ejecutar sobre una base vacía PuntoStock. Fechas en UTC.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
CREATE TABLE roles (id bigint IDENTITY PRIMARY KEY, nombre varchar(20) NOT NULL UNIQUE,
 CONSTRAINT ck_rol CHECK(nombre IN ('ADMIN','VENDEDOR')));
CREATE TABLE usuarios (id bigint IDENTITY PRIMARY KEY, username nvarchar(80) NOT NULL UNIQUE,
 password_hash varchar(100) NOT NULL, activo bit NOT NULL DEFAULT 1);
CREATE TABLE usuario_roles (usuario_id bigint NOT NULL REFERENCES usuarios(id),
 rol_id bigint NOT NULL REFERENCES roles(id), PRIMARY KEY(usuario_id,rol_id));
CREATE TABLE productos (id bigint IDENTITY PRIMARY KEY, sku nvarchar(80) NOT NULL UNIQUE,
 nombre nvarchar(160) NOT NULL, descripcion nvarchar(2000) NOT NULL,
 precio_compra decimal(19,2) NOT NULL CHECK(precio_compra>=0),
 precio_venta decimal(19,2) NOT NULL CHECK(precio_venta>=0),
 stock int NOT NULL DEFAULT 0 CHECK(stock>=0), foto_url nvarchar(1000) NOT NULL,
 activo bit NOT NULL DEFAULT 1, version bigint NOT NULL DEFAULT 0);
CREATE TABLE ventas (id bigint IDENTITY PRIMARY KEY, fecha datetime2(6) NOT NULL,
 usuario_id bigint NOT NULL REFERENCES usuarios(id),
 total decimal(19,2) NOT NULL CHECK(total>=0), ganancia decimal(19,2) NOT NULL);
CREATE TABLE detalle_venta (id bigint IDENTITY PRIMARY KEY,
 venta_id bigint NOT NULL REFERENCES ventas(id), producto_id bigint NOT NULL REFERENCES productos(id),
 cantidad int NOT NULL CHECK(cantidad>0), costo_unitario decimal(19,2) NOT NULL CHECK(costo_unitario>=0),
 precio_unitario decimal(19,2) NOT NULL CHECK(precio_unitario>=0),
 CONSTRAINT uq_detalle UNIQUE(venta_id,producto_id));
CREATE TABLE movimientos_inventario (id bigint IDENTITY PRIMARY KEY,
 producto_id bigint NOT NULL REFERENCES productos(id), usuario_id bigint NOT NULL REFERENCES usuarios(id),
 venta_id bigint NULL REFERENCES ventas(id), fecha datetime2(6) NOT NULL,
 tipo varchar(40) NOT NULL, cantidad int NOT NULL, stock_anterior int NOT NULL CHECK(stock_anterior>=0),
 stock_posterior int NOT NULL CHECK(stock_posterior>=0), motivo nvarchar(500) NOT NULL,
 CONSTRAINT ck_mov_stock CHECK(CAST(stock_anterior AS bigint)+cantidad=stock_posterior),
 CONSTRAINT ck_mov_tipo CHECK(
 (tipo IN ('COMPRA','DEVOLUCION_CLIENTE') AND cantidad>0 AND venta_id IS NULL) OR
 (tipo IN ('MERMA','DEVOLUCION_PROVEEDOR') AND cantidad<0 AND venta_id IS NULL) OR
 (tipo='VENTA' AND cantidad<0 AND venta_id IS NOT NULL) OR
 (tipo='AJUSTE' AND cantidad<>0 AND venta_id IS NULL)));
CREATE INDEX ix_ventas_fecha ON ventas(fecha) INCLUDE(total,ganancia);
CREATE INDEX ix_mov_producto_fecha ON movimientos_inventario(producto_id,fecha);
INSERT INTO roles(nombre) VALUES ('ADMIN'),('VENDEDOR');
COMMIT;
