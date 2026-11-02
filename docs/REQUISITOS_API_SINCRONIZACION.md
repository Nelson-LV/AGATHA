# La app móvil frente a la API de Alert Management

**29/10/2026, Mauricio (HE-07).** Revisión de la guía `ALERT_MANAGEMENT_API.MD` que compartió
el equipo backend, desde lo que la app necesita para trabajar sin conexión y sincronizar
después (HU-7.1 a HU-7.4). Jira: AGT-175.

## Para qué es este documento

No propone rutas ni formatos: esos los define el backend. Sirve para tres cosas:

1. **Confirmar** qué necesidades de la app ya cubre la API (§1), para construir sobre eso.
2. **Preguntar** por lo que la app necesita y la guía no cubre (§2), antes de que el módulo se
   cierre y cambiarlo cueste más.
3. **Dejar escrito** cómo va a usar la app cada endpoint (§3), para que ambos equipos prueben
   lo mismo.

Cuando las preguntas del §2 tengan respuesta, este documento se actualiza y se convierte en la
referencia de la integración.

## 1. Lo que la API ya cubre

| La app necesita | La API lo resuelve con | Estado |
|---|---|---|
| No duplicar un registro cuando la app lo reenvía (RNF-MOV-03) | `client_record_id` (UUID); el mismo otra vez ⇒ `200 ALREADY_RECEIVED` | ✅ La cola ya genera ese UUID por registro |
| Clasificar como un envío propio, separado del reporte (cambio del 14/10) | `POST /api/alerts/{id}/classification` | ✅ |
| Agregar la etiqueta después, una sola vez | `POST /api/alerts/{id}/label`, `409 CONFLICT` si aún no está clasificada | ✅ La cola envía la clasificación antes que la etiqueta |
| Registrar origen y hora de campo sin login (RN-10) | `origin: APP_MOVIL`, `device_identifier`, `recorded_at` | ✅ |
| No perder lo enviado si la web clasificó primero (RN-18) | `409` con `outcome: SAVED_AS_CONFLICT_OBSERVATION`, `existing` y la alerta | ✅ Guardado como observación de conflicto |
| Observaciones acumulables (HU-5.3) | `POST /api/alerts/{id}/observations`, solo se agregan | ✅ |
| Errores legibles por la app | `{ code, message }` | ✅ Mismo formato que la API de tablero |
| Datos para trabajar sin conexión (HU-7.1) | `GET /api/alerts`, `/{id}`, `/active`, `/pending`, `/devices/{id}/alerts` | ✅ Por confirmar el uso (§2.6) |

## 2. Lo que falta o hay que aclarar

1. **Reporte de inspección y mantenimiento (HU-5.1, HU-5.4).** La guía no tiene dónde guardar
   un reporte de campo ni el mantenimiento realizado (tipos RN-16 y descripción), que el
   historial del dispositivo debe mostrar (HU-8.3). Hoy la app podría mandar la etiqueta por
   `/label` y la observación por `/observations`, pero el mantenimiento y un reporte **sin
   alerta** (creado desde el mapa) no tienen endpoint. ¿Está previsto en otro módulo?
2. **Fotos (HU-6.1, HU-6.2).** La guía las deja para después del MVP. La app ya las captura y
   comprime; hasta que exista el endpoint, quedan guardadas en el celular y la app no las
   intenta enviar. ¿Hay fecha estimada?
3. **Dos significados del 409.** El mismo HTTP 409 significa "ya estaba clasificada, se guardó
   como conflicto" (`outcome: SAVED_AS_CONFLICT_OBSERVATION`) y "todavía no está clasificada"
   (sin `outcome`). La app los va a distinguir por `outcome`. ¿Podemos contar con que el
   segundo nunca trae `outcome`?
4. **Etiqueta después de un conflicto.** Si la app clasificó Confirmada sin conexión y la web
   ya había puesto Falsa alerta, la etiqueta que eligió el técnico es de la lista equivocada y
   la API respondería `400`. ¿Debe la app no enviarla, o el backend la guarda también como
   observación de conflicto (`CONFLICTO_ETIQUETA`)?
5. **Registro del token de push (HU-4.1).** La guía dice que las notificaciones son de otro
   módulo. La app ya obtiene el token FCM; ¿quién expone el registro del token y quién envía el
   push? Propuesta de la app en `CONTRATO_PUSH_FCM.md` §1.
6. **Datos sin conexión (HU-7.1).** Para guardar en el celular las alertas activas y Sin
   clasificar, ¿se recomienda `GET /api/alerts/active` y `/pending` en lugar del listado con
   10 filas por página? ¿El historial de 30 días sale de `GET /api/alerts/devices/{id}/alerts`,
   y con qué parámetros?
7. **Evento "Fuga y Movimiento" (RN-12).** El filtro de la guía solo trae `FUGA` y
   `MOVIMIENTO`. ¿Qué valor de `event` llega cuando la alerta es de los dos?
8. **Formato de `recorded_at`.** ¿RFC 3339 con offset, igual que las fechas de respuesta
   (`2026-10-29T14:32:00-05:00`)?
