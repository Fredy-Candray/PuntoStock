# Recuperación de contraseña por Gmail

Aplicar `database/005_password_recovery.sql` antes de iniciar la nueva versión del backend. El inicializador local ya incluye esta migración. La base restaurada desde respaldos anteriores también necesita la migración.

Cada cuenta debe tener su correo correcto en Mi perfil. El formulario público solicita usuario y correo registrado; no revela si existe una cuenta. Los enlaces vencen en 15 minutos y solo pueden usarse una vez. Se conserva únicamente el hash SHA-256 del token. Cada cuenta puede recibir como máximo un enlace cada dos minutos. Un restablecimiento invalida sus sesiones JWT anteriores.

## Configuración en el equipo que ejecuta el backend

Usar una cuenta Gmail con verificación en dos pasos y una contraseña de aplicación. No usar la contraseña normal ni guardar la contraseña de aplicación en Git, capturas o mensajes. La función permanece desactivada hasta que se configure.

En PowerShell 7, antes de iniciar el backend:

La opción local más sencilla guarda la contraseña cifrada con DPAPI en `.cache/local/mail.clixml` (excluida de Git):

```powershell
./scripts/Set-LocalMail.ps1 -GmailUser 'direccion-remitente@gmail.com'
```

El asistente solicita la contraseña de aplicación de forma oculta; no manda correos. Reiniciar el backend después. Ese archivo no se puede trasladar a otro usuario/equipo: ejecutar el asistente en cada equipo. Para otra URL del frontend, pasar `-PublicUrl 'https://tu-direccion'`.

Como alternativa temporal, configurar variables de entorno:

```powershell
$env:MAIL_USERNAME = 'direccion-remitente@gmail.com'
$gmailSecret = Read-Host 'Contraseña de aplicación de Gmail' -AsSecureString
$env:MAIL_PASSWORD = [Net.NetworkCredential]::new('', $gmailSecret).Password
$env:APP_PUBLIC_URL = 'http://127.0.0.1:14200'
$env:RECOVERY_ENABLED = 'true'
./scripts/Start-Backend.ps1
```

Estas variables pertenecen solo a esta terminal y sus procesos hijos. Al cerrar el proceso, volver a configurarlas para otro arranque. `APP_PUBLIC_URL` debe ser la dirección de confianza del frontend que usarán los destinatarios; loopback funciona únicamente si abren el enlace en el mismo equipo del sistema. Para un despliegue accesible desde otros equipos, configurar su URL HTTPS real. No se toma esta URL de cabeceras o parámetros enviados por un visitante.

Sin remitente o sin configuración habilitada, el formulario informa que la recuperación no está disponible. Si Gmail rechaza la conexión, se muestra un error y se revierte la solicitud. No se simulan envíos exitosos ni se imprimen enlaces o tokens en logs.

## Verificación pendiente con la cuenta real

Solicitar un enlace para una cuenta propia con correo registrado, revisar también Spam, definir una contraseña de al menos 12 caracteres (máximo 72 bytes UTF-8), entrar con ella y comprobar que el mismo enlace no funciona otra vez. Las pruebas automatizadas usan un emisor simulado y no envían correos reales.

El limitador por cuenta no reemplaza un límite global por IP en el proxy antes de exponer el sistema a Internet.
