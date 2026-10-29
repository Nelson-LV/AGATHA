# Contrato de sincronización de la app — propuesta para el equipo web/backend

**Borrador para discutir (29/10/2026, Mauricio).** Nada de esto está implementado en el
backend todavía; rutas, verbos y nombres se pueden cambiar. Avísennos cómo queda y lo
ajustamos en la app. Jira: AGT-175 (este acuerdo), AGT-124 (endpoints) y AGT-130 (conflictos).

La app registra todo sin conexión (HU-7.1) y lo envía después, sola, al recuperar la señal
(HU-7.2). Este documento dice qué envía, en qué orden y qué respuestas necesita para no
duplicar registros (RNF-MOV-03) ni perder evidencia cuando la web y la app tocan la misma
alerta (RN-18). El registro del token de push ya está en
[`CONTRATO_PUSH_FCM.md`](CONTRATO_PUSH_FCM.md) §1 y sigue estas mismas reglas.

## 1. Cómo envía la app

- **Cola en el celular.** Cada registro se guarda primero en el celular y se encola. Un
  proceso en segundo plano (WorkManager) lo envía cuando hay red, aunque la app esté cerrada.
- **Orden.** Se envía en el orden en que se registró y respetando dependencias:
  **clasificación → reporte de inspección → fotos de ese reporte**. Una foto no sale antes
  que su reporte.
- **Reintentos.** Si no hay conexión o el servidor responde 408, 429 o 5xx, la app reintenta
  sola hasta 3 veces (con espera creciente) y después deja el registro en *Error* para que el
  técnico lo reintente a mano (HU-7.3). Cualquier otro 4xx se toma como rechazo definitivo y
  no se reintenta solo.
- **Consecuencia para el backend:** el mismo registro **puede llegar más de una vez** (la
  respuesta se perdió, el celular se apagó a mitad del envío…). Ver §2.

## 2. Reglas comunes a todos los envíos

| Regla | Detalle |
|---|---|
| **`id` generado en el celular** | Cada registro trae un `id` UUID v4 creado en el celular. Es la llave de idempotencia. |
| **Idempotencia** | Si llega un `id` que ya existe, el servidor **no crea otro**: responde lo mismo que la primera vez (2xx con el registro guardado, o el mismo 409). |
| **Origen (RN-10)** | No hay login. Cada registro trae `origin: { "type": "MOBILE_APP", "deviceId": "CEL-1A2B3C" }`. La web registra `{ "type": "WEB" }`. El `deviceId` es el mismo del token push (P-17 sigue abierta). |
| **Fecha-hora** | La del momento en que se registró en campo, no la del envío. RFC 3339 con zona: `2026-10-29T14:32:00-05:00`. Se muestra en UTC-5 (`dd/mm/aaaa HH:mm`). |
| **JSON** | `application/json`, UTF-8, enums en MAYÚSCULAS como en la tabla del §6. Campos desconocidos se ignoran en ambos sentidos. |
| **Errores** | El mismo formato de la API de tablero: `{ "code": "…", "message": "…" }`, más campos extra cuando el código lo dice (ver §4). |
| **HTTPS** | Solo HTTPS (la app bloquea el tráfico en claro). |

## 3. Endpoints

### 3.1 Clasificar una alerta (HU-5.2, primer envío)

Se envía apenas el técnico toca "Falsa alerta" o "Confirmar alerta" y confirma.

```
POST /api/alerts/{alertId}/classification
{
  "id": "6f1c…-uuid",
  "classification": "CONFIRMED",          // o FALSE_ALARM
  "classifiedAt": "2026-10-29T14:32:00-05:00",
  "origin": { "type": "MOBILE_APP", "deviceId": "CEL-1A2B3C" }
}
→ 201 { "alertId": "ALR-0142", "classification": "CONFIRMED", "classifiedAt": "…",
        "origin": {…}, "managementStatus": "CLASSIFIED_WITHOUT_TAG" }
```

- Solo aplica si la alerta está **Sin clasificar**. La clasificación no se edita (RN-06).
- Si ya estaba clasificada → **409 `ALREADY_CLASSIFIED`** (§4).
- Falsa alerta sincronizada → la alerta termina y el punto vuelve a Verde en web y app (RN-04).

