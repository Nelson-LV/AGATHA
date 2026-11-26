# Guía de integración móvil: alertas, niveles y mensajes

Destinatario: agente o desarrollador que implementa en la app móvil (Android) las pantallas de alertas de AGATHA.
Fuente: código del backend en `backend/internal/modules/alert_management` y `backend/internal/modules/notifications` (rama `dev`, catálogo de mensajes v11). Todo lo que sigue ya existe en el backend: la app solo consume.

---

## 1. Qué hay que construir

1. **Lista de alertas**: activas, pendientes de clasificar e historial con filtros.
2. **Detalle de la alerta**: cabecera con el estado y una **línea de tiempo agrupada por nivel (color)**. Cada color (Amarillo, Naranja, Rojo) es un tramo con sus propios mensajes.
3. **Gestión desde campo**: clasificar (Confirmada / Falsa alerta), etiquetar la causa y agregar observaciones.
4. **Push**: registrar el token FCM. Al tocar el push, se abre el detalle de la alerta.

Regla de oro: **la app NO arma textos de mensajes**. El backend entrega `title` y `body` ya renderizados (en español, con los datos). La app los muestra tal cual.

---

## 2. Convenciones generales

| Tema | Regla |
|---|---|
| Base URL | `{BASE_URL}` (configurable por entorno). Todas las rutas empiezan con `/api`. |
| Autenticación | **No hay** (temporal). Las escrituras identifican al celular con `origin` + `device_identifier`. |
| Formato | JSON UTF-8. El backend rechaza campos desconocidos en el cuerpo (`400`). |
| Fechas en respuestas | RFC 3339 en hora de Colombia con offset: `2026-08-24T11:00:00-05:00`. |
| Fechas en filtros | `dd/mm/aaaa` o `aaaa-mm-dd` (día en hora de Colombia; `to` incluye el día completo). |
| Id de alerta en la ruta | Número (`142`) o código (`ALR-000142`). Ambos sirven. |
| Campos opcionales | Llegan como `null`, no se omiten (salvo donde se indica `omitempty`). |
| Errores | `{"code": "...", "message": "..."}` con el status HTTP correspondiente (ver §9). |

### Enumeraciones

| Campo | Valores |
|---|---|
| `event` | `FUGA`, `MOVIMIENTO` |
| Nivel (`current_level`, `max_level`, `level`) | `AMARILLO` < `NARANJA` < `ROJO` (una alerta nunca está en `NORMAL`) |
| `state` | `ACTIVA`, `FINALIZADA` (una finalizada puede volver a `ACTIVA` por reactivación) |
| `management_status` | `SIN_CLASIFICAR`, `CLASIFICADA_SIN_ETIQUETA`, `CLASIFICADA_CON_ETIQUETA` (solo avanza) |
| `classification` | `CONFIRMADA`, `FALSA_ALERTA` |
| `available_actions` | `CONFIRM`, `FALSE_ALERT`, `ADD_LABEL`, `VIEW` |
| `opened_by` (tramo) | `OPEN` (abrió la alerta), `UP` (subió), `DOWN` (bajó), `REACTIVATION` (se reactivó) |
| `kind` (mensaje) | `NOTIFICACION`, `ACTUALIZACION`, `RESUMEN`, `FIN`, `GESTION` |

### Etiquetas válidas por clasificación

| Clasificación | Etiquetas (`label`) |
|---|---|
| `CONFIRMADA` | `FUGA_CONFIRMADA_CAMPO`, `MOVIMIENTO_TIERRA`, `MAQUINARIA_AJENA`, `OTRO` |
| `FALSA_ALERTA` | `CLIMA_EXTREMO`, `TRANSITO_PESADO`, `MANTENIMIENTO_PROGRAMADO`, `OTRO` |

Textos para mostrar en la UI:

| `label` | Texto |
|---|---|
| `FUGA_CONFIRMADA_CAMPO` | Fuga confirmada en campo |
| `MOVIMIENTO_TIERRA` | Movimiento de tierra |
| `MAQUINARIA_AJENA` | Intervención de maquinaria ajena |
| `CLIMA_EXTREMO` | Condiciones climáticas extremas |
| `TRANSITO_PESADO` | Tránsito de vehículo pesado |
| `MANTENIMIENTO_PROGRAMADO` | Mantenimiento o intervención programada |
| `OTRO` | Otro (exige texto libre) |

