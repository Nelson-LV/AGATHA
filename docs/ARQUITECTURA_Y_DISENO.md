# AGATHA — Arquitectura, patrones de diseño, buenas prácticas y sistema visual

Este documento es la referencia viva de cómo está construida la app móvil de AGATHA
(épicas HE-04 a HE-08 del backlog del proyecto). Se actualiza en cada sprint que cambie la
estructura, un patrón o la paleta — no describe aspiraciones, describe el código tal como
está en `app/src/main/java/com/hivend/agatha`.

> **Backlog 1.3 (10/10/2026).** Las reglas funcionales cambiaron: la web pasa a ser la
> fuente de verdad y la app deja de tener estados propios. Este documento sigue
> describiendo el código actual; lo que hay que cambiar, las reglas nuevas y las
> referencias de la web están en [`BACKLOG_V1_3_APP_MOVIL.md`](BACKLOG_V1_3_APP_MOVIL.md).
> Las secciones afectadas (máquina de estados en §2 y reglas del flujo en §4) están
> marcadas.

## Índice

1. [Estructura del proyecto](#1-estructura-del-proyecto)
2. [Patrones de diseño](#2-patrones-de-diseño)
3. [Buenas prácticas de desarrollo](#3-buenas-prácticas-de-desarrollo)
4. [Navegación](#4-navegación)
5. [Sistema visual (Material 3)](#5-sistema-visual-material-3)
6. [Versiones del proyecto](#6-versiones-del-proyecto)
7. [Datos de ejemplo vs. API real](#7-datos-de-ejemplo-vs-api-real)

---

## 1. Estructura del proyecto

La app sigue una **arquitectura en capas** (presentación → dominio → datos), la misma que
recomienda la guía oficial de arquitectura de Android y la que ya está pactada en
`AGATHA_Decisiones_Tecnologicas_App_Movil.docx`: Kotlin + Jetpack Compose + Room +
WorkManager + Retrofit, con la lógica desacoplada de la UI para dejar abierta una futura
versión iOS vía Kotlin Multiplatform sin reescribir nada de esta capa de dominio.

```
app/src/main/java/com/hivend/agatha/
├── AgathaApplication.kt        # @HiltAndroidApp — raíz del grafo de dependencias
├── MainActivity.kt             # Única Activity (patrón Single-Activity)
│
├── core/                       # Código transversal, sin lógica de negocio propia
│   ├── di/                     # Módulos Hilt (bindings Repository → implementación)
│   ├── navigation/             # Rutas, NavHost, Scaffold raíz con bottom bar condicional
│   └── util/                   # Utilidades pequeñas y puras (p. ej. FileProvider para fotos)
│
├── domain/                     # Capa de dominio: NO depende de Android ni de Compose
│   ├── model/                  # Alert, Inspection, SensorNode, HistoryEvent, enums…
│   └── repository/             # Interfaces (puertos) que la UI consume
│
├── data/                       # Implementaciones concretas de los puertos de dominio
│   ├── repository/             # Hoy: InMemory*Repository con datos de muestra (§7)
│   ├── local/                  # Futuro: Room (AppDatabase, entidades, DAOs) — ver README interno
│   ├── remote/                 # Futuro: Retrofit (AgathaApiService, DTOs)
│   └── sync/                   # Futuro: WorkManager (SyncWorker)
│
└── ui/                         # Presentación: un paquete por pantalla + uno compartido
    ├── theme/                  # Color.kt, Type.kt, Shape.kt, Theme.kt — Material 3
    ├── components/             # Composables reutilizados por ≥2 pantallas
    ├── notification/           # HE-04 — vista previa de notificación push
    ├── alerts/                 # HE-04 — bandeja de alertas
    ├── alertdetail/            # HE-04 — detalle de alerta
    ├── inspection/             # HE-05 — formulario de inspección
    ├── evidence/                # HE-06 — evidencia fotográfica
    ├── sync/                   # HE-07 — sincronización offline
    ├── map/                    # HE-08 — mapa de nodos
    └── history/                # HE-08 — historial de dispositivo
```

**Regla de dependencia:** las flechas solo pueden apuntar hacia adentro.
`ui` → `domain` ← `data`. Ningún archivo de `ui/` importa una clase de `data/`; solo conoce
las interfaces de `domain/repository`. Esto es lo que permite que **todas las
implementaciones en memoria de `data/repository/` se reemplacen por Room + Retrofit en el
sprint de integración sin tocar una sola pantalla ni un ViewModel** — ver §7.

Cada paquete de `ui/<pantalla>/` sigue el mismo patrón de 2 archivos:
`<Pantalla>Screen.kt` (composable, sin lógica de negocio) + `<Pantalla>ViewModel.kt`
(estado + llamadas a repositorios). Un paquete nuevo (por ejemplo, para una futura pantalla
de login) debe replicar esta misma forma.

---

## 2. Patrones de diseño

| Patrón | Dónde se usa | Por qué |
|---|---|---|
| **Repository** | `domain/repository/*Repository` (interfaces) + `data/repository/InMemory*Repository` (implementación) | La UI y los ViewModels dependen de una interfaz, nunca de Room/Retrofit directamente. Cambiar el origen de datos (memoria → Room+API real) no requiere tocar `ui/`. Es el patrón que más impacto tendrá cuando se conecte la API central en el sprint 3. |
| **Singleton** (vía DI) | Todas las clases `InMemory*Repository` están anotadas `@Singleton` y se enlazan en `core/di/RepositoryModule.kt` | Todas las pantallas deben observar el **mismo** estado de alertas/sincronización; si cada pantalla creara su propia instancia del repositorio, un cambio de estado en una pantalla no se reflejaría en otra. Con Hilt nunca se escribe manualmente `object` ni `getInstance()`: el contenedor de Hilt garantiza una única instancia por proceso. |
| **Inyección de dependencias (Dependency Inversion)** | Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Module`/`@Binds`) | Desacopla la construcción de un objeto de su uso. Facilita además las pruebas unitarias: un test puede inyectar un `AlertRepository` falso sin tocar Hilt. |
| **MVVM (Model-View-ViewModel)** | Cada `*ViewModel.kt` expone `StateFlow<UiState>`; el `Screen.kt` solo lo colecta con `collectAsStateWithLifecycle()` | Separa el estado de pantalla (sobrevive rotaciones, se testea sin Compose) de su representación visual. Es el patrón de arquitectura recomendado oficialmente para Compose. |
| **Observer** | `kotlinx.coroutines.flow.StateFlow`/`Flow` en repositorios y ViewModels | Cuando `InMemoryAlertRepository` cambia una alerta, **todas** las pantallas suscritas (bandeja, detalle, notificación) se recomponen solas — nadie hace polling ni recarga manual. |
| **Máquina de estados (State Machine)** | `AlertStatus` (`GENERATED → RECEIVED → IN_INSPECTION → CLASSIFIED → CLOSED`) gobierna qué botones muestra `AlertDetailScreen` | Evita si-anidados dispersos por la UI: el estado válido siguiente se decide en un solo `when` (`PrimaryActions` en `AlertDetailScreen.kt`). **Backlog 1.3:** estos estados se eliminan (D-19); el patrón se conserva sobre el estado de gestión de la web (`Sin clasificar → Clasificada sin etiqueta → Clasificada con etiqueta`). Ver `BACKLOG_V1_3_APP_MOVIL.md` §2 y §6. |
| **Adapter implícito (mapper)** | Punto de extensión reservado en `data/remote` (DTO → `domain.model`) y `data/local` (Entity → `domain.model`) | Ningún DTO/Entity debe llegar nunca a `ui/`. Cuando se implemente, cada repositorio real mapea explícitamente. |
| **Factory** (delegado a Hilt) | `@HiltViewModel` + `hiltViewModel()` en cada `Screen.kt` | Hilt genera la fábrica de cada ViewModel (incluida la inyección de argumentos de navegación vía `SavedStateHandle`), evitando `ViewModelProvider.Factory` manuales. |

### Patrones deliberadamente NO usados todavía

- **Casos de uso / UseCase** (capa extra entre ViewModel y Repository): con operaciones tan
  directas como "leer alertas" o "guardar inspección", un UseCase de una sola línea sería
  indirection sin beneficio (YAGNI). Se introducirá si una regla de negocio empieza a
  combinar 2+ repositorios (p. ej. "cerrar alerta" que además debe cancelar sincronizaciones
  pendientes).
- **Dynamic Color / Material You**: ver §5.

---

## 3. Buenas prácticas de desarrollo

**Nomenclatura**
- Paquetes en inglés técnico (`ui`, `domain`, `data`) y también **nombres de dominio en
  inglés** (`Alert`, `AlertStatus`, `registerInspection`) — ver "Convención de idioma"
  abajo. El backlog y las conversaciones con 0G Colombia/HIVEND siguen en español; solo el
  código pasa a inglés.
- `Screen` para composables de pantalla completa, `ViewModel` para su estado, nunca
  abreviados (`VM`, `Scr`).

**Convención de idioma**
- Todo el código (clases, interfaces, enums, propiedades, parámetros, funciones,
  constantes de enum) se escribe en **inglés**, en las cuatro capas (`domain`, `data`,
  `ui`, `core`). Ejemplo del refactor de vocabulario hecho sobre HE-04..HE-08:

  | Antes (español) | Ahora (inglés) |
  |---|---|
  | `Alerta`, `EstadoAlerta`, `NivelAlerta`, `TelemetriaAlerta` | `Alert`, `AlertStatus`, `AlertLevel`, `AlertTelemetry` |
  | `Inspeccion`, `ResultadoInspeccion`, `CategoriaEvento` | `Inspection`, `InspectionResult`, `EventCategory` |
  | `EvidenciaFoto`, `NodoSensor`, `EventoHistorial`, `TipoEvento` | `PhotoEvidence`, `SensorNode`, `HistoryEvent`, `EventType` |
  | `EstadoSincronizacion`, `RegistroPendiente`, `ConflictoSincronizacion` | `SyncStatus`, `PendingRecord`, `SyncConflict` |
  | `AlertaRepository`, `HistorialRepository`, `NodoSensorRepository`, `SincronizacionRepository` | `AlertRepository`, `HistoryRepository`, `SensorNodeRepository`, `SyncRepository` |
  | `registrarInspeccion`, `observarAlertas`, `actualizarEstado`, `sincronizarAhora` | `registerInspection`, `observeAlerts`, `updateStatus`, `syncNow` |

- **Excepción — comentarios y KDoc**: pueden quedarse en español (es el idioma de trabajo
  del equipo). No se traducen como efecto secundario de un cambio no relacionado.
- **Excepción — cadenas de texto de la UI**: lo que ve el usuario de campo (`Text(...)`,
  `contentDescription`, mensajes) está en **español (idioma por defecto) e inglés** vía
  recursos de Android: `res/values/strings.xml` (español) + `res/values-en/strings.xml`
  (inglés). Ningún Composable ni ViewModel escribe texto visible como literal: se usa
  `stringResource(R.string.…)` / `pluralStringResource(...)`, y toda clave nueva se agrega
  en los dos archivos en el mismo cambio. Los enums del dominio no llevan texto; la UI los
  traduce a `@StringRes` en `ui/components/Labels.kt`. Las reglas completas están en
  `CLAUDE.md` § "User-facing strings". Los datos de ejemplo de `data/repository/` simulan
  contenido que enviará la API y no se traducen en la app.

**Compose**
- Un composable de pantalla jamás recibe un `NavController`: recibe funciones lambda
  (`onBack: () -> Unit`, `onAlertClick: (String) -> Unit`). Así cada `Screen.kt` se puede
  previsualizar y probar sin un grafo de navegación real.
- `ViewModel` nunca importa nada de `androidx.compose.*`: expone `StateFlow`, no `State` de
  Compose, para no atar la capa de presentación a un solo framework de UI.
- Los colores, tipografías y radios de esquina viven **solo** en `ui/theme/`. Ningún
  composable escribe un `Color(0xFF...)` suelto — si un color no existe todavía en
  `Color.kt`, se agrega ahí primero.

**Concurrencia**
- Toda operación que toque un repositorio desde la UI ocurre en `viewModelScope`
  (estructurado, se cancela solo con el ViewModel). Nunca se lanza una corrutina con
  `GlobalScope`.

**Offline-first (HE-07)**
- Cualquier pantalla que escriba datos de campo (inspección, evidencia) debe poder
  guardar sin conexión y mostrarlo de inmediato en la UI local — nunca esperar una
  respuesta de red para dar feedback. `InMemoryAlertRepository` ya modela esto: escribe
  primero en el estado local, la sincronización es un paso aparte (`data/sync`, futuro).

**Seguridad**
- Ninguna clave (Maps API key, credenciales de Firebase) se escribe en el repositorio: se
  leen desde `local.properties` (ignorado por git) en tiempo de compilación — ver
  `mapsApiKey` en `app/build.gradle.kts`.
- `res/xml/file_paths.xml` limita el `FileProvider` a una sola carpeta de cache; nunca se
  comparte una ruta `file://` directa a la app de cámara (requisito desde Android 7 y buena
  práctica frente a RNF-MOV-05).

**Control de versiones (cuando el equipo inicialice el repositorio Git)**
- Seguir el flujo ya descrito en la propuesta de desarrollo: rama `develop` + ramas
  `feature/*` por historia de usuario, integradas por PR; etiquetas `v0.1`, `v0.2`… por
  incremento de sprint.
- Nunca commitear `local.properties`, `google-services.json` ni cualquier `*.jks` de firma
  (ver `.gitignore`).
- **Commit por tarea, push por trabajo terminado**: al terminar cada tarea se verifica que
  compila y funciona (`./gradlew assembleDebug lintDebug testDebugUnitTest`, y prueba en
  emulador si toca UI) y se hace commit; cuando se termina la tarea grande completa se hace
  `git push`. Detalle en `CLAUDE.md` § "Git workflow".

**Verificación antes de dar por completada una tarea**
- Antes de decir que una pantalla o cambio "funciona", correr como mínimo
  `./gradlew :app:assembleDebug` (compila Kotlin, corre KSP/Hilt, empaqueta el APK). Si el
  cambio afecta UI, instalarlo en un emulador/dispositivo Android 13+ y probar el flujo
  real — un build verde certifica que compila, no que se ve o navega correctamente.

---

## 4. Navegación

Un solo `NavHost` (`core/navigation/AgathaApp.kt`), con una barra inferior que **solo se
muestra en las 5 pantallas raíz** — igual que en el prototipo de Figma, donde las pantallas
de detalle/formulario reemplazan la barra por una flecha de regreso. La pestaña
**Dispositivos** no estaba en el prototipo: lista todos los nodos con la misma ficha que el
mapa (`ui/components/DeviceInfoCard.kt`).

```
┌─ alerts ─┬─ devices ──┬─ map ─┬─ history/{deviceId} ─┬─ sync ─┐   ← bottom bar
│ (Alertas)│(Dispositivos)│(Mapa)│     (Historial)     │ (Sync) │
└────┬─────┴─────┬──────┴───┬───┴──────────────────────┴────────┘
     │ tap alerta │ "Ver historial completo" (lista o nodo del mapa)
     ▼            └──────────┴──────────► history/{deviceId}
alert_detail/{alertId}  ── "Cerrar alerta" → diálogo de confirmación → Cerrada
     │ botón verde "Iniciar / Continuar inspección"
     ▼
inspection_form/{alertId}
       │ "Guardar inspección"
       ▼
photo_evidence/{alertId}
       │ "Guardar evidencias" → pop hasta alert_detail/{alertId}
       ▼
   (vuelve al detalle, ya actualizado)
```

Decisión de diseño relevante: el prototipo de Figma dibuja **"Detalle de Alerta"** dos
veces (`Recibida` y `En inspección`) como si fueran pantallas distintas. En código es
**una sola** `AlertDetailScreen`, cuyos botones principales cambian según
`Alert.status` (`PrimaryActions` en el archivo). Duplicar la pantalla por cada estado
posible (5 estados) habría significado 5 composables casi idénticos y un bug garantizado
el día que alguien actualice uno y olvide los otros cuatro.

**Reglas del flujo de una alerta** (acordadas con el equipo de campo el 07/10/2026).

> **Backlog 1.3 choca con varias de estas reglas** (amarillo también es alerta, no existe
> "cerrar alerta", no hay estados `IN_INSPECTION`/`CLASSIFIED`, la etiqueta web es "Fuga
> confirmada en campo"). Siguen describiendo el código actual; el detalle y lo que falta
> confirmar están en `BACKLOG_V1_3_APP_MOVIL.md` §5.

- **Solo naranja y rojo son alertas.** El verde indica normalidad: esos nodos siguen en el
  mapa y en Dispositivos, pero no aparecen en la bandeja (`AlertLevel.requiresAttention`).
- **El botón verde siempre lleva a la inspección.** "Iniciar inspección" pasa la alerta a
  `IN_INSPECTION` y abre el formulario; "Continuar inspección" lo reabre. La alerta solo pasa
  a `CLASSIFIED` al guardar la inspección, nunca con un botón suelto.
- **Se puede cerrar sin inspección**, con un diálogo de confirmación. Las observaciones
  pendientes se guardan antes de cerrar.
- **Las observaciones viven en el detalle**, al mismo nivel que la información de la alerta
  (`Alert.observations`), no dentro del formulario de inspección.
- **La inspección es visual y superficial.** El personal de campo no tiene acceso directo a
  la tubería: el formulario prioriza clasificar la alerta y solo registra lo observado en
  superficie y en el dispositivo. Por eso no existe "Fuga confirmada" sino "Indicios de
  fuga", que confirma el operador.

---

## 5. Sistema visual (Material 3)

**Identidad.** Un azul "acero" profundo como color de marca (instrumentación industrial,
sobrio) sobre neutros fríos. Rojo, ámbar y verde quedan **reservados** a severidad y estado,
nunca a decoración, para que una alerta roja sea lo primero que ve el técnico. El logotipo
(tres barras) va en blanco sobre un cuadro azul de marca, igual que el ícono del launcher.
No se usan emojis en la interfaz: todo pictograma es un ícono Material
(`material-icons-extended`), que respeta el tinte del tema y no cambia según el fabricante.

**Claro y oscuro.** `ui/theme/Color.kt` define `AgathaColors`, una paleta **por roles**
(`brand`, `critical`, `stateInspection`, `offline`, `nodeCritical`…) con dos instancias,
`LightAgathaColors` y `DarkAgathaColors`. Las pantallas nunca usan un hex: leen
`AgathaTheme.colors.<rol>`, y `Theme.kt` deriva además el `ColorScheme` de Material 3 de esa
misma paleta, así los componentes de Material y los propios no se contradicen. En oscuro los
colores semánticos se aclaran y sus fondos se oscurecen para mantener contraste AA sin
deslumbrar de noche. La ventana de arranque (`values/` y `values-night/themes.xml`) usa el
mismo fondo para que no haya destello al abrir.

| Rol | Claro | Oscuro | Uso |
|---|---|---|---|
| `brand` | `#1D4ED8` | `#8AB0FF` | Logo, enlaces, acciones principales, pestaña activa |
| `background` | `#F3F5F8` | `#0D1117` | Fondo general |
| `surface` | `#FFFFFF` | `#161B22` | Tarjetas, encabezados, barra inferior |
| `textPrimary` / `textSecondary` | `#0F172A` / `#5A6474` | `#E7EBF0` / `#A8B1BD` | Texto |
| `critical` | `#C62828` | `#FF8A80` | Alerta roja, "Cerrar alerta" |
| `warning` / `stateInspection` | `#B45309` / `#9A4A06` | `#F5B054` | Alerta naranja, "En inspección", "Pendiente" |
| `positive` | `#15803D` | `#5BD68A` | "Iniciar/Continuar inspección", "Guardar", conectado |
| `stateClassified` | `#6D3FC0` | `#C8B5FF` | Chip "Clasificada" |

Los valores partieron de los prototipos de Figma (`AGATHA — Prototipos App Móvil`, nodos
`2:2`, `1:4` y `34:398`) y se ajustaron para contraste y para el modo oscuro, que el
prototipo no cubría.

**Por qué no usamos Dynamic Color.** Material 3 permite generar la paleta a partir del
wallpaper del usuario (Material You). Se decidió **no** activarla: el rojo/ámbar/verde de
AGATHA no es una preferencia estética, es información de seguridad (nivel de riesgo de una
alerta). Si el teléfono de un técnico de campo tuviera un wallpaper que tiñera esos colores,
podría volverse ambiguo distinguir una alerta crítica de una normal — inaceptable para una
app que se usa para decidir si hay que atender una fuga de gas. La paleta es **fija e
idéntica en todos los dispositivos**.

**Tipografía.** La escala (`ui/theme/Type.kt`) está pensada para lectura en campo (sol
directo, guantes, vehículo en movimiento): cuerpo a 14 sp y nunca menos de 12 sp, títulos
SemiBold/Bold, y etiquetas en mayúsculas con tracking positivo. El prototipo medía el cuerpo
en 10,5 px y las etiquetas en 8,5 px, por debajo del mínimo legible de Material 3, así que no
se copió literal. La familia es la del sistema (Roboto): legible, con tildes y eñes, y sin
descargas porque la app opera sin conexión. Si se adopta una fuente de marca (p. ej. Inter),
hay un único punto de cambio documentado en el propio archivo.

**Formas.** Radios de esquina (`ui/theme/Shape.kt`): 8dp pastillas/barra de sitio, 10-12dp
botones, 14dp tarjetas — medidos directamente del archivo de diseño.

**Mapa de nodos: mock-up vs. SDK real.** La pantalla `SensorMapScreen` dibuja los nodos con
un `Canvas`/`Box` posicionado por coordenadas normalizadas (`SensorNode.normalizedX/
normalizedY`), igual de fiel visualmente al prototipo (que tampoco usa un mapa real, sino
un fondo verde con puntos de color). `com.google.maps.android:maps-compose` ya está en el
catálogo de versiones para cuando existan coordenadas lat/lng reales de los nodos AGATHA —
no se activó todavía porque su versión actual exige `compileSdk 37`/AGP 9.1+, un salto que
este proyecto evita por ahora (§6).

**Notificaciones push: pendiente de FCM real.** La app arranca directamente en la Bandeja
de alertas (`alerts`). Hubo una pantalla que simulaba visualmente una notificación de
Android (nodo 34:2 de Figma), pero se retiró: la notificación debe implementarse de verdad
con Firebase Cloud Messaging (HE-04, RNF-MOV-06), donde la posta el sistema operativo y su
`PendingIntent` abre `alert_detail/{alertId}` directamente.

---

## 6. Versiones del proyecto

| Herramienta | Versión | Nota |
|---|---|---|
| Gradle | 8.14.5 | Última versión estable de la serie 8.x. |
| Android Gradle Plugin | 8.13.2 | Ver "Por qué no AGP 9" abajo. |
| Kotlin | 2.3.20 | Última versión con soporte completo y probado de KSP/Compose compiler. |
| KSP | 2.3.12 | Procesador de anotaciones de Room/Hilt. |
| JDK (compileOptions) | 17 | Mínimo exigido por AGP 8.13/Gradle 8.14; LTS, sin fricción con ninguna librería del proyecto. |
| compileSdk / targetSdk | 36 (Android 16) | Techo recomendado para AGP 8.13.2. |
| minSdk | **33 (Android 13)** | Pedido explícito del alcance del piloto; además es la primera versión con el Photo Picker nativo que usa HE-06 sin pedir permisos de almacenamiento. |

**Por qué no AGP 9 (y por qué esto no es un "quedarse atrás").** Se intentó primero con
AGP 9.3.0 (la versión más reciente al momento de crear el proyecto). Dos builds reales
fallaron:

1. AGP 9.x introduce un nuevo DSL de `android {}` que **no es compatible** con el plugin
   clásico `org.jetbrains.kotlin.android` (error real:
   *"The 'org.jetbrains.kotlin.android' plugin is not compatible with AGP's 9.0 new DSL"*).
2. Las versiones más nuevas de varias librerías de AndroidX (`core-ktx 1.19.0`,
   `compose-bom 2026.09.00`, `coil3 3.6.x`, `maps-compose 8.5.0`) ya están compiladas contra
   API 37 y exigen AGP ≥ 9.1 para poder consumirlas.

Migrar a built-in Kotlin (la vía que Google recomienda para AGP 9) es una migración de
ecosistema completo (Hilt, KSP, Compose compiler, Navigation Safe Args) que varios de esos
plugins todavía no han terminado de estabilizar según sus propios issues de GitHub. Dado que
el pedido explícito era **"usar versiones recientes para evitar errores"**, se optó por
AGP 8.13.2 — la última versión de la serie más madura, con Kotlin 2.3.20 y las últimas
versiones de cada librería que siguen compilando contra `compileSdk 36` sin exigir el salto
a AGP 9. Ambos builds (`compileDebugKotlin` y `assembleDebug`) se verificaron de punta a
punta en este entorno antes de cerrar esta fase.

Cuando el ecosistema (Hilt, KSP, Compose) confirme soporte estable para AGP 9's built-in
Kotlin, migrar es sencillo: quitar `org.jetbrains.kotlin.android`/`kotlin.plugin.compose`
del `libs.versions.toml`, subir `agp` y `kotlin`, y seguir la guía oficial
(`developer.android.com/build/migrate-to-built-in-kotlin`).

---

## 7. Datos de ejemplo vs. API real

Todas las pantallas ya son **interactivas** (no son solo maquetas estáticas): navegan,
actualizan estado, guardan formularios y reflejan esos cambios donde corresponda. Lo hacen
contra `data/repository/InMemory*Repository`, que mantiene el estado en un
`MutableStateFlow` sembrado con `SampleAgathaData` (los mismos 4 sensores, sitio y textos
del prototipo de Figma: `MP-1156`, `MP-1189`, `MP-1122`, `MP-1201`, sitio
"Güepsa – San José de Pare").

Los datos de ejemplo **también se traducen**: sus textos son claves `sample_*` en
`values/` y `values-en/strings.xml`, envueltas en `LocalizedText.Resource`, y lo
estructurado (tiempos, tipo de sensor, aceleración) son minutos o enums que la UI formatea.
Así la demo se ve completa en el idioma del dispositivo. Cuando llegue la API real, el texto
escrito por personas (observaciones, notas del operador) viajará como `LocalizedText.Plain`.

Esto es intencional para esta fase del proyecto (pantallas + navegación + paleta, sin
integración con la API central todavía). El contrato para el sprint de integración es
simple porque ya está trazado por interfaces:

1. Implementar `AlertRepository`, `SensorNodeRepository`, `HistoryRepository` y
   `SyncRepository` respaldadas por Room (cache local) + Retrofit (API central) +
   WorkManager (sincronización diferida).
2. Enlazarlas en `core/di/RepositoryModule.kt` en lugar de las `InMemory*`.
3. Ninguna pantalla ni ViewModel cambia — es exactamente el problema que el patrón
   Repository (§2) existe para resolver.
