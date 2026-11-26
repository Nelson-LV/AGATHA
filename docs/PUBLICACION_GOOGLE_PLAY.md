# Publicación en Google Play

## Estado de la firma

- `release` (`com.hivend.agatha`) y `qa` (`com.hivend.agatha.qa`) se firman con el keystore
  de release, **nunca** con la llave de debug. El keystore vive fuera del repositorio.
- `app/build.gradle.kts` lee cuatro valores de `local.properties` o de variables de entorno:

  ```properties
  RELEASE_STORE_FILE=C:/ruta/fuera/del/repo/agatha-release.jks
  RELEASE_STORE_PASSWORD=...
  RELEASE_KEY_ALIAS=...
  RELEASE_KEY_PASSWORD=...
  ```

- Si falta alguno y se pide `assemble`/`bundle`/`appDistributionUpload` de `Release` o `Qa`,
  el build falla de entrada con "Falta la firma de release". Así nunca sale un artefacto sin
  firmar. `assembleDebug`, lint y pruebas no los necesitan.
- En GitHub Actions los mismos valores vienen de los secrets `RELEASE_KEYSTORE_BASE64`,
  `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` y `RELEASE_KEY_PASSWORD` (ya cargados);
  `android-distribute.yml` decodifica el keystore en `$RUNNER_TEMP` y lo borra al final.

## Generar el AAB para Play

Google Play solo acepta Android App Bundle (`.aab`):

```powershell
./gradlew bundleRelease
```

Sale en `app/build/outputs/bundle/release/app-release.aab`, firmado con el keystore de
release. El `versionCode` es el número de commits de la rama: cada subida a Play debe tener
uno mayor que la anterior, así que se construye siempre desde `master` actualizado.

## Play App Signing

Play firma lo que instala en los celulares con la **llave de firma de la app**, que guarda
Google. Nosotros firmamos el AAB con la **llave de subida** (upload key); si se pierde o se
filtra, Google permite cambiarla. Al crear la app en Play Console hay dos caminos:

1. **Llave generada por Google** (lo más simple). El keystore actual pasa a ser la llave de
   subida. Consecuencia: la app de Play queda firmada con otra llave que los APK de Firebase
   App Distribution, así que un celular que ya tiene `com.hivend.agatha` instalado desde
   Firebase no puede actualizarse desde Play: hay que desinstalar (y se pierden los registros
   offline sin sincronizar). AGATHA QA (`.qa`) no se ve afectada.
2. **Subir el keystore actual como llave de firma de la app** (Play Console > "Usar una llave
   diferente" > exportar con la herramienta PEPK que indica Play). Los celulares con la versión
   de Firebase se actualizan desde Play sin desinstalar. Después se genera una llave de subida
   nueva, que reemplaza al keystore en `local.properties` y en los secrets del CI:

   ```powershell
   keytool -genkeypair -v -keystore agatha-upload.jks -alias agatha-upload -keyalg RSA -keysize 4096 -validity 10000
   ```

Guarda cada `.jks` y sus contraseñas en un gestor de contraseñas con respaldo. Nunca en el
repositorio, en el chat ni en un correo.

## Huellas SHA después de activar Play App Signing

Play Console > Integridad de la app > Firma de apps muestra los SHA-1 y SHA-256 de la llave de
firma de la app y de la llave de subida. Hay que registrar la de **firma de la app** donde se
restrinja por huella:

- Google Cloud Console: la API key de Maps (`MAPS_API_KEY`) si está restringida a apps
  Android, con el paquete `com.hivend.agatha` y el SHA-1 de Play.
- Firebase Console > Configuración del proyecto > app `com.hivend.agatha`: agregar SHA-1 y
  SHA-256 de Play (FCM no los exige, pero App Check, Auth o Dynamic Links sí, si se usan
  más adelante).

Para ver la huella del keystore actual:

```powershell
keytool -list -v -keystore C:\ruta\agatha-release.jks -alias <alias>
```
