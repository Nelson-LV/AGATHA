package com.hivend.agatha.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AGATHA color palette, extracted directly from the approved Figma prototypes
 * (node ids 2:2, 1:4 and 34:398 of the "AGATHA Móvil" file) and mapped onto Material 3
 * color roles. See docs/ARQUITECTURA_Y_DISENO.md § "Paleta de colores (Material 3)" for
 * the full role-by-role rationale and contrast notes.
 *
 * Every semantic color below (Rojo/Ambar/Verde/Azul/Morado) mirrors a domain meaning that
 * repeats across screens — alert severity, sync/pending state, and inspection outcomes —
 * so it is named by role, never reused ad hoc with a raw hex value in a screen.
 */

// ---- Brand ----
val AgathaBlue = Color(0xFF2563EB) // Logo mark, links, "Recibida" chip, primary actions
val AgathaBlueContainer = Color(0xFFE5F2FF)

// ---- Neutrals (backgrounds, text, borders) ----
val NeutralBackground = Color(0xFFF1F1F1)
val NeutralSurface = Color(0xFFFFFFFF)
val NeutralSurfaceVariant = Color(0xFFFAFAFA)
val NeutralBorder = Color(0xFFE5E8EB)
val NeutralBorderStrong = Color(0xFFD9DBE0)
val NeutralDivider = Color(0xFFEBEBED)

val TextPrimary = Color(0xFF0F1729)
val TextSecondary = Color(0xFF6B7380)
val TextTertiary = Color(0xFF999EA8)
val TextOnMuted = Color(0xFF4A5463)

// ---- Semantic: alert severity (Nivel de alerta) ----
val AlertRed = Color(0xFFDB2626) // Crítico / Alerta roja
val AlertRedBorder = Color(0xFFF87171)
val AlertRedSurface = Color(0xFFFFFAFA)
val AlertAmber = Color(0xFFD9591A) // Medio / Alerta amarilla (texto de énfasis)
val AlertGreen = Color(0xFF17A34A) // Normal / acciones positivas (botón "Actualizar estado")

// ---- Semantic: estado de alerta (chips en Bandeja / Historial) ----
val StateReceivedText = Color(0xFF2863FB)
val StateReceivedContainer = Color(0xFFE5F2FF)
val StateInspectionText = Color(0xFFB2660D)
val StateInspectionContainer = Color(0xFFFFEDDB)
val StateClassifiedText = Color(0xFF804DD9)
val StateClassifiedContainer = Color(0xFFEDE5FF)
val StateClosedText = Color(0xFF178A45)
val StateClosedContainer = Color(0xFFDBFAE5)
val StateSimulatedText = Color(0xFF6B7380)
val StateSimulatedContainer = Color(0xFFEDEDF0)

// ---- Semantic: conectividad / sincronización (HE-07) ----
val ConnectedText = Color(0xFF178040)
val ConnectedContainer = Color(0xFFE5F7EB)
val OfflineText = Color(0xFFA6590D)
val OfflineContainer = Color(0xFFFAEDDE)
val OfflineBannerBorder = Color(0xFFD9B280)
val OfflineBannerContainer = Color(0xFFF2E5DB)
val ConflictText = Color(0xFF8C6B0D)
val ConflictContainer = Color(0xFFFFFAE5)
val ConflictBorder = Color(0xFFE5C766)

// ---- Sensor map legend (HE-08) ----
val NodeNormal = Color(0xFF17A34A)
val NodeAlert = Color(0xFFF0990D)
val NodeCritical = Color(0xFFDB2626)
val MapCanvasBackground = Color(0xFFD9E8D9)