---

## 3. Mapa de endpoints

| # | Método y ruta | Uso en la app |
|---|---|---|
| 1 | `GET /api/alerts/active` | Mapa / pantalla de alertas activas agrupadas por punto |
| 2 | `GET /api/alerts/pending?limit=` | Bandeja de pendientes de clasificar |
| 3 | `GET /api/alerts?...filtros&page=` | Historial con filtros y paginación |
| 4 | `GET /api/alerts/{id}` | **Detalle con niveles y mensajes** (pantalla principal de esta guía) |
| 5 | `GET /api/alerts/devices/{pointId}/alerts?from=&to=` | Historial de un sensor, también con niveles y mensajes |
| 6 | `POST /api/alerts/{id}/classification` | Clasificar |
| 7 | `POST /api/alerts/{id}/label` | Etiquetar |
| 8 | `POST /api/alerts/{id}/observations` | Agregar observación |
| 9 | `PUT /api/devices/push-token` | Registrar token FCM |

---

## 4. Objeto `AlertItem` (común a listas)

Lo devuelven los endpoints 1, 2, 3 y es la base del detalle (4).

```json
{
  "id": 142,
  "code": "ALR-000142",
  "device": { "id": 11556, "pk": "Sensor PK34-470" },
  "event": "FUGA",
  "state": "ACTIVA",
  "started_at": "2026-11-20T08:00:00-05:00",
  "ended_at": null,
  "in_progress": true,
  "duration_hours": 27,
  "current_level": "NARANJA",
  "max_level": "ROJO",
  "blinking": false,
  "classification": null,
  "management_status": "SIN_CLASIFICAR",
  "label": null,
  "label_other_text": null,
  "available_actions": ["CONFIRM", "FALSE_ALERT", "VIEW"]
}
```

| Campo | Significado |
|---|---|
| `current_level` | Nivel actual. **`null` si la alerta está `FINALIZADA`**. |
| `max_level` | Nivel más alto que alcanzó la alerta. Usarlo como color cuando está finalizada. |
| `blinking` | `true` si está `ACTIVA`, en `ROJO` y sin clasificar. La UI debe hacerla parpadear o destacarla. |
| `duration_hours` | Horas transcurridas (hasta el fin o hasta la última hora procesada). Es el mismo número que cita el mensaje de fin. |
| `device.pk` | Nombre del dispositivo (ej. `Sensor PK34-470`). Se muestra tal cual. |
| `available_actions` | Qué botones habilitar. **Basarse en esto, no en reglas propias.** |

Botones según `available_actions`:

| `management_status` | `available_actions` | UI |
|---|---|---|
| `SIN_CLASIFICAR` | `CONFIRM`, `FALSE_ALERT`, `VIEW` | Botones "Confirmar" y "Falsa alerta" |
| `CLASIFICADA_SIN_ETIQUETA` | `ADD_LABEL`, `VIEW` | Botón "Agregar etiqueta" |
| `CLASIFICADA_CON_ETIQUETA` | `VIEW` | Solo lectura |

---

## 5. Endpoints de lectura

### 5.1 `GET /api/alerts/active`

Alertas activas agrupadas por punto de medición. Sin parámetros.

```json
{
  "devices": [
    {
      "measuring_point_id": 11556,
      "pk": "Sensor PK34-470",
      "level": "ROJO",
      "blinking": true,
      "alerts": [ /* AlertItem[] */ ]
    }
  ]
}
```

`level` y `blinking` son los del punto (el peor de sus alertas). Un punto puede tener una alerta de `FUGA` y otra de `MOVIMIENTO` a la vez: son alertas separadas.

### 5.2 `GET /api/alerts/pending?limit=5`

Alertas `SIN_CLASIFICAR` más recientes. `limit`: por defecto 5, máximo 50.

```json
{ "total": 12, "items": [ /* AlertItem[] */ ] }
```

`total` es el total de pendientes (para un badge), `items` solo trae `limit`.

### 5.3 `GET /api/alerts` (historial con filtros)

