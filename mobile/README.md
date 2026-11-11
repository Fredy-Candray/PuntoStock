# PuntoStock: APK de prueba local por Wi-Fi personal o USB

La aplicación Android muestra el sistema que corre en la computadora mediante WebView. Requiere Android 8 o posterior y Android System WebView ya instalado. No incluye una base SQL en el teléfono y necesita mantener la computadora y sus servidores encendidos. Las operaciones se guardan en la base configurada en esa computadora.

## Probar sin cable en tu red personal

1. Conectar computadora y teléfono al mismo Wi-Fi personal. Iniciar el backend de PuntoStock normalmente en la computadora.
2. Consultar `ipconfig` y obtener la IPv4 de Wi-Fi. Desde la raíz ejecutar `powershell -NoProfile -ExecutionPolicy Bypass -File mobile/Iniciar-Wifi.ps1 -Direccion 192.168.1.10`, sustituyendo la IP. El servidor se enlaza solo con esa dirección en el puerto 14201 y envía `/api` al backend en loopback; no publica SQL ni el backend directamente.
3. Copiar `.cache/android-local/PuntoStock-local.apk` al teléfono por un medio local e instalarla. Si Android solicita permiso, permitir instalar únicamente desde el administrador de archivos que se está usando. Se puede actualizar la APK de USB con esta versión conservando la misma firma.
4. Abrir **PuntoStock local**, pulsar **Conexión** e indicar la dirección que muestra el script, por ejemplo `http://192.168.1.10:14201/`. Se guarda para las próximas aperturas. La APK preparada puede incluir la IP privada inicial; si cambia, modificarla allí.
5. Probar primero el enlace en Chrome del teléfono. Si no responde, revisar que no sea Wi-Fi de invitados ni tenga aislamiento de clientes y que el firewall permita el puerto 14201 solo en la red privada. El script no cambia el firewall automáticamente. No desactivar el firewall ni abrir puertos del router.
6. Mantener la computadora y el servidor encendidos durante la prueba. Al cerrar el servidor o salir de ese Wi-Fi, la app deja de conectar; muestra **Volver a intentar** y **Cambiar servidor**. No necesita acceso a Internet ni túneles. Usa HTTP únicamente dentro de la red personal de confianza.

El botón **Recargar** vuelve a cargar la vista. El modo USB anterior se conserva indicando `http://127.0.0.1:14200/` en Conexión y usando `Conectar-USB.ps1`.

## Compilar sin Internet

Ejecutar `python mobile/build-local.py` desde la raíz, usando un Python local. Opcionalmente incluir `--server http://IP_PRIVADA:14201/` para precargar el servidor Wi-Fi. Requiere JDK 21, Android SDK Platform 35 y Build Tools 35.0.0 ya instalados. Admite `ANDROID_HOME` y `PUNTOSTOCK_JDK` para otras ubicaciones. No descarga dependencias ni utiliza Gradle, servicios de compilación, analítica o túneles. Produce `.cache/android-local/PuntoStock-local.apk` y `SHA256.txt`. La clave de prueba y su contraseña quedan en `.cache/local/`, excluidas de Git. Conservarlas permite actualizar la APK sin desinstalar.

## Probar en el teléfono

1. Iniciar normalmente el backend y frontend de PuntoStock en la computadora (puertos 18080 y 14200). El frontend mantiene el proxy local `/api`.
2. En el teléfono, habilitar Opciones de desarrollador y Depuración USB. Conectarlo mediante un cable de datos y aceptar la autorización de la computadora en el teléfono.
3. Desde la raíz, ejecutar `powershell -NoProfile -ExecutionPolicy Bypass -File mobile/Conectar-USB.ps1 -Instalar`. Si hay varios dispositivos, agregar `-Device SERIAL` con el identificador que muestra `adb devices`.
4. Abrir **PuntoStock local** e iniciar sesión con la cuenta habitual. No es necesario Wi-Fi ni datos móviles para la conexión al sistema. Se puede desactivar ambos en el teléfono para comprobarlo.
5. Al desconectar o reiniciar el teléfono, ejecutar nuevamente el script sin `-Instalar`. Para retirar la conexión: `adb reverse --remove tcp:14200`. Al terminar las pruebas, desactivar la depuración USB en el teléfono.

La APK permite exclusivamente el origen privado configurado; valida IPv4 RFC1918 o loopback y bloquea navegación y recursos de otros servidores. No admite nombres de dominio públicos ni crea enlaces públicos. En USB, la dirección loopback del teléfono llega a la computadora mediante `adb reverse`. En Wi-Fi, el script atiende en la interfaz privada elegida. Las imágenes externas no se cargan; las guardadas en la base sí. El correo Gmail del backend es una configuración independiente: no solicitar recuperación por correo durante una prueba sin red.

Incluye selección de imágenes del dispositivo, confirmaciones Android y guardado de PDF usando el selector de archivos. La impresión y el compartir PDF por aplicaciones externas requieren validación específica en un dispositivo real; WebView no garantiza las mismas funciones que Chrome. No es una aplicación autónoma ni una publicación de producción. No contiene contraseñas de PuntoStock ni credenciales de SQL/Gmail.

## Comprobaciones

La compilación ejecuta 18 casos de validación de direcciones y verifica la firma con `apksigner` y el paquete con `aapt`. Antes de usar datos del negocio, comprobar en el móvil: conexión local (Wi-Fi o USB), login, categorías/productos, desplazamiento, modal de consulta, selección de imagen, descargar PDF y conexión perdida al retirar USB o desconectar el Wi-Fi. No se ejecutan ventas reales automáticamente al compilar o instalar.
