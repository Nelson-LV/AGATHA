/**
 * Persistencia local offline-first (HE-07, RNF-MOV-02/03/04).
 *
 * Planeado para el sprint de integración con la API central — no implementado todavía
 * porque esta fase del proyecto solo cubre pantallas, navegación y la paleta Material 3
 * (ver docs/ARQUITECTURA_Y_DISENO.md § "Datos de ejemplo vs. API real"):
 *
 * - `AgathaDatabase`: `RoomDatabase` con las entidades de abajo.
 * - `AlertaEntity`, `InspeccionEntity`, `EvidenciaEntity`, `RegistroPendienteEntity`.
 * - DAOs correspondientes, expuestos como `Flow<...>` para que los repositorios reales
 *   de data/repository los combinen con Retrofit sin que domain/ui cambien.
 *
 * Las dependencias de Room (androidx.room:room-runtime/ktx/compiler) ya están declaradas en
 * app/build.gradle.kts y el compilador KSP configurado, listos para recibir estas clases.
 */
package com.hivend.agatha.data.local