### 3.2 Reporte de inspección (HU-5.1, HU-5.2, HU-5.3, HU-5.4)

Pertenece a un dispositivo y, opcionalmente, a una alerta. No se edita: un reporte nuevo lo
complementa. Desde el 14/10/2026 **no lleva "Resultado de la inspección"** ni clasificación;
puede traer la etiqueta que le falte a una alerta ya clasificada.

```
POST /api/inspection-reports
{
  "id": "a93e…-uuid",
  "deviceId": "MP-11556",
  "alertId": "ALR-0142",                 // opcional
  "tag": "OTHER_CONFIRMED",              // opcional, solo con alertId (§6)
  "tagOtherDetail": "Excavación de un vecino",   // obligatorio solo si tag es OTHER_*, 1–100
  "observation": "Olor a gas cerca de la válvula", // opcional, máx. 500
  "maintenance": {                       // opcional (HU-5.4)
    "types": ["BATTERY_CHANGE_OR_RECHARGE", "OTHER"],
    "otherDetail": "Cambio de antena",   // obligatorio solo si incluye OTHER, 1–100
    "description": "Batería al 10 %"     // opcional, máx. 500
  },
  "photoCount": 2,                       // fotos que vienen después (0–5)
  "recordedAt": "2026-10-29T14:40:00-05:00",
  "origin": { "type": "MOBILE_APP", "deviceId": "CEL-1A2B3C" }
}
→ 201 { "id": "a93e…-uuid", "managementStatus": "CLASSIFIED_WITH_TAG" }
```

- Un reporte con `tag` exige que la alerta **ya esté clasificada** y que la etiqueta sea de la
  lista de esa clasificación (RN-07); si no → 400 `INVALID_INPUT`.
- Si la alerta ya tenía etiqueta → **409 `ALREADY_TAGGED`** (§4). **El reporte se guarda
  igual**, sin la etiqueta.
- El reporte, el mantenimiento y la observación deben verse en el historial del dispositivo
  (HU-8.3) con su origen.

### 3.3 Foto de un reporte (HU-6.1, HU-6.2)

Una petición por foto, después de que el reporte se sincronizó.

```
POST /api/inspection-reports/{reportId}/photos      (multipart/form-data)
  metadata = { "id": "c2d4…-uuid", "description": "Válvula norte",   // descripción opcional, máx. 200
               "recordedAt": "2026-10-29T14:41:00-05:00", "origin": {…} }
  file     = <imagen>                                // image/jpeg o image/png, ≤ 5 MB
→ 201 { "id": "c2d4…-uuid", "url": "https://…" }
```

- La app ya comprime a ≤ 5 MB y no deja pasar más de 5 por reporte.
- Errores: 404 `REPORT_NOT_FOUND`, 400 `TOO_MANY_PHOTOS` (más de 5), 413 `FILE_TOO_LARGE`,
  415 `UNSUPPORTED_MEDIA_TYPE`.

### 3.4 Observación suelta (HU-5.3)

La que se escribe en el detalle de la alerta fuera de un reporte. Se acumulan; ninguna se edita.

```
POST /api/observations
{
  "id": "e71b…-uuid",
  "deviceId": "MP-11556",
  "alertId": "ALR-0142",                 // opcional
  "text": "Sigue el olor a las 3 pm",    // 1–500
  "recordedAt": "2026-10-29T15:00:00-05:00",
  "origin": { "type": "MOBILE_APP", "deviceId": "CEL-1A2B3C" }
}
→ 201 { "id": "e71b…-uuid" }
```

## 4. Conflictos entre la web y la app (RN-18, HU-7.4)

Caso típico: el técnico clasifica sin señal; mientras tanto un operador clasifica la misma
alerta desde la web. Cuando el celular sincroniza, la alerta ya está clasificada.

**Regla RN-18:** prevalece lo primero que llegó al servidor; **lo que llega después no se
descarta**: el servidor lo guarda como **observación de conflicto** en la alerta (texto, fecha
y origen del envío perdedor), para que no se pierda evidencia (AGT-132).

