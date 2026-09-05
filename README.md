# REDTVPOINT 0.3.0 — Envío desde iPhone

Incluye todos los cambios de v0.2 (acceso por mando, icono/banner, búsqueda, favoritos de canales y películas, episodios) y un receptor local de vídeos.

## Cómo usarlo
1. Compila e instala esta versión. El APK anterior no tiene esta función.
2. Conecta iPhone y Fire Stick a la misma Wi-Fi.
3. Abre Desde iPhone en el menú o Enviar vídeo desde iPhone en la pantalla de acceso. No hace falta cuenta IPTV.
4. Pulsa Activar recepción. La TV muestra una dirección http:// y un código nuevo de seis cifras.
5. Escribe esa dirección en Safari del iPhone. Introduce el código de la TV y selecciona un vídeo de Fotos/Archivos, o pega un enlace directo de vídeo.
6. Pulsa Enviar y mantén Safari abierto. Se muestra el porcentaje de transferencia. El vídeo se copia primero y luego comienza en la TV. Usa el mando para controlar la reproducción.

Máximo 500 MB y sujeto al espacio libre del Fire Stick. El formato depende del decodificador del dispositivo; MP4/H.264 ofrece buena compatibilidad. MOV/HEVC de iPhone puede no reproducirse en todos los modelos. No hay conversión de formatos.
No es AirPlay, duplicación de pantalla ni el botón de emisión de otras aplicaciones. Un enlace a una página de YouTube/Netflix no es un enlace directo reproducible. No envía vídeos protegidos por DRM.
Si Safari no conecta, comprueba que ambos dispositivos están en la misma red y que no es una red de invitados con aislamiento entre dispositivos. La dirección y el código pueden cambiar al volver a activar la recepción.

## Recepción y almacenamiento
Solo se activa a petición. Se detiene al salir, pasar a segundo plano o comenzar una reproducción. Hay que activarla otra vez para enviar otro vídeo. El código no es el código de Downloader.
Los envíos requieren el código y no aceptan solicitudes desde otros orígenes web. Después de diez códigos incorrectos se debe reiniciar la recepción. Límite de cabeceras, tamaño y tiempo de espera; los archivos se escriben por bloques, sin cargarlos enteros en memoria. Los archivos recibidos no se ofrecen para descarga. Se borran al salir del reproductor o iniciar una nueva recepción. La transferencia usa HTTP en la red local, no un servicio en la nube: úsala en tu Wi-Fi de confianza.

## Actualizar GitHub y descargar
1. Extraer todo del ZIP. Abrir REDTVPOINT y subir su contenido conservando app, gradle y .github mediante Code > Add file > Upload files > Commit changes.
2. Actions ejecuta pruebas del receptor (:app:testDebugUnitTest) y compila (:app:assembleDebug).
3. Cuando esté en verde, descargar REDTVPOINT-debug en Artifacts y extraer el APK.
4. Crear una release v0.3.0 con REDTVPOINT-debug.apk. Crear un código Downloader para ese nuevo enlace: el código anterior apunta a la versión antigua.

La firma sigue siendo debug; no se dispone de la clave original. Si Android rechaza actualizar por firma distinta, será necesaria una firma compatible o desinstalar la anterior, lo que elimina sus datos. Todavía no hay firma permanente ni actualización automática.

## Validación y pruebas pendientes
Comprobado localmente el JavaScript del formulario con solicitudes simuladas: código obligatorio, protocolo válido, límite de tamaño, rutas de envío y estado final. Comprobada la estructura del ZIP.
Incluidas cuatro pruebas JVM del receptor: código/origen inválidos, página/enlace válido, tamaño/protocolo inválidos, y conservación exacta de bytes sin descarga pública del archivo. Se ejecutarán en Actions. No se han ejecutado aquí porque no hay JDK/SDK Android.
NO se ha compilado ni probado esta versión en Fire Stick/iPhone. Hay que verificar en dispositivos: código incorrecto, vídeo corto MP4, cancelar envío, detener receptor, reproducir enlace directo, salir del reproductor y enviar otro vídeo. Repetir las pruebas de campos con mando de v0.2.
EPG, perfiles, historial y PIN parental siguen pendientes.

Referencias de compatibilidad:
https://developer.apple.com/library/archive/documentation/AppleApplications/Reference/SafariWebContent/CreatingContentforSafarioniPhone/CreatingContentforSafarioniPhone.html
https://developer.android.com/media/media3/exoplayer/supported-formats
