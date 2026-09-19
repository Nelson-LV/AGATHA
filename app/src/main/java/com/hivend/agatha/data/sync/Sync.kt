/**
 * Motor de sincronización en segundo plano (HE-07, RNF-MOV-03/04).
 *
 * Planeado para el sprint de integración, no implementado todavía (ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Datos de ejemplo vs. API real"):
 *
 * - `SyncWorker`: `CoroutineWorker` de WorkManager, encolado con `ExistingWorkPolicy.KEEP`
 *   para no duplicar envíos, con política de reintento exponencial.
 * - Reemplazará a `InMemorySincronizacionRepository` (data/repository) implementando la
 *   misma interfaz `SincronizacionRepository`, para que
 *   [com.hivend.agatha.ui.sync.SyncScreen] no cambie una sola línea.
 */
package com.hivend.agatha.data.sync