| Parámetro | Ejemplo | Notas |
|---|---|---|
| `device` | `11556` o `todos` | Id del punto. Omitido o `todos` = todos. |
| `from`, `to` | `2026-11-01`, `25/11/2026` | `to` incluye todo ese día. `to` antes de `from` → `400`. |
| `max_level` | `ROJO` o `NARANJA,ROJO` | Filtra por nivel máximo. Multivalor: repetido (`?max_level=A&max_level=B`) o con comas. |
| `status` | `SIN_CLASIFICAR` | Estado de gestión. Multivalor. |
| `event` | `FUGA` | Multivalor. |
| `page` | `1` | Desde 1. Página fija de **10** elementos. |

Orden: más reciente primero (`started_at DESC`).

```json
{ "total": 37, "page": 1, "page_size": 10, "items": [ /* AlertItem[] */ ] }
```

### 5.4 `GET /api/alerts/{id}` — DETALLE (niveles + mensajes)

Esta es la pantalla central. Devuelve todos los campos de `AlertItem` más los siguientes:

| Campo | Significado |
|---|---|
| `opened_at` | Cuándo se creó el registro de la alerta |
| `reactivation_count`, `last_reactivated_at` | Reactivaciones |
| `opening_indicator_value`, `last_indicator_value`, `max_indicator_value` | Índice al abrir, último y máximo (número crudo, ej. `0.18`) |
| `last_evaluated_at` | Última hora evaluada |
| `hours_calculated`, `hours_no_data` | Horas con cálculo y horas sin datos |
| `low_confidence_at_open` | El sensor estaba "en ajuste" al abrir |
| `threshold_version_id_at_open` | Versión de umbrales usada al abrir |
| `classification_detail` | `null` o `{value, at, origin, device_identifier, recorded_at}` |
| `label_detail` | `null` o `{value, other_text, at, origin, device_identifier, recorded_at}` |
| `observations` | Lista de observaciones (siempre array, puede estar vacío) |
| **`levels`** | **Tramos de color en orden, cada uno con sus mensajes** |
| `motion_history` | Solo `MOVIMIENTO`: objeto con el historial del sensor al abrir. `null` en `FUGA`. |
| `motion_table_level` | Solo `MOVIMIENTO`: nivel calculado máximo. `null` en `FUGA`. |

#### Estructura de `levels`

```json
"levels": [
  {
    "seq": 1,
    "opened_by": "OPEN",
    "level": "AMARILLO",
    "started_at": "2026-11-20T08:00:00-05:00",
    "ended_at": "2026-11-20T14:00:00-05:00",
    "messages": [
      {
        "id": 9001,
        "at": "2026-11-20T08:00:00-05:00",
        "kind": "NOTIFICACION",
        "code": "OPEN",
        "level": "AMARILLO",
        "indicator_value": 0.18,
        "title": "Alerta nueva en Sensor PK34-470: nivel Amarillo",
        "body": "El índice de anomalía acústica está por encima de lo normal desde las 08:00 del 20/11. Dato: 0,18 (lo normal es menos de 0,16)."
      },
      {
        "id": 9007,
        "at": "2026-11-20T12:00:00-05:00",
        "kind": "ACTUALIZACION",
        "code": "COUNTDOWN",
        "level": "AMARILLO",
        "indicator_value": 0.21,
        "title": "La alerta puede subir a Naranja",
        "body": "El índice de anomalía acústica lleva 4 h por encima del límite de Amarillo (0,16) y ahora está en 0,21. Si sigue así 2 h más, la alerta sube a Naranja."
      }
    ]
  },
  {
    "seq": 2,
    "opened_by": "UP",
    "level": "NARANJA",
    "started_at": "2026-11-20T14:00:00-05:00",
    "ended_at": null,
    "messages": [
      {
        "id": 9010,
        "at": "2026-11-20T14:00:00-05:00",
        "kind": "NOTIFICACION",
        "code": "UP_NARANJA_PERSIST",
        "level": "NARANJA",
        "indicator_value": 0.22,
        "title": "La alerta ALR-000142 subió a Naranja",
        "body": "El índice de anomalía acústica lleva 6 h por encima de lo normal. Dato: 0,22 (lo normal es menos de 0,16)."
      }
    ]
  }
]
```

(Los valores del ejemplo son ilustrativos; la forma es la real.)

Reglas de `levels`:

- Orden: por `seq` ascendente (el tramo más viejo primero). Dentro de cada tramo, mensajes por `at` ascendente.
- `ended_at = null` → es el tramo vigente (solo si la alerta está `ACTIVA`).
- Un mismo color puede aparecer **varias veces** (ej. Amarillo → Naranja → Amarillo por bajada `DOWN`). No agrupar por color: cada tramo es un bloque propio.
- Los mensajes de gestión (`GESTION`: clasificada / etiquetada) aparecen dentro del tramo en que ocurrieron.
- `indicator_value` puede ser `null` (mensajes de gestión, horas sin dato, movimiento).

