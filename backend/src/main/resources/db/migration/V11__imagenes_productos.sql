CREATE TABLE producto_imagenes (
 producto_id bigint NOT NULL PRIMARY KEY REFERENCES productos(id),
 contenido varbinary(max) NOT NULL,
 tipo varchar(20) NOT NULL CHECK(tipo IN ('image/jpeg','image/png')),
 revision uniqueidentifier NOT NULL,
 CONSTRAINT ck_producto_imagen_tamano CHECK(DATALENGTH(contenido) BETWEEN 1 AND 1048576)
);
