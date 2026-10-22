SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.usuarios','auth_version') IS NULL
 ALTER TABLE dbo.usuarios ADD auth_version bigint NOT NULL CONSTRAINT DF_usuarios_auth_version DEFAULT 0;
IF COL_LENGTH('dbo.usuarios','recuperacion_hash') IS NULL
 ALTER TABLE dbo.usuarios ADD recuperacion_hash varchar(64) NULL;
IF COL_LENGTH('dbo.usuarios','recuperacion_expira') IS NULL
 ALTER TABLE dbo.usuarios ADD recuperacion_expira datetime2(6) NULL;
IF COL_LENGTH('dbo.usuarios','recuperacion_solicitada') IS NULL
 ALTER TABLE dbo.usuarios ADD recuperacion_solicitada datetime2(6) NULL;
COMMIT;