### 5.5 `GET /api/alerts/devices/{pointId}/alerts?from=&to=`

Historial de un sensor. Sin fechas → últimos 30 días. Trae los `levels` con mensajes de cada alerta (misma estructura que §5.4), pero con una cabecera reducida.

```json
{
  "measuring_point_id": 11556,
  "pk": "Sensor PK34-470",
  "alerts": [
    {
      "id": 142, "code": "ALR-000142", "event": "FUGA", "state": "FINALIZADA",
      "started_at": "...", "ended_at": "...", "max_level": "ROJO",
      "reactivation_count": 0, "classification": "CONFIRMADA", "label": "FUGA_CONFIRMADA_CAMPO",
      "levels": [ /* LevelItem[] */ ]
    }
  ]
}
```

---

## 6. Cómo mostrar los mensajes (UI)

### 6.1 Pantalla de detalle sugerida

```
+------------------------------------------------+
| ALR-000142 · FUGA · Sensor PK34-470            |
| [NARANJA]  máx: ROJO   Activa hace 27 h        |
| Sin clasificar      [Confirmar] [Falsa alerta] |
+------------------------------------------------+
| Filtro: (Todos) (Amarillo) (Naranja) (Rojo)    |
+------------------------------------------------+
| ■ AMARILLO  08:00 20/11 – 14:00 20/11  (abrió) |
|   08:00  Alerta nueva en ...: nivel Amarillo   |
|          El índice de anomalía acústica ...    |
|   12:00  La alerta puede subir a Naranja       |
|          ...                                   |
| ■ NARANJA   14:00 20/11 – ahora      (subió)   |
|   14:00  La alerta ALR-000142 subió a Naranja  |
|          ...                                   |
+------------------------------------------------+
| Observaciones (2)            [+ Observación]   |
+------------------------------------------------+
```

- **Cabecera de cada tramo**: color de `level`, rango `started_at – ended_at` ("ahora" si `null`) y texto según `opened_by`: `OPEN` "abrió", `UP` "subió", `DOWN` "bajó", `REACTIVATION` "se reactivó".
- **Filtro por color**: se hace en el cliente, filtrando `levels` por `level`. No hay endpoint aparte.
- **Orden**: cronológico (más viejo arriba) o invertido, a elección de diseño, pero consistente.
- **Texto**: mostrar `title` en negrita y `body` debajo, **sin modificar**. No parsear números del texto: si hace falta el número, usar `indicator_value`.

### 6.2 Estilo según `kind`

| `kind` | Qué es | Sugerencia visual |
|---|---|---|
| `NOTIFICACION` | Aviso importante (alerta nueva, subida de nivel, movimiento nuevo). Son los que generan push. | Destacado, ícono de alerta |
| `ACTUALIZACION` | Cambio o novedad horaria (cuenta regresiva, bajada, sin datos, tendencia) | Normal |
| `RESUMEN` | Resumen periódico sin cambios / resumen diario de movimiento | Atenuado / colapsable |
| `FIN` | La alerta terminó | Separador de cierre |
| `GESTION` | Clasificación o etiqueta registrada | Ícono de usuario / check |

### 6.3 Colores

| Nivel | Uso |
|---|---|
| `AMARILLO` | amarillo |
| `NARANJA` | naranja |
| `ROJO` | rojo; si `blinking = true`, destacar (parpadeo o borde animado) |

Usar la paleta de la app. Para una alerta `FINALIZADA` usar `max_level` (porque `current_level` es `null`).

### 6.4 Catálogo de códigos (referencia)

La app no necesita conocer los códigos para mostrar texto, pero sirven para íconos o agrupación. Títulos tal como los genera el backend (las `{variables}` ya llegan reemplazadas).

**FUGA**

