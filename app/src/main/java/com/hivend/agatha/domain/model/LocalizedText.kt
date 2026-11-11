package com.hivend.agatha.domain.model

import androidx.annotation.StringRes

/**
 * Texto libre que el dominio transporta sin conocer el idioma del dispositivo.
 *
 * - [Plain]: texto ya escrito por una persona (observaciones del técnico, notas que envíe
 *   la API central). Se muestra tal cual, sin traducir.
 * - [Resource]: texto que vive en res/values(-en)/strings.xml. Lo usan los datos de ejemplo
 *   de data/repository para que la demo se vea en español o inglés según el dispositivo.
 *
 * La UI lo resuelve con `LocalizedText.resolve()` (ui/components/Texts.kt), así el idioma se
 * decide al dibujar y no al crear el dato. Ver CLAUDE.md § "User-facing strings".
 */
sealed interface LocalizedText {
    data class Plain(val value: String) : LocalizedText
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : LocalizedText
}
