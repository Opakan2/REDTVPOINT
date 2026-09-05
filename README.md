# REDTVPOINT 0.3.1 — Corrección de acceso Xtream

Corregido un rechazo local confirmado: la versión anterior exigía http:// o https://, por lo que dominio:puerto nunca llegaba al servidor. Ahora se admite ese formato (HTTP por defecto, HTTPS para puerto 443). Se respeta cualquier protocolo escrito explícitamente y se muestra la dirección normalizada en el campo.
También se normalizan enlaces get.php/player_api.php/xmltv.php, conservando subcarpetas y eliminando parámetros. Los campos Usuario y Contraseña siguen siendo los utilizados; no se importan credenciales de la URL.
Se aceptan auth=1, "1", true y "true"; los valores ausentes o negativos no autorizan acceso. Conexión y catálogo usan el mismo cliente, con timeouts definidos. Se mantienen la validación TLS y la contraseña exacta. Errores de dirección, DNS, puerto, HTTPS, HTTP y JSON se distinguen sin mostrar URLs con credenciales.
Se desactivan sugerencias del teclado al editar campos de acceso.

Incluye las funciones de v0.3: envío de vídeos desde iPhone y mejoras de mando/icono, búsqueda, favoritos y episodios.

## Compilar
Subir TODO el contenido de REDTVPOINT a la raíz del mismo repositorio, conservando app, gradle y .github. Actions ejecuta :app:testDebugUnitTest y :app:assembleDebug. Descargar REDTVPOINT-debug cuando termine en verde. Publicar el APK con una etiqueta nueva v0.3.1 si se quiere crear un nuevo código Downloader.

## Verificación
Incluidas pruebas JVM de normalización dominio:puerto, HTTPS, subcarpetas/enlaces de playlist, formato inválido, variantes auth y una autenticación Retrofit simulada que verifica endpoint, puerto y caracteres especiales en las credenciales. También conserva las pruebas del receptor iPhone.
NO se han ejecutado las pruebas JVM ni compilado esta versión localmente: faltan JDK/SDK y la política de red impide descargarlos. No se ha probado contra la cuenta real del usuario. La causa local del formato sin protocolo sí está identificada en el código anterior. Actions y una prueba en Fire Stick deben confirmar la nueva versión.

## Instalación
Se mantiene la firma debug; puede no coincidir con la versión instalada. No se ha creado una firma permanente. No desinstalar sin tener en cuenta que se perderán los datos locales.