Respuesta propuesta para la clasificación (igual para `ALREADY_TAGGED`, con `tag` en `current`):

```
409
{
  "code": "ALREADY_CLASSIFIED",
  "message": "La alerta ya estaba clasificada",
  "current": {
    "classification": "FALSE_ALARM",
    "classifiedAt": "2026-10-29T14:20:00-05:00",
    "origin": { "type": "WEB" }
  },
  "conflictObservationId": "9a0f…"      // la observación donde quedó lo enviado por la app
}
```

- El 409 **también es idempotente**: reenviar el mismo `id` devuelve el mismo 409 y no crea
  otra observación de conflicto.
- Con `current` la app muestra "La alerta ya había sido clasificada como *Falsa alerta* desde
  *Web*" junto al registro afectado y da el registro por sincronizado.

## 5. Resumen de respuestas y qué hace la app

| Respuesta | Significa | La app |
|---|---|---|
| 200 / 201 | Guardado (o ya existía con ese `id`) | Marca *Sincronizado* |
| 409 `ALREADY_CLASSIFIED` / `ALREADY_TAGGED` | Conflicto RN-18, lo enviado quedó como observación | Marca sincronizado y muestra el aviso de conflicto |
| 400 / 404 / 413 / 415 | Rechazo: el dato no es válido | *Error* con el motivo, sin reintento automático |
| 408 / 429 / 5xx / sin red | Problema temporal | Reintenta sola hasta 3 veces, luego *Error* |

## 6. Valores de los enums

Son los nombres que ya usa la app (`domain/model`); si la web usa otros, acordamos un mapeo.

| Campo | Valores |
|---|---|
| `classification` | `CONFIRMED` · `FALSE_ALARM` |
| `managementStatus` | `UNCLASSIFIED` · `CLASSIFIED_WITHOUT_TAG` · `CLASSIFIED_WITH_TAG` |
| `tag` (Confirmada) | `LEAK_CONFIRMED_IN_FIELD` · `GROUND_MOVEMENT` · `THIRD_PARTY_MACHINERY` · `OTHER_CONFIRMED` |
| `tag` (Falsa alerta) | `EXTREME_WEATHER` · `HEAVY_VEHICLE_TRAFFIC` · `SCHEDULED_MAINTENANCE` · `OTHER_FALSE_ALARM` |
| `maintenance.types` | `BATTERY_CHANGE_OR_RECHARGE` · `CLEANING` · `ADJUSTMENT_OR_REINSTALLATION` · `COMPONENT_REPLACEMENT` · `OTHER` |
| `origin.type` | `MOBILE_APP` · `WEB` |

## 7. Pendientes para la reunión

1. **¿Endpoints compartidos con la web?** Lo ideal es que la web clasifique y etiquete por
   los mismos endpoints, para que RN-06 y RN-18 vivan en un solo lugar del backend.
2. **Autenticación de la app.** No hay login (P-06), pero la API no debería aceptar escrituras
   de cualquiera. ¿Una llave por instalación, un token por celular registrado…?
3. **Identificador del celular (P-17).** Hoy la app genera `CEL-XXXXXX`; si el cliente define
   otro, solo cambia el valor de `origin.deviceId`.
4. **Lectura para trabajar sin conexión (HU-7.1).** Además de este contrato de escritura, la
   app necesita leer para guardar en el celular: alertas activas y Sin clasificar, dispositivos
   con su estado, trazado e historial de 30 días por dispositivo. Proponemos acordarlo aparte.
5. **Cómo se identifica el dispositivo.** En la app el nodo es el código `MP-11556`; la API
   de tablero lo identifica con un id numérico (`11556`) y el código va dentro del nombre
   ("Sensor MP-11556 PK34-470"). Los ejemplos usan el código; hay que decidir cuál viaja en
   `deviceId` y que sea el mismo en todas las APIs.
6. **Plazos.** La app ya tiene la cola, los reintentos y el modelo de errores listos; necesita
   al menos el endpoint de clasificación (§3.1) y el del token push para la primera prueba de
   punta a punta (AGT-125).
