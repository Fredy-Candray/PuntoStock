-- Ampliación del perfil; no modifica credenciales, roles ni datos de ventas.
SET XACT_ABORT ON;
IF DB_NAME() <> N'PuntoStock' AND NOT (LEN(DB_NAME())=46 AND LEFT(DB_NAME(),14)=N'PuntoStock_qa_' AND SUBSTRING(DB_NAME(),15,32) COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^0-9a-f]%') THROW 51000, 'Esta migración solo admite PuntoStock o una base desechable de pruebas identificada.', 1;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.usuarios','nombre') IS NULL ALTER TABLE dbo.usuarios ADD nombre nvarchar(160) NULL;
IF COL_LENGTH('dbo.usuarios','correo') IS NULL ALTER TABLE dbo.usuarios ADD correo nvarchar(254) NULL;
IF COL_LENGTH('dbo.usuarios','telefono') IS NULL ALTER TABLE dbo.usuarios ADD telefono nvarchar(30) NULL;
IF COL_LENGTH('dbo.usuarios','foto_perfil') IS NULL ALTER TABLE dbo.usuarios ADD foto_perfil varchar(max) NULL;
IF COL_LENGTH('dbo.usuarios','foto_portada') IS NULL ALTER TABLE dbo.usuarios ADD foto_portada varchar(max) NULL;
-- No se inventa una fecha de alta para cuentas existentes.
IF COL_LENGTH('dbo.usuarios','fecha_creacion') IS NULL ALTER TABLE dbo.usuarios ADD fecha_creacion datetime2(6) NULL;
COMMIT;
