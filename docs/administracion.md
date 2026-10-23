# Usuarios y menú

Estas funciones toman como referencia el comportamiento de COFI y están adaptadas a los roles y permisos de PuntoStock. COFI no se modifica.

## Instalación y actualización

Ejecutar `database/006_menu_administrable.sql` después de las migraciones anteriores y reiniciar el backend. El inicializador local ya la incluye. La migración conserva los datos existentes y crea la configuración inicial con las pantallas actuales más Usuarios y Administrar menú.

## Usuarios

Solo un administrador puede listar, consultar, crear, editar, desactivar o reactivar cuentas. El directorio permite buscar por nombre, usuario o correo y filtrar por rol y estado; muestra fecha de alta, contacto y foto al consultar un perfil.

Las contraseñas nuevas requieren al menos 12 caracteres y como máximo 72 bytes UTF-8. Al editar, una contraseña vacía conserva la actual. Las respuestas nunca contienen contraseñas ni hashes. Cambiar contraseña, nombre de usuario, rol o estado revoca las sesiones anteriores. Cambiar el correo invalida los enlaces de recuperación pendientes.

La desactivación es lógica: conserva ventas, movimientos y perfil. Una reactivación requiere un nuevo ingreso. No se permite desactivar la propia cuenta, cambiar su identificador ni quitarse el rol de administrador desde este módulo. La administración de cuentas serializa los cambios sobre el rol ADMIN y vuelve a comprobar al actor después de tomar el bloqueo, para impedir que dos administradores se desactiven entre sí simultáneamente y dejen el sistema sin acceso administrativo.

API: GET/POST `/api/usuarios`, GET/PUT `/api/usuarios/{id}`, PUT `/api/usuarios/{id}/activo?valor=true|false` y DELETE `/api/usuarios/{id}` (desactivación, sin borrado físico). El POST antiguo con usuario, contraseña y rol conserva compatibilidad; los formularios nuevos también piden nombre y correo.

## Administrar menú

Permite agregar o quitar secciones y opciones, modificar títulos, etiquetas, íconos, orden y estado, y elegir qué roles ven cada opción. Las pantallas se seleccionan de un catálogo de rutas existentes; configurar el menú no crea páginas nuevas. La vista lateral se actualiza al guardar, y otras sesiones recargan su menú al navegar o actualizar.

Los permisos de la API y los guards administrativos se conservan. Una opción administrativa no puede asignarse a Vendedor. Las rutas ocultas se rechazan también en el guard de navegación; Mi perfil siempre permanece disponible desde la cuenta. El menú no sustituye la autorización del backend.

Debe permanecer activa una opción `/administrar-menu` dentro de una sección activa y visible para ADMIN. Se rechazan rutas e identificadores duplicados, pantallas desconocidas, íconos no admitidos y órdenes fuera de rango. El documento completo se guarda transaccionalmente y con versión optimista: si otra sesión lo cambió, se debe volver a cargar antes de guardar.

API: GET/PUT `/api/menu-admin` para ADMIN y GET `/api/auth/me/menu` para la navegación de la cuenta autenticada. Se persiste en `menu_configuracion`, con un documento JSON validado y una columna de versión.
