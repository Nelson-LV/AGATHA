# Guía para confirmar los push de prueba del backend

Sirve para responderle al backend si los push llegan con la app abierta, en segundo plano y
cerrada. Formato de los mensajes: `CONTRATO_PUSH_FCM.md`.

## Antes de empezar

1. Abre en el emulador o celular de prueba la app **instalada con el token que le diste al
   backend**. Desinstalarla o borrar sus datos cambia el token y hay que pasarlo de nuevo; una
   reinstalación con `installDebug` lo conserva.
2. Acepta el permiso de notificaciones. En un emulador también se puede dar por comando:

   ```bash
   adb shell pm grant com.hivend.agatha.debug android.permission.POST_NOTIFICATIONS
   ```

3. Para volver a ver el token y el `deviceId` (solo en compilación debug), abre la app y corre:

   ```bash
   adb logcat -d -s AgathaPush
   ```

   `adb` está en `%LOCALAPPDATA%\Android\Sdk\platform-tools`.

## Cómo poner cada estado

| Estado | Cómo |
|---|---|
| Abierta | La app en pantalla |
| Segundo plano | Botón Inicio; la app queda en recientes |
| Cerrada | Deslizarla fuera de recientes |

**No uses "Forzar detención" ni `am force-stop`:** Android bloquea los push de una app detenida a la
fuerza hasta que se vuelva a abrir, y parecería un fallo que no lo es.

## Qué pedir y qué anotar

Pide los 6 push (3 niveles × 2 tipos) en **cada** estado. Todos traen el mismo `alertId`, así que cada
uno **reemplaza** al anterior en la barra: es lo esperado. Revisa el título de cada uno antes de que
llegue el siguiente, o pide que los manden con pausas.

Con el celular en español, el título debe ser:

| `level` | `kind=NEW` | `kind=LEVEL_UP` |
|---|---|---|
| `YELLOW` | Nueva alerta ALR-PRUEBA-000001: Amarillo | La alerta ALR-PRUEBA-000001 subió a Amarillo |
| `ORANGE` | Nueva alerta ALR-PRUEBA-000001: Naranja | La alerta ALR-PRUEBA-000001 subió a Naranja |
| `RED` | Nueva alerta ALR-PRUEBA-000001: Rojo | La alerta ALR-PRUEBA-000001 subió a Rojo |

En inglés dicen "New alert … : Yellow" y "Alert … rose to Yellow".

| Estado | Amarillo NEW | Amarillo LEVEL_UP | Naranja NEW | Naranja LEVEL_UP | Rojo NEW | Rojo LEVEL_UP |
|---|---|---|---|---|---|---|
| Abierta | | | | | | |
| Segundo plano | | | | | | |
| Cerrada | | | | | | |

## Otras dos comprobaciones

- **Alerta inexistente:** toca una notificación en cada estado. Debe abrir el detalle con "Alerta no
  encontrada", sin botón Reintentar y sin que la app se cierre. La flecha de volver lleva a Alertas.
- **Duplicados:** pide dos envíos con el **mismo** `messageId`. Debe aparecer una sola notificación;
  si borras la primera antes del segundo envío, no debe aparecer otra.

## Si algo falla

- **No llega nada:** revisa que el token sea el actual (paso 3), que el permiso esté dado y que el
  dispositivo tenga internet. Pídele al backend la respuesta de FCM: `UNREGISTERED` significa token
  viejo.
- **Llega abierta pero no cerrada:** confirma que no fue una detención forzada y que el mensaje lleve
  `android.priority: "HIGH"` sin bloque `notification`. Algunos fabricantes también limitan las apps
  en segundo plano, así que anota el modelo del celular.
- **Mensaje descartado:** nivel `GREEN` o gris, `classification=FALSE_ALARM`, o falta `alertId` o
  `level`. La app no los muestra, a propósito.
