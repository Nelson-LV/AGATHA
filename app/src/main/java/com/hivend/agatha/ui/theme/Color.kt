package com.hivend.agatha.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta de AGATHA por roles, con una variante clara y una oscura. Las pantallas nunca usan
 * un hex directo: leen `AgathaTheme.colors.<rol>`, de modo que el modo oscuro del sistema
 * cambia toda la app a la vez. Ver docs/ARQUITECTURA_Y_DISENO.md § 5 "Sistema visual".
 *
 * Identidad: un azul "acero" profundo como color de marca (instrumentación industrial,
 * sobrio frente a los colores de alerta) sobre neutros fríos. El rojo/ámbar/verde quedan
 * reservados a la severidad y al estado, nunca a la decoración, para que una alerta roja
 * sea lo primero que ve el técnico. En oscuro los colores semánticos se aclaran y sus
 * fondos se oscurecen para mantener contraste AA sin deslumbrar en campo de noche.
 */
@Immutable
data class AgathaColors(
    // ---- Marca ----
    val brand: Color,
    val onBrand: Color,
    val brandContainer: Color,
    val onBrandContainer: Color,

    // ---- Neutros ----
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnMuted: Color,

    // ---- Severidad (Nivel de alerta) ----
    val critical: Color,
    val onCritical: Color,
    val criticalBorder: Color,
    val criticalSurface: Color,
    val warning: Color,
    val positive: Color,
    val onPositive: Color,

    // ---- Estado de alerta (chips) ----
    val stateReceived: Color,
    val stateReceivedContainer: Color,
    val stateInspection: Color,
    val stateInspectionContainer: Color,
    val stateClassified: Color,
    val stateClassifiedContainer: Color,
    val stateClosed: Color,
    val stateClosedContainer: Color,
    val stateSimulated: Color,
    val stateSimulatedContainer: Color,

    // ---- Conectividad / sincronización (HE-07) ----
    val connected: Color,
    val connectedContainer: Color,
    val offline: Color,
    val offlineContainer: Color,
    val offlineBannerBorder: Color,
    val offlineBannerContainer: Color,
    val conflict: Color,
    val conflictContainer: Color,
    val conflictBorder: Color,

    // ---- Mapa de nodos (HE-08) ----
    val nodeNormal: Color,
    val nodeAlert: Color,
    val nodeCritical: Color,
    val nodeOutline: Color,
    val mapCanvas: Color,
    val mapLabelContainer: Color,
)

val LightAgathaColors = AgathaColors(
    brand = Color(0xFF1D4ED8),
    onBrand = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFE0E9FF),
    onBrandContainer = Color(0xFF0B2A6B),

    background = Color(0xFFF3F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF7F8FA),
    border = Color(0xFFDDE2E9),
    divider = Color(0xFFE8EBF0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF5A6474),
    textTertiary = Color(0xFF7D8696),
    textOnMuted = Color(0xFF3F4957),

    critical = Color(0xFFC62828),
    onCritical = Color(0xFFFFFFFF),
    criticalBorder = Color(0xFFF19A9A),
    criticalSurface = Color(0xFFFEF3F2),
    warning = Color(0xFFB45309),
    positive = Color(0xFF15803D),
    onPositive = Color(0xFFFFFFFF),

    stateReceived = Color(0xFF1D4ED8),
    stateReceivedContainer = Color(0xFFE0E9FF),
    stateInspection = Color(0xFF9A4A06),
    stateInspectionContainer = Color(0xFFFDECD8),
    stateClassified = Color(0xFF6D3FC0),
    stateClassifiedContainer = Color(0xFFEDE6FF),
    stateClosed = Color(0xFF15803D),
    stateClosedContainer = Color(0xFFDDF6E5),
    stateSimulated = Color(0xFF5A6474),
    stateSimulatedContainer = Color(0xFFECEEF2),

    connected = Color(0xFF15803D),
    connectedContainer = Color(0xFFE3F5E9),
    offline = Color(0xFF9A4A06),
    offlineContainer = Color(0xFFFBEEDD),
    offlineBannerBorder = Color(0xFFE2B57E),
    offlineBannerContainer = Color(0xFFFBEEDD),
    conflict = Color(0xFF7A5A00),
    conflictContainer = Color(0xFFFFF7DB),
    conflictBorder = Color(0xFFE5C766),

    nodeNormal = Color(0xFF16A34A),
    nodeAlert = Color(0xFFE38A06),
    nodeCritical = Color(0xFFDC2626),
    nodeOutline = Color(0xFFFFFFFF),
    mapCanvas = Color(0xFFDCE7DD),
    mapLabelContainer = Color(0xD9FFFFFF),
)

val DarkAgathaColors = AgathaColors(
    brand = Color(0xFF8AB0FF),
    onBrand = Color(0xFF0A1F52),
    brandContainer = Color(0xFF1C2D57),
    onBrandContainer = Color(0xFFD9E4FF),

    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF1D232C),
    border = Color(0xFF2D3540),
    divider = Color(0xFF252C35),
    textPrimary = Color(0xFFE7EBF0),
    textSecondary = Color(0xFFA8B1BD),
    textTertiary = Color(0xFF7F8997),
    textOnMuted = Color(0xFFC3CAD3),

    critical = Color(0xFFFF8A80),
    onCritical = Color(0xFF3B0A0A),
    criticalBorder = Color(0xFF7A2A2A),
    criticalSurface = Color(0xFF2A1416),
    warning = Color(0xFFF5B054),
    positive = Color(0xFF5BD68A),
    onPositive = Color(0xFF0A2A16),

    stateReceived = Color(0xFF9DBBFF),
    stateReceivedContainer = Color(0xFF1C2D57),
    stateInspection = Color(0xFFF5B054),
    stateInspectionContainer = Color(0xFF3A2610),
    stateClassified = Color(0xFFC8B5FF),
    stateClassifiedContainer = Color(0xFF2C2147),
    stateClosed = Color(0xFF5BD68A),
    stateClosedContainer = Color(0xFF12301E),
    stateSimulated = Color(0xFFA8B1BD),
    stateSimulatedContainer = Color(0xFF242A33),

    connected = Color(0xFF5BD68A),
    connectedContainer = Color(0xFF112619),
    offline = Color(0xFFF5B054),
    offlineContainer = Color(0xFF2E1F0E),
    offlineBannerBorder = Color(0xFF6B4A1E),
    offlineBannerContainer = Color(0xFF2E1F0E),
    conflict = Color(0xFFF2CF5B),
    conflictContainer = Color(0xFF2D2610),
    conflictBorder = Color(0xFF6B5A1E),

    nodeNormal = Color(0xFF34C46A),
    nodeAlert = Color(0xFFF5A524),
    nodeCritical = Color(0xFFFF5A52),
    nodeOutline = Color(0xFF0D1117),
    mapCanvas = Color(0xFF17231B),
    mapLabelContainer = Color(0xD9161B22),
)

val LocalAgathaColors = staticCompositionLocalOf { LightAgathaColors }
