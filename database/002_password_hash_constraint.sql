-- Ejecutar en PuntoStock después de reparar cualquier contraseña guardada como texto.
SET XACT_ABORT ON;
IF DB_NAME() <> N'PuntoStock'
    THROW 51000, 'Seleccione la base PuntoStock antes de aplicar esta migracion.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name=N'ck_usuarios_password_bcrypt' AND parent_object_id=OBJECT_ID(N'dbo.usuarios'))
BEGIN
    ALTER TABLE dbo.usuarios WITH CHECK ADD CONSTRAINT ck_usuarios_password_bcrypt CHECK (
        DATALENGTH(password_hash)=60
        AND LEFT(password_hash,4) COLLATE Latin1_General_100_BIN2 IN ('$2a$','$2b$','$2y$')
        AND SUBSTRING(password_hash,5,2) LIKE '[0-3][0-9]'
        AND TRY_CONVERT(int,SUBSTRING(password_hash,5,2)) BETWEEN 4 AND 31
        AND SUBSTRING(password_hash,7,1)='$'
        AND SUBSTRING(password_hash,8,53) COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^./A-Za-z0-9]%'
    );
END;
