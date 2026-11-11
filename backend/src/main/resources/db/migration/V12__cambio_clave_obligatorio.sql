ALTER TABLE dbo.usuarios ADD cambio_clave_pendiente bit NOT NULL
  CONSTRAINT df_usuarios_cambio_clave_pendiente DEFAULT 0 WITH VALUES;
