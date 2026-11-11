# Restablecimiento local de contraseña de vendedores

En Usuarios, un administrador puede usar la llave «Restablecer contraseña» de un vendedor activo. Un modal identifica la cuenta, informa que la clave temporal será `puntostock` en minúsculas y permite cancelar antes de aplicar. No se envían correos y no se contactan servicios externos. No se admite esta acción para administradores, cuentas con ambos roles ni vendedores inactivos.

El restablecimiento guarda exclusivamente el hash BCrypt de la clave temporal, marca `cambio_clave_pendiente` y aumenta `auth_version`. Invalida las sesiones anteriores (JWT y renovación) y los enlaces previos de recuperación. No reactiva cuentas ni modifica roles, ventas o datos de perfil. No cambia automáticamente las contraseñas de vendedores existentes: solo se aplica al confirmar la acción de una cuenta concreta.

Al ingresar o restaurar una sesión con cambio pendiente, la interfaz redirige a `/cambiar-clave`. El servidor asigna únicamente la autoridad `CAMBIAR_CLAVE`, por lo que no se puede acceder a ventas, productos, perfil, menú o administración hasta completar el cambio. Solo se permiten el cambio inicial, la renovación de esa sesión limitada y cerrar sesión. Las rutas públicas de salud/recuperación mantienen sus reglas actuales.

La nueva contraseña debe repetirse, tener al menos 8 caracteres y ocupar como máximo 72 bytes en UTF-8. Tras guardarla se quita la marca, vuelven a invalidarse los accesos temporales y se solicita ingresar con la contraseña personal. La clave temporal deja de funcionar. Si se cierra la app antes de cambiarla, la obligación persiste en la base.

## API y migración

- `POST /api/usuarios/{id}/restablecer-clave`: ADMIN; solo vendedor activo sin ADMIN. Sin contraseña en el cuerpo, para evitar convertirlo en un cambio arbitrario con clave corta.
- `POST /api/auth/cambiar-clave-inicial`: JWT limitado con cambio pendiente; cuerpo `{ "password": "...", "confirmacion": "..." }`.
- Login/renovación agregan `cambioClavePendiente` a la respuesta. El indicador persistido determina la restricción en cada petición, no un valor enviado por el cliente.
- V12 agrega `usuarios.cambio_clave_pendiente bit NOT NULL DEFAULT 0`. Los hashes y datos actuales se conservan. Usar Flyway; no ejecutar el SQL de referencia si la migración ya está aplicada.

Los bloqueos de usuario serializan cambios simultáneos y el control de versión de autenticación invalida sesiones sin depender de borrar cookies en dispositivos ajenos. Las actualizaciones parciales de la entidad evitan que guardar datos de perfil sobrescriba un restablecimiento concurrente.

La contraseña temporal es conocida: entregar el acceso directamente al vendedor y pedirle que complete el cambio cuanto antes. Esta acción es para recuperar acceso, no para consultar contraseñas.
