-- Categorías exclusivas del catálogo de artículos y servicios, no de recordatorios.
CREATE TABLE categorias_productos (
 id bigint IDENTITY NOT NULL PRIMARY KEY,
 nombre nvarchar(80) NOT NULL,
 activo bit NOT NULL CONSTRAINT df_categorias_productos_activo DEFAULT 1,
 version bigint NOT NULL CONSTRAINT df_categorias_productos_version DEFAULT 0,
 CONSTRAINT uq_categorias_productos_nombre UNIQUE(nombre),
 CONSTRAINT ck_categorias_productos_nombre CHECK(LEN(LTRIM(RTRIM(nombre)))>0)
);
UPDATE productos SET categoria=N'General' WHERE LEN(LTRIM(RTRIM(categoria)))=0;
INSERT INTO categorias_productos(nombre) SELECT DISTINCT categoria FROM productos;
INSERT INTO categorias_productos(nombre)
SELECT nombre FROM (VALUES (N'General'),(N'Impresiones'),(N'Fotocopias'),(N'Fotografías'),
 (N'Papelería'),(N'Útiles escolares'),(N'Juguetes'),(N'Carteras'),(N'Audífonos y accesorios'),
 (N'Manualidades'),(N'Tecnología'),(N'Belleza'),(N'Accesorios'),(N'Alimentos'),(N'Variedades'),(N'Otros')) s(nombre)
WHERE NOT EXISTS(SELECT 1 FROM categorias_productos c WHERE c.nombre=s.nombre);
-- Conserva el contrato de catálogo por nombre, con integridad referencial y renombrado atómico.
ALTER TABLE productos ADD CONSTRAINT fk_productos_categoria
 FOREIGN KEY(categoria) REFERENCES categorias_productos(nombre) ON UPDATE CASCADE;
