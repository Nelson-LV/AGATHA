# AGATHA — App móvil de campo

Aplicación Android nativa (Kotlin + Jetpack Compose) para el personal de campo de 0G
Colombia que atiende las alertas tempranas generadas por los sensores IoT del sistema
AGATHA sobre el gasoducto Otero-Santana. Cubre las épicas **HE-04 a HE-08** del backlog del
proyecto: recepción de alertas, inspección y clasificación de eventos, evidencia
fotográfica, sincronización offline-first y mapa de sensores con historial.

📄 **La documentación completa de arquitectura, patrones de diseño, buenas prácticas y el
sistema visual (paleta Material 3) vive en
[`docs/ARQUITECTURA_Y_DISENO.md`](docs/ARQUITECTURA_Y_DISENO.md). Léelo antes de tocar
código.**

## Requisitos

- Android Studio Narwhal o más reciente.
- JDK 17 (Gradle 8.14.5 lo exige para correr el build).
- Un dispositivo o emulador con **Android 13 (API 33) o superior**.

## Cómo abrir el proyecto

1. Abre esta carpeta en Android Studio ("Open" → selecciona `AGATHA - App`).
2. Espera la sincronización de Gradle (descarga Gradle 8.14.5 la primera vez).
3. Si compileSdk 36 no está instalado, Android Studio lo ofrecerá automáticamente durante
   el sync ("Install missing platform(s) and sync").
4. Ejecuta la configuración `app` sobre un emulador/dispositivo Android 13+.

## Compilar desde la línea de comandos

```bash
./gradlew :app:assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estado actual del proyecto

Esta fase entrega: estructura del proyecto, paleta Material 3, y las 9 pantallas del
prototipo de Figma completamente navegables (con datos de ejemplo — ver
`docs/ARQUITECTURA_Y_DISENO.md` § 7). Todavía no están conectadas la API central, Room,
WorkManager, Firebase Cloud Messaging ni el SDK real de Google Maps: esos puntos están
documentados y con las dependencias ya declaradas, listos para el sprint de integración.

### Configuración local antes de compilar

Crea (o edita) `local.properties` en la raíz del proyecto — Android Studio lo genera solo
al abrir el proyecto — y agrega tu SDK y, si vas a activar el mapa real más adelante, tu
clave de Google Maps:

```properties
sdk.dir=/ruta/a/tu/Android/Sdk
MAPS_API_KEY=
```

`local.properties` nunca se sube al repositorio (ver `.gitignore`).