| `code` | `kind` | Título |
|---|---|---|
| `OPEN` | NOTIFICACION | Alerta nueva en {pk}: nivel Amarillo / Naranja / Rojo |
| `NEW_AFTER_CLASSIFIED` | NOTIFICACION | Alerta nueva en {pk}: nivel {nivel} |
| `UP_NARANJA_PERSIST`, `UP_NARANJA_FAST` | NOTIFICACION | La alerta {codigo} subió a Naranja |
| `UP_ROJO` | NOTIFICACION | La alerta {codigo} subió a Rojo |
| `REACTIVATED` | NOTIFICACION o ACTUALIZACION | La alerta {codigo} se reactiva en nivel {nivel} |
| `COUNTDOWN` | ACTUALIZACION | La alerta puede subir a {nivel_siguiente} |
| `DOWNGRADE_START` | ACTUALIZACION | El ruido bajó al rango de {nivel_hora} |
| `DOWNGRADE` | ACTUALIZACION | La alerta bajó a nivel {nivel_inferior} |
| `ROJO_HELD` | ACTUALIZACION | La alerta sigue en Rojo |
| `TREND` | ACTUALIZACION | El ruido está {direccion} |
| `OSCILLATING` | ACTUALIZACION | El ruido oscila |
| `ALMOST_NORMAL` | ACTUALIZACION | El ruido está casi normal |
| `ONE_HOUR_NORMAL` | ACTUALIZACION | 1 h con ruido normal |
| `BACK_ABNORMAL` | ACTUALIZACION | El ruido anormal volvió |
| `NO_CALC_START` | ACTUALIZACION | Sin datos suficientes para evaluar la alerta |
| `NO_CALC_END` | ACTUALIZACION | Vuelve a haber datos suficientes del sensor |
| `SUMMARY` | RESUMEN | La alerta no tiene cambios |
| `END_NORMAL` | FIN | La alerta {codigo} terminó |

**MOVIMIENTO y temperatura**

| `code` | `kind` | Título |
|---|---|---|
| `MOV_OPEN` | NOTIFICACION | Cambio brusco / rápido / lento y acumulado de inclinación del sensor, o durante una pérdida de datos |
| `MOV_LEVEL_UP` | NOTIFICACION | La alerta de movimiento subió a {nivel} |
| `MOV_LEVEL_OBSERVED` | ACTUALIZACION | El cambio de inclinación sigue aumentando |
| `MOV_SLOW_ONGOING` | ACTUALIZACION | Además, cambio lento y acumulado de inclinación |
| `MOV_DISPLACED` | ACTUALIZACION | El sensor sigue en la nueva posición / se volvió a mover |
| `MOV_RETURN` | ACTUALIZACION | El sensor volvió a su posición |
| `MOV_DAILY` | RESUMEN | Resumen del día {n} de la alerta de movimiento |
| `TEMP_STABILIZED`, `TEMP_VARIES_AGAIN` | NOTIFICACION | La temperatura de la tubería se estabilizó / volvió a variar |
| `TEMP_NOT_STABILIZED`, `TEMP_STILL_STABLE` | ACTUALIZACION | La temperatura no se estabilizó / sigue estable |
| `END_NORMAL` | FIN | La alerta de movimiento terminó |

**Gestión (ambos eventos)**

| `code` | `kind` | Título |
|---|---|---|
| `CLASSIFIED` | GESTION | Alerta clasificada como {Confirmada / Falsa alerta} |
| `LABELED` | GESTION | Etiqueta de la alerta: {etiqueta} |

Notas:
- Los cuerpos de movimiento pueden traer **saltos de línea** (`\n`): renderizar respetándolos.
- Un cuerpo puede terminar con "Sensor en ajuste: resultado preliminar." (baja confianza). Ya viene en el texto.
- Códigos nuevos pueden aparecer en el futuro: la app debe mostrar cualquier `code` desconocido con estilo por defecto según `kind`, nunca fallar.

---

## 7. Endpoints de gestión (escritura)

### 7.1 Campos comunes de origen (obligatorios desde la app)

| Campo | Tipo | Regla |
|---|---|---|
| `origin` | string | Siempre `"APP_MOVIL"` desde la app |
| `device_identifier` | string | **Obligatorio** con `APP_MOVIL`. Usar el mismo `deviceId` que se registra en el push token (ej. `CEL-3FEF76`). |
| `client_record_id` | string UUID | Generarlo **una vez por acción** y guardarlo. Reenviar el mismo UUID no duplica nada (idempotencia). |
| `recorded_at` | RFC 3339 | Hora en que el operador hizo la acción en el celular (soporta modo sin conexión). Omitido = hora de recepción. |

