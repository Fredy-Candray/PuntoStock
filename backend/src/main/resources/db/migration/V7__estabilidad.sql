CREATE TABLE sesiones (
 id varchar(36) NOT NULL PRIMARY KEY, usuario_id bigint NOT NULL REFERENCES usuarios(id),
 renovacion_hash varchar(64) NOT NULL, expira_epoch bigint NOT NULL, auth_version bigint NOT NULL, recordar bit NOT NULL);
CREATE INDEX ix_sesiones_expira ON sesiones(expira_epoch);
CREATE TABLE correo_pendiente (
 id bigint IDENTITY PRIMARY KEY, usuario_id bigint NOT NULL REFERENCES usuarios(id),
 recuperacion_hash varchar(64) NOT NULL, contenido_cifrado varchar(max) NULL,
 expira_epoch bigint NOT NULL, proximo_epoch bigint NOT NULL, intentos int NOT NULL DEFAULT 0,
 estado varchar(20) NOT NULL DEFAULT 'PENDIENTE', lease_epoch bigint NOT NULL DEFAULT 0);
CREATE INDEX ix_correo_pendiente ON correo_pendiente(estado,proximo_epoch);