9. **Autenticación.** Hoy no hay. Cuando se agregue, ¿cómo se identificará la app, si no tiene
   login (P-06)?

## 3. Cómo va a usar la app la API

### 3.1 Qué se envía y a dónde

Cada registro creado en el celular se guarda localmente y entra a la cola de sincronización.
Se envía cuando hay red, en el orden en que se registró:

| Registro en la app | Endpoint | Depende de |
|---|---|---|
| Clasificación ("Confirmar alerta" / "Falsa alerta") | `POST /api/alerts/{id}/classification` | — |
| Etiqueta (dentro del reporte de inspección) | `POST /api/alerts/{id}/label` | La clasificación de esa alerta |
| Observación | `POST /api/alerts/{id}/observations` | — |
| Mantenimiento, reporte sin alerta, fotos | Pendientes (§2.1, §2.2) | — |

Campos que la app pone en todos los envíos:

| Campo | Valor |
|---|---|
| `client_record_id` | UUID generado en el celular al crear el registro; el mismo en cada reintento |
| `origin` | `APP_MOVIL` |
| `device_identifier` | Identificador del celular (`CEL-XXXXXX`; P-17 sigue abierta) |
| `recorded_at` | Hora en que se registró en campo, no la del envío |

La app **no envía campos que la API no conoce** (la API responde `400` ante un campo
desconocido) ni campos con `null`.

### 3.2 Qué hace la app con cada respuesta

| Respuesta | La app |
|---|---|
| `201 SAVED` / `200 ALREADY_RECEIVED` | Marca el registro como *Sincronizado* y actualiza la alerta con `alert` |
| `409 SAVED_AS_CONFLICT_OBSERVATION` | Marca *Sincronizado* y muestra "La alerta ya había sido clasificada como *X* desde *origen*" con `existing` (HU-7.4) |
| `409 CONFLICT` sin `outcome` (etiqueta sin clasificación) | No debería ocurrir: la cola envía primero la clasificación. Si ocurre, reintenta después |
| `400` / `404` | *Error* con el motivo; no reintenta solo |
| `500` o sin conexión | Reintenta sola hasta 3 veces y luego *Error*, con "Reintentar" manual (HU-7.3) |

### 3.3 Valores: la app traduce en el cliente

El dominio de la app usa nombres en inglés y la API en español. La traducción vive en la capa
de datos de la app (`data/remote/alert`), así que **el backend no tiene que cambiar nada**:

| Concepto | API | App |
|---|---|---|
| Clasificación | `CONFIRMADA` · `FALSA_ALERTA` | `CONFIRMED` · `FALSE_ALARM` |
| Estado de gestión | `SIN_CLASIFICAR` · `CLASIFICADA_SIN_ETIQUETA` · `CLASIFICADA_CON_ETIQUETA` | `UNCLASSIFIED` · `CLASSIFIED_WITHOUT_TAG` · `CLASSIFIED_WITH_TAG` |
| Etiqueta (Confirmada) | `FUGA_CONFIRMADA_CAMPO` · `MOVIMIENTO_TIERRA` · `MAQUINARIA_AJENA` · `OTRO` | `LEAK_CONFIRMED_IN_FIELD` · `GROUND_MOVEMENT` · `THIRD_PARTY_MACHINERY` · `OTHER_CONFIRMED` |
| Etiqueta (Falsa alerta) | `CLIMA_EXTREMO` · `TRANSITO_PESADO` · `MANTENIMIENTO_PROGRAMADO` · `OTRO` | `EXTREME_WEATHER` · `HEAVY_VEHICLE_TRAFFIC` · `SCHEDULED_MAINTENANCE` · `OTHER_FALSE_ALARM` |
| Nivel | `AMARILLO` · `NARANJA` · `ROJO` | `YELLOW` · `ORANGE` · `RED` |
| Evento | `FUGA` · `MOVIMIENTO` (· ¿§2.7?) | `LEAK` · `MOVEMENT` · `LEAK_AND_MOVEMENT` |

### 3.4 Identificadores

- **Alerta:** la app usa el `id` numérico o el `code` que entrega la API, nunca un id inventado
  en el celular. Las alertas solo las crea el backend.
- **Dispositivo:** el `measuring_point_id` numérico (`11556`), como en la API de tablero. En
  pantalla se muestra `device.pk` (el nombre).

## 4. Qué implica en la app

Ninguna pieza ya construida cambia de forma. Al implementar los envíos reales:

- **Emisores** de clasificación, etiqueta y observación sobre la cola existente, con sus DTOs y
  la traducción de valores del §3.3.
- **Conflictos:** distinguirlos por `outcome` y no solo por el 409 (§2.3), y dar el registro
  por sincronizado mostrando el aviso (AGT-131).
- **Etiqueta como envío propio:** la cola tendrá un tipo `LABEL` que depende de la
  clasificación, en lugar de enviar el reporte completo.
- **Fotos y mantenimiento** quedan guardados en el celular, sin emisor, hasta que haya endpoint.
