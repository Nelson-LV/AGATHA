# AGATHA app móvil — Backlog 1.3 y referencias de la web

Referencia funcional de la app (épicas HE-04 a HE-08) según el **Backlog 1.3** del proyecto
(10/10/2026). Manda sobre cualquier regla anterior de este repositorio que la contradiga.
`ARQUITECTURA_Y_DISENO.md` describe el código tal como está hoy; este documento describe a
dónde tiene que llegar. Lo que todavía no está implementado está en §6.

## 1. Principio: la web es la fuente de verdad (D-19)

La app **no define estados ni listas propias**. Niveles, estados de gestión, clasificación,
etiquetas, límites, colores y formatos son exactamente los de la plataforma web (RN-01 a
RN-13). Lo único exclusivo del móvil es:

- notificaciones push (HE-04),
- reporte de inspección y mantenimiento (HE-05, RN-16),
- fotos con compresión (HE-06),
- offline-first y conflictos (HE-07, RN-17, RN-18).

No hay inicio de sesión: todo registro guarda **fecha-hora y origen** ("App móvil" + el
identificador del celular configurado en la app; la web registra "Web").

## 2. Modelo de dominio que pide el backlog

| Concepto | Valores | Regla |
|---|---|---|
| Nivel de alerta | `YELLOW` < `ORANGE` < `RED` (Verde = normal, sin alerta) | RN-01. Umbrales por dispositivo, los define el backend. |
| Aviso (no es alerta) | Sin comunicación · Indicador no calculable | RN-02. Gris; sin push, sin historial de alertas. |
| Color del punto | Rojo > Naranja > Amarillo > Gris > Verde | RN-03. Alerta + aviso = color de la alerta + ícono de aviso. |
| Clasificación | Confirmada · Falsa alerta | RN-06. No editable una vez guardada. |
| Estado de gestión | Sin clasificar → Clasificada sin etiqueta → Clasificada con etiqueta | RN-06. Solo avanza. |
| Etiquetas Confirmada | Fuga confirmada en campo · Movimiento de tierra · Intervención de maquinaria ajena · Otro ¿Cuál? | RN-07 |
| Etiquetas Falsa alerta | Condiciones climáticas extremas · Tránsito de vehículo pesado · Mantenimiento o intervención programada · Otro ¿Cuál? | RN-07 ("Falla del sensor" se retiró) |
| Evento | Fuga · Movimiento · Fuga y Movimiento | RN-12 |
| Línea de tiempo | Lista de (nivel, fecha-hora) por alerta | RN-05. La lista y el mapa muestran el nivel actual; el historial, el nivel máximo. |
| Fin de alerta | Fecha del primer cálculo Normal, o de la clasificación como Falsa alerta | RN-04. "En curso" mientras esté activa. |
| Resultado de inspección | Sin novedad · Mantenimiento requerido · Problema del dispositivo/sensor · Daño físico · Batería baja · Sin comunicación · Otro ¿Cuál? | RN-16. Uno, obligatorio. |
| Mantenimiento realizado | Cambio o recarga de batería · Limpieza · Ajuste o reinstalación · Reemplazo de componente · Otro ¿Cuál? | RN-16. Opcional, varios. |
| Estado de sincronización | Pendiente · Sincronizando · Sincronizado · Error | RN-17 |
| Origen | Web · App móvil (+ id del celular) | RN-10 |

Límites: observación máx. 500 caracteres (contador "n/500"); "Otro ¿Cuál?" 1–100
(obligatorio, "Guardar" deshabilitado si está vacío); fotos JPG/JPEG/PNG, máx. 5 por
registro, 5 MB c/u (la app comprime a ≤ 5 MB), descripción máx. 200; fechas
`dd/mm/aaaa HH:mm` 24 h, hora de Colombia (UTC-5).

## 3. Historias de usuario móviles (resumen de CA)

