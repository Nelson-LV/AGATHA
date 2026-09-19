/**
 * Cliente de la API central de AGATHA (compartida con la plataforma web — HE-04, HE-07).
 *
 * Planeado para el sprint de integración, no implementado todavía (ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Datos de ejemplo vs. API real"):
 *
 * - `AgathaApiService`: interfaz Retrofit con los endpoints de alertas, inspecciones,
 *   evidencias y nodos de sensores.
 * - DTOs `@Serializable` (kotlinx.serialization) que los repositorios reales mapean a los
 *   modelos de domain/model — la UI nunca ve un DTO directamente.
 * - Un `OkHttpClient` con `HttpLoggingInterceptor` (solo en debug) y forzando TLS, según
 *   RNF-MOV-05.
 *
 * Las dependencias de Retrofit/OkHttp/kotlinx.serialization ya están declaradas en
 * app/build.gradle.kts.
 */
package com.hivend.agatha.data.remote
