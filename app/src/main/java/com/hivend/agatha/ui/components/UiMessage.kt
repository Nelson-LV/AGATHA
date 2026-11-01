package com.hivend.agatha.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.AlertFailure

/**
 * Aviso para la persona que usa la app, sin resolver aún al idioma: los ViewModels exponen un
 * recurso o un texto que ya viene escrito de la API (los errores del backend vienen en español).
 */
sealed interface UiMessage {
    data class Res(@param:StringRes val id: Int) : UiMessage
    data class Raw(val text: String) : UiMessage
}

@Composable
@ReadOnlyComposable
fun UiMessage.resolve(): String = when (this) {
    is UiMessage.Res -> stringResource(id)
    is UiMessage.Raw -> text
}

fun AlertFailure.toUiMessage(): UiMessage = when (this) {
    AlertFailure.NoConnection -> UiMessage.Res(R.string.alert_error_no_connection)
    AlertFailure.Server -> UiMessage.Res(R.string.alert_error_server)
    AlertFailure.NotFound -> UiMessage.Res(R.string.alert_detail_not_found)
    is AlertFailure.Rejected -> message?.let { UiMessage.Raw(it) } ?: UiMessage.Res(R.string.alert_error_rejected)
}