### 7.2 `POST /api/alerts/{id}/classification`

```json
{
  "classification": "CONFIRMADA",
  "observation": "Se ve humedad en la vía",
  "origin": "APP_MOVIL",
  "device_identifier": "CEL-3FEF76",
  "client_record_id": "6f1c2a7e-3b9d-4f0a-9c1e-2d7b8a4e5f60",
  "recorded_at": "2026-11-25T09:14:00-05:00"
}
```

- `classification`: `CONFIRMADA` o `FALSA_ALERTA`.
- `observation`: opcional, 1 a 500 caracteres.
- Se clasifica **una sola vez** (inmutable).

### 7.3 `POST /api/alerts/{id}/label`

```json
{
  "label": "OTRO",
  "other_text": "Fuga en acometida vecina",
  "observation": "",
  "origin": "APP_MOVIL",
  "device_identifier": "CEL-3FEF76",
  "client_record_id": "0a8b3c1d-...",
  "recorded_at": "2026-11-25T09:20:00-05:00"
}
```

- Requiere que la alerta ya esté clasificada (si no → `409`).
- `label` debe pertenecer a la lista de su clasificación (§2).
- `other_text`: **obligatorio solo con `OTRO`** (1 a 100 caracteres). Con otra etiqueta, no enviarlo o enviarlo vacío.
- Se etiqueta una sola vez.

### 7.4 `POST /api/alerts/{id}/observations`

```json
{
  "text": "Cuadrilla en el sitio",
  "origin": "APP_MOVIL",
  "device_identifier": "CEL-3FEF76",
  "client_record_id": "9d2e...",
  "recorded_at": "2026-11-25T10:02:00-05:00"
}
```

`text`: 1 a 500 caracteres. Se pueden agregar las que se quieran.

### 7.5 Respuesta de gestión

```json
{
  "outcome": "SAVED",
  "alert": { /* AlertItem actualizado */ }
}
```

| Status | `outcome` | Qué hacer en la app |
|---|---|---|
| `201` | `SAVED` | Guardado. Actualizar la alerta con `alert`. |
| `200` | `ALREADY_RECEIVED` | Era un reintento con el mismo `client_record_id`. Tratar como éxito. |
| `409` | `SAVED_AS_CONFLICT_OBSERVATION` (con `"code": "CONFLICT"`) | Otro operador clasificó o etiquetó antes. **Prevalece el primero**; lo enviado quedó guardado como observación. Mostrar `existing` al usuario. |

Ejemplo de conflicto:

```json
{
  "code": "CONFLICT",
  "outcome": "SAVED_AS_CONFLICT_OBSERVATION",
  "alert": { /* AlertItem */ },
  "existing": {
    "classification": "FALSA_ALERTA",
    "classified_at": "2026-11-25T09:10:00-05:00",
    "origin": "WEB",
    "device_identifier": null
  }
}
```

Después de cualquier escritura exitosa, **volver a pedir `GET /api/alerts/{id}`**: el backend agrega el mensaje `GESTION` a la línea de tiempo.

### 7.6 Modo sin conexión (recomendado)

1. Guardar la acción en una cola local con su `client_record_id` y `recorded_at`.
2. Reintentar al recuperar conexión, con el **mismo** UUID.
3. `201` o `200` → sacar de la cola. `409` → sacar de la cola y avisar del conflicto. `400` → sacar de la cola y mostrar el error (no se arregla reintentando). `5xx` o red → reintentar con backoff.

---

## 8. Push (FCM) y apertura del detalle

### 8.1 Registro del token: `PUT /api/devices/push-token`

```json
{ "deviceId": "CEL-3FEF76", "token": "<token FCM>", "platform": "android" }
```

- Responde `204`. Es idempotente: llamarlo al iniciar la app y cada vez que FCM rote el token.
- Cuerpo máximo 4 KB.

### 8.2 Contenido del push

Es un mensaje **solo de datos** (sin bloque `notification`): la app arma la notificación local. Todos los valores son string; los opcionales pueden faltar.