**HE-04 — Notificación y consulta de alertas (AGT-8)**
- **HU-4.1 Push (AGT-25):** push al generarse una alerta o al subir de nivel ("La alerta
  ALR-xxx subió a <nivel>"), con nivel, dispositivo, PK, evento e inicio; al tocarla abre el
  detalle (deep link, también en arranque en frío); nunca por avisos ni por alertas
  suprimidas por Falsa alerta; si no hay permiso de notificaciones, aviso con acceso a
  ajustes.
- **HU-4.2 Lista y detalle (AGT-27):** lista con Sin clasificar primero y luego activas
  clasificadas, más reciente primero (color de nivel actual, dispositivo, PK, evento,
  inicio). Detalle: ID, dispositivo, PK, municipio, evento, nivel actual, línea de tiempo,
  valor del indicador, clasificación, estado de gestión, etiqueta, observaciones, fotos y
  reportes de inspección. "Ver gráfica" (indicador, últimas 24 h, solo con conexión).
  Sin conexión: "Sin conexión · datos al dd/mm/aaaa HH:mm". "Nuevo reporte de inspección"
  abre HU-5.1 con dispositivo y alerta asociados.

**HE-05 — Reporte de inspección y clasificación (AGT-9)**
- **HU-5.1 Reporte (AGT-29):** pertenece a un dispositivo (obligatorio) y opcionalmente a
  una alerta; resultado RN-16 obligatorio; guarda con fecha-hora y origen; no se edita,
  solo se complementa; sin conexión queda "Pendiente de sincronizar"; si se crea desde el
  mapa y el dispositivo tiene una alerta Sin clasificar, pregunta si asociarla.
- **HU-5.2 Clasificación desde la inspección (AGT-30):** solo si la alerta asociada está
  Sin clasificar según la última sincronización: Confirmada / Falsa alerta → "Clasificada
  sin etiqueta"; etiqueta opcional con la lista de su clasificación → "Clasificada con
  etiqueta". Si ya está clasificada, solo lectura (y puede agregar etiqueta si falta).
  Conflicto al sincronizar → RN-18 y aviso "La alerta ya había sido clasificada como <X>
  desde <origen>". Falsa alerta sincronizada → punto Verde en web y app.
- **HU-5.3 Observaciones (AGT-31):** texto libre máx. 500, con fecha-hora, también offline.
- **HU-5.4 Mantenimiento (AGT-154, nueva):** interruptor "Se realizó mantenimiento", tipos
  RN-16 (varios), descripción opcional; aparece en el historial del dispositivo.

**HE-06 — Evidencia fotográfica, opcional (AGT-10)**
- **HU-6.1 (AGT-32):** cámara o galería, hasta 5 por reporte, compresión automática,
  "Máximo 5 fotografías por reporte", permiso de cámara denegado → explicar y ofrecer galería.
- **HU-6.2 (AGT-33):** descripción opcional máx. 200, visible también en la web.

**HE-07 — Offline-first y sincronización (AGT-12)**
- **HU-7.1 (AGT-34):** registrar todo sin conexión; datos locales: alertas activas y Sin
  clasificar, dispositivos, trazado, historial de 30 días; indicador permanente "Sin
  conexión · última sincronización … " con número de pendientes; refresco cada 15 min o
  al deslizar.
- **HU-7.2 (AGT-35):** sincronización automática en orden de registro (reporte antes que
  sus fotos), ID único generado en el celular, estados por registro, 3 reintentos y luego
  Error con motivo.
- **HU-7.3 (AGT-36):** "Reintentar" y "Reintentar todo"; pendientes sobreviven al cierre;
  sin conexión: "Sin conexión. Se sincronizará automáticamente al recuperarla."
- **HU-7.4 (AGT-37):** RN-18 y aviso del conflicto con el registro afectado.

**HE-08 — Mapa e historial del dispositivo (AGT-13)**
- **HU-8.1 (AGT-38):** trazado y nodos con los mismos colores, avisos y reglas de la web;
  offline con el último estado y su fecha (el mapa base puede no verse).
- **HU-8.2 (AGT-39):** ficha del nodo = ID, PK, municipio, estado (color y motivo del
  aviso), última comunicación y batería informativa; "Nuevo reporte de inspección" /
  "Ver historial".
- **HU-8.3 (AGT-40):** últimos 30 días, más reciente primero: alertas (nivel máximo, línea de
  tiempo, clasificación, etiqueta), reportes, mantenimientos, observaciones y fotos, con su
  origen; los no sincronizados marcados "Pendiente de sincronizar".

Fuera del alcance móvil (solo web): telemetría completa (HU-3.2), historial global y
exportación (HE-09), panel "Últimas alertas" (HU-2.1).

## 4. Referencias visuales de la web (`docs/referencias-web/`)

| Archivo | Qué tomar para la app |
|---|---|
| `01_pantalla_principal_mapa_alertas.jpeg` | Leyenda de estados del punto (Normal, Amarilla, Naranja, Roja titila sin clasificar, aviso gris, alerta + aviso), material de tubería (Acero oscuro / Flexible azul), ficha de punto y de tramo, chip "NUEVA", tarjeta de alerta (chip de nivel, evento, fecha, "Disp. · PK · Municipio"). |
| `02_detalle_clasificacion_alerta.jpeg` | Encabezado "chip nivel + Alerta ALR-xxxx + chip estado", grilla de datos (Fin "En curso" en rojo, indicador con umbral), observaciones con contador, banner informativo, par de botones "Falsa alerta" (secundario) / "Confirmar alerta" (rojo). |
| `03_etiquetado_alerta_confirmada.jpeg` | Banner de éxito tras clasificar, causas como opciones de radio en grilla, observación previa en solo lectura con fecha y origen, "Nueva observación", fotos con contador "n/5" y error por archivo, "Ahora no" / "Guardar etiqueta". |
| `04_etiquetado_falsa_alerta.jpeg` | Validación de "Otro ¿Cuál?" (campo rojo, mensaje, contador 0/100) y "Guardar etiqueta" deshabilitado. |
| `05_historial_alertas.jpeg` | Chips de clasificación (Confirmada / Falsa alerta) y de estado de gestión (Sin clasificar gris, Clasificada sin etiqueta azul, Clasificada con etiqueta verde); sirve para el historial del dispositivo (HU-8.3). |

Las capturas son de antes de cerrar la v1.3. Donde difieren, rige el backlog: la lista de
falsa alerta no lleva "Falla del sensor", la batería no lleva color y se registra el
origen, no el usuario.

Adaptación a móvil: los diálogos de la web pasan a pantallas completas o bottom sheets, la
grilla de causas a una columna, y los colores salen de `AgathaTheme.colors` (agregar roles
nuevos en `ui/theme/Color.kt` para amarillo, gris de aviso y los chips de estado y
clasificación, con su variante oscura).

## 5. Choques con lo acordado el 07/10/2026 (pendiente de confirmar)

| Implementado / acordado | Backlog 1.3 |
|---|---|
| La bandeja muestra solo naranja y rojo (`AlertLevel.requiresAttention`) | Amarillo también es alerta |
| "Cerrar alerta" sin inspección, con confirmación | No existe cerrar: termina sola al volver a Normal o al clasificarla como Falsa alerta |
| Botón verde "Iniciar / Continuar inspección", estados `IN_INSPECTION` / `CLASSIFIED` | "Nuevo reporte de inspección"; estados de gestión de la web |
| Resultado "Indicios de fuga" (inspección solo visual) | Etiqueta "Fuga confirmada en campo" (lista web) |

Hasta que Nelson confirme, **no implementar ninguno de los dos lados de un choque**;
preguntar antes.

## 6. Brecha con el código actual (para los próximos sprints)

- `AlertStatus` (`GENERATED → RECEIVED → IN_INSPECTION → CLASSIFIED → CLOSED`) debe
  reemplazarse por `AlertClassification` (+ nulo) y `ManagementStatus`.
- `AlertLevel` necesita `YELLOW`; los avisos (`NO_COMMUNICATION`, `INDICATOR_UNAVAILABLE`)
  van en un tipo aparte, no como nivel.
- `Alert` necesita: id "ALR-xxxx", PK, municipio, evento, inicio, fin, línea de tiempo,
  valor del indicador, etiqueta, origen y fecha de clasificación.
- `Inspection` pasa a ser un **reporte** de dispositivo (alerta opcional) con
  `InspectionResult` de RN-16, mantenimiento y observaciones acumulables; las categorías
  propias (`EventCategory`) se sustituyen por las etiquetas RN-07.
- `SensorNode` necesita PK, municipio, estado de comunicación, motivo del aviso y batería
  informativa.
- Push real con FCM (HU-4.1) y deep link a `alert_detail/{alertId}`.
- Cada valor visible nuevo va a `strings.xml` (es) y `values-en/strings.xml` (en), y cada
  enum nuevo a `ui/components/Labels.kt`.
