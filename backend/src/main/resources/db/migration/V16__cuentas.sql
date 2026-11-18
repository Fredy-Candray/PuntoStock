CREATE TABLE cuentas (
 id bigint IDENTITY PRIMARY KEY,
 nombre nvarchar(100) NOT NULL,
 ambito varchar(10) NOT NULL CHECK(ambito IN ('PERSONAL','NEGOCIO')),
 inicial decimal(19,2) NOT NULL CHECK(inicial>=0),
 solicitud uniqueidentifier NOT NULL UNIQUE
);
CREATE TABLE cuenta_movimientos (
 id bigint IDENTITY PRIMARY KEY,
 solicitud uniqueidentifier NOT NULL UNIQUE,
 huella varchar(64) NOT NULL,
 fecha datetime2(6) NOT NULL,
 usuario_id bigint NOT NULL REFERENCES usuarios(id),
 tipo varchar(16) NOT NULL CHECK(tipo IN ('APORTE','RETIRO','TRASPASO','INGRESO','GASTO','COMPRA','PAGO_COMPRA')),
 origen bigint NULL REFERENCES cuentas(id),
 destino bigint NULL REFERENCES cuentas(id),
 caja_id bigint NULL REFERENCES cajas(id),
 concepto nvarchar(250) NOT NULL,
 monto decimal(19,2) NOT NULL CHECK(monto>0),
 compra_id bigint NULL REFERENCES cuenta_movimientos(id),
 producto_id bigint NULL REFERENCES productos(id),
 cantidad int NULL,
 pagado_inicial decimal(19,2) NULL
);
CREATE INDEX ix_cuenta_origen ON cuenta_movimientos(origen) INCLUDE(monto,pagado_inicial);
CREATE INDEX ix_cuenta_destino ON cuenta_movimientos(destino) INCLUDE(monto);
CREATE INDEX ix_cuenta_compra ON cuenta_movimientos(compra_id) INCLUDE(monto);
ALTER TABLE pedidos ADD fecha_entregado datetime2(6) NULL;
UPDATE menu_configuracion SET contenido=JSON_MODIFY(contenido,'append $.secciones',
 JSON_QUERY(N'{"id":"finanzas","titulo":"Finanzas","orden":5,"activo":true,"opciones":[{"id":"cuentas","etiqueta":"Cuentas","ruta":"/cuentas","icono":"money","orden":1,"activo":true,"roles":["ADMIN"]}]}')),
 version=version+1 WHERE id=1;