| Clave | Ejemplo | Notas |
|---|---|---|
| `alertId` | `ALR-000142` | Código de la alerta. Usarlo directo en `GET /api/alerts/{alertId}`. |
| `level` | `YELLOW`, `ORANGE`, `RED` | En inglés (distinto de la API REST). |
| `kind` | `NEW`, `LEVEL_UP` | `NEW` = alerta nueva; `LEVEL_UP` = subió de nivel o se reactivó. |
| `messageId` | `9010` | Opcional. Descartar pushes repetidos con el mismo id. |
| `sensorId` | `Sensor PK34-470` | Opcional. |
| `pk` | `34+470` | Opcional. Abscisa sin el prefijo "PK". |
| `event` | `LEAK`, `MOVEMENT` | Opcional. |
| `startedAt` | `2026-11-20T08:00:00-05:00` | Opcional. |
| `classification` | `CONFIRMED`, `FALSE_ALARM` | Opcional (en alertas nuevas tras una clasificada). |

Mapeo push → REST:

| Push | REST |
|---|---|
| `YELLOW` / `ORANGE` / `RED` | `AMARILLO` / `NARANJA` / `ROJO` |
| `LEAK` / `MOVEMENT` | `FUGA` / `MOVIMIENTO` |
| `CONFIRMED` / `FALSE_ALARM` | `CONFIRMADA` / `FALSA_ALERTA` |

### 8.3 Flujo al tocar el push

1. Leer `alertId`.
2. `GET /api/alerts/{alertId}`.
3. Abrir el detalle y hacer scroll al mensaje con `id == messageId` (si vino) o al tramo vigente.

### 8.4 Qué genera push hoy

Solo estos códigos: `OPEN`, `NEW_AFTER_CLASSIFIED` (→ `NEW`) y `UP_NARANJA_PERSIST`, `UP_NARANJA_FAST`, `UP_ROJO`, `REACTIVATED` (→ `LEVEL_UP`). Los mensajes `NOTIFICACION` de movimiento y temperatura (`MOV_OPEN`, `MOV_LEVEL_UP`, `TEMP_*`) **no generan push hoy**: solo se ven en el detalle. No asumir lo contrario en la app.

---

## 9. Errores

Formato: `{"code": "<CODIGO>", "message": "<texto en español>"}`.

| Status | Casos |
|---|---|
| `400` | Id inválido, filtros inválidos, JSON inválido o con campos desconocidos, `origin` inválido o sin `device_identifier`, clasificación o etiqueta inválida, falta `other_text` con `OTRO`, observación fuera de 1–500, `client_record_id` no es UUID |
| `404` | La alerta no existe |
| `409` | Etiquetar sin clasificar; conflicto de gestión (ver §7.5) |
| `500` | Error consultando o guardando. Reintentar más tarde. |

Mostrar `message` al usuario en los `400` de gestión: ya está en español y es claro.

---

## 10. Checklist de implementación

- [ ] Cliente HTTP con `BASE_URL` por entorno.
- [ ] Modelos: `AlertItem`, `AlertDetail`, `LevelItem`, `MessageItem`, `ObservationItem`, `ManagementStep`, `ManagementResponse`. Campos nulos tolerados; enums desconocidos tolerados.
- [ ] Lista de activas (§5.1), pendientes con badge (§5.2) e historial con filtros y paginación de 10 (§5.3).
- [ ] Detalle con tramos por color, mensajes por tramo y filtro de color en cliente (§5.4, §6).
- [ ] `title` y `body` mostrados sin cambios; respetar `\n`.
- [ ] Botones según `available_actions` (§4).
- [ ] Clasificar, etiquetar (lista según clasificación, `OTRO` con texto) y observar, con `origin = APP_MOVIL`, `device_identifier`, `client_record_id` UUID y `recorded_at` (§7).
- [ ] Manejo de `201` / `200` / `409` y refresco del detalle tras escribir.
- [ ] Cola offline con reintento idempotente (§7.6).
- [ ] Registro de token FCM al iniciar y al rotar (§8.1).
- [ ] Notificación local desde el push de datos; tocar → detalle por `alertId`; deduplicar por `messageId` (§8).

## 11. Limitaciones conocidas del backend (no resolver en la app)

- Sin autenticación: cualquier cliente puede escribir. El `device_identifier` es solo trazabilidad.
- Push sin segmentación: llega a todos los celulares registrados (no por zona ni por rol). Solo Android.
- Movimiento y temperatura no generan push (§8.4).
- No hay stream en tiempo real para la app (el SSE `GET /api/notifications/stream` es para la web). Para refrescar, volver a pedir el detalle al abrir la pantalla o con pull-to-refresh.
