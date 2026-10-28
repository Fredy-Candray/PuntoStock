CREATE TABLE caja_control (id int NOT NULL PRIMARY KEY CHECK(id=1));
INSERT INTO caja_control(id) VALUES(1);
CREATE TABLE cajas (
 id bigint IDENTITY NOT NULL PRIMARY KEY,
 apertura datetime2(6) NOT NULL, abierto_por bigint NOT NULL REFERENCES usuarios(id),
 inicial decimal(19,2) NOT NULL CHECK(inicial>=0),
 nota_apertura nvarchar(500) NOT NULL,
 solicitud_apertura uniqueidentifier NOT NULL UNIQUE,
 cierre datetime2(6) NULL, cerrado_por bigint NULL REFERENCES usuarios(id),
 contado decimal(19,2) NULL, esperado decimal(19,2) NULL, diferencia decimal(19,2) NULL,
 nota_cierre nvarchar(500) NULL, solicitud_cierre uniqueidentifier NULL,
 CONSTRAINT ck_caja_cierre CHECK (
  (cierre IS NULL AND cerrado_por IS NULL AND contado IS NULL AND esperado IS NULL AND diferencia IS NULL AND solicitud_cierre IS NULL AND nota_cierre IS NULL)
  OR (cierre IS NOT NULL AND cerrado_por IS NOT NULL AND contado IS NOT NULL AND contado>=0 AND esperado IS NOT NULL AND esperado>=0 AND diferencia IS NOT NULL AND diferencia=contado-esperado AND solicitud_cierre IS NOT NULL AND nota_cierre IS NOT NULL AND cierre>=apertura))
);
CREATE UNIQUE INDEX uq_caja_abierta ON cajas(cierre) WHERE cierre IS NULL;
CREATE UNIQUE INDEX uq_caja_cierre_solicitud ON cajas(solicitud_cierre) WHERE solicitud_cierre IS NOT NULL;
CREATE TABLE caja_movimientos (
 id bigint IDENTITY NOT NULL PRIMARY KEY,
 caja_id bigint NOT NULL REFERENCES cajas(id),
 usuario_id bigint NOT NULL REFERENCES usuarios(id),
 fecha datetime2(6) NOT NULL,
 tipo varchar(10) NOT NULL CHECK(tipo IN ('ENTRADA','SALIDA')),
 monto decimal(19,2) NOT NULL CHECK(monto>0),
 motivo nvarchar(500) NOT NULL,
 solicitud_id uniqueidentifier NOT NULL UNIQUE
);
CREATE INDEX ix_caja_movimientos ON caja_movimientos(caja_id,id);
ALTER TABLE ventas ADD caja_id bigint NULL REFERENCES cajas(id);
CREATE INDEX ix_ventas_caja ON ventas(caja_id,id) INCLUDE(total,metodo_pago);
-- Agrega una sección sin reemplazar las opciones personalizadas del menú existente.
IF NOT EXISTS(SELECT 1 FROM menu_configuracion CROSS APPLY OPENJSON(contenido,'$.secciones') s CROSS APPLY OPENJSON(s.value,'$.opciones') WITH(ruta nvarchar(200)) o WHERE o.ruta='/caja')
 UPDATE menu_configuracion SET contenido=JSON_MODIFY(contenido,'append $.secciones',JSON_QUERY(N'{"id":"caja-operacion-v8","titulo":"Caja","orden":3,"activo":true,"opciones":[{"id":"caja-v8","etiqueta":"Caja","ruta":"/caja","icono":"receipt","orden":1,"activo":true,"roles":["ADMIN"]}]}')),version=version+1 WHERE id=1;
