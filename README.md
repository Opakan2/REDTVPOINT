# REDTVPOINT v0.1 — GitHub Actions

## Obtener el APK sin Android Studio
1. Descomprime el ZIP y crea en GitHub un repositorio llamado REDTVPOINT (puede ser privado).
2. Usa Add file → Upload files (o uploading an existing file). Sube TODO el contenido de la carpeta REDTVPOINT, incluida .github, y pulsa Commit changes. No subas el ZIP ni la carpeta contenedora: app, gradle, .github y gradlew deben quedar en la raíz. Activa la vista de archivos ocultos si hace falta.
3. Abre Actions → Build REDTVPOINT → Run workflow → Run workflow. También se ejecuta automáticamente al subir cambios; puedes usar esa ejecución.
4. Cuando termine en verde, abre la ejecución y descarga Artifacts → REDTVPOINT-debug. Descomprime ese artifact para obtener REDTVPOINT-debug.apk.

Si no aparece el workflow, comprueba que .github/workflows/build-apk.yml está en la rama predeterminada. GitHub no descomprime los ZIP subidos.

El artifact dura 14 días. Es un APK debug firmado automáticamente para pruebas. Las claves debug pueden cambiar entre ejecuciones: puede ser necesario desinstalar la versión anterior (perdiendo sus datos) antes de instalar otra. Para distribución y actualizaciones estables hace falta una clave propia. Esto no crea un enlace público ni código de Downloader.

## Cambios
- Wrapper oficial completo Gradle 8.13, con checksum SHA-256 de la distribución.
- AGP 8.13.0 y Kotlin/Compose compiler 2.2.10. Java y Kotlin alineados a JDK 17.
- compileSdk 36, targetSdk 35, minSdk 23. Actions instala plataforma 36 y Build Tools 35.0.0.
- AndroidX activado. Compose BOM 2025.08.00 y Activity Compose 1.10.1 fijados como base estable para el compilador elegido.
- Añadidos Material 3 y Foundation que usa el código; retiradas TV Material y Lifecycle Compose no utilizadas.
- Añadido Media3 HLS 1.8.0 para los enlaces .m3u8 de TV en vivo.
- Workflow manual y por push: :app:assembleDebug, validación del wrapper, caché y publicación del APK. Falla si no existe el APK.
- .gitignore y finales de línea para Windows/Linux.

## Verificación
SHA-256 del JAR verificado contra el publicado por Gradle; principales dependencias comprobadas en Google Maven/Maven Central; estructura del ZIP comprobada, incluida .github.
No se ejecutó una compilación completa local: este equipo no tiene Java ni Android SDK disponibles. La primera ejecución en Actions debe confirmar el build. No se ha probado en Fire Stick.

## Alcance original conservado
Login Xtream, inicio, catálogo en vivo y películas con reproducción, y catálogo de series. Las series aún no reproducen episodios; EPG, favoritos, búsqueda y ajustes siguen pendientes. La interfaz original usa Compose Material 3 y requiere pruebas con mando. No incluye contenido ni credenciales precargadas. Las credenciales se mantienen en memoria.

## Terminal opcional
Con JDK 17 y Android SDK, define ANDROID_HOME o sdk.dir en local.properties y ejecuta ./gradlew :app:assembleDebug (Windows: gradlew.bat :app:assembleDebug).
Salida: app/build/outputs/apk/debug/app-debug.apk.

## Referencias
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://android-developers.googleblog.com/2025/08/whats-new-in-jetpack-compose-august-25-release.html
- https://github.com/gradle/actions
- https://github.com/actions/upload-artifact
