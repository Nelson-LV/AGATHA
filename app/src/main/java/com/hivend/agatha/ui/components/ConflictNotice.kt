package com.hivend.agatha.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.ActionConflict
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.ui.theme.AgathaTheme

/**
 * Aviso de un conflicto de la cola (RN-18, guía §7.5): lo que envió este celular y lo que
 * prevalece en el servidor (`existing`), con quién y cuándo. Se muestra en Sincronización y en
 * el detalle de la alerta hasta que el técnico toca "Entendido".
 */
@Composable
fun ConflictNotice(conflict: ActionConflict, onUnderstood: () -> Unit, modifier: Modifier = Modifier) {
    val isTag = conflict.type == SyncRecordType.INSPECTION_REPORT
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.conflictContainer, RoundedCornerShape(14.dp))
            .border(1.3.dp, AgathaTheme.colors.conflictBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = AgathaTheme.colors.conflict, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.conflict_title, conflict.alertId),
                color = AgathaTheme.colors.conflict,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            stringResource(if (isTag) R.string.conflict_tag_description else R.string.conflict_classification_description),
            color = AgathaTheme.colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ConflictVersion(stringResource(R.string.sync_conflict_local), localText(conflict), Modifier.weight(1f))
            ConflictVersion(
                stringResource(R.string.sync_conflict_server),
                conflict.existing?.let { existingText(it, isTag) } ?: stringResource(R.string.conflict_server_unknown),
                Modifier.weight(1f),
            )
        }
        OutlinedButton(
            onClick = onUnderstood,
            border = BorderStroke(1.2.dp, AgathaTheme.colors.conflictBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgathaTheme.colors.conflict),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sync_understood))
        }
    }
}

@Composable
private fun localText(conflict: ActionConflict): String {
    val tag = conflict.localTag
    return when {
        tag != null -> tagText(tag, conflict.localTagDetail)
        conflict.localClassification != null -> stringResource(conflict.localClassification.labelRes())
        else -> stringResource(R.string.common_empty_value)
    }
}

/** "Falsa alerta · Web · 25/11/2026 09:10" (o la etiqueta, si el conflicto es de etiqueta). */
@Composable
private fun existingText(existing: ClassificationRecord, isTag: Boolean): String {
    val tag = existing.tag
    val value = if (isTag && tag != null) tagText(tag.tag, tag.otherDetail) else stringResource(existing.classification.labelRes())
    val parts = listOfNotNull(value, existing.origin?.label(), existing.classifiedAt?.takeUnless { isTag }?.let { formatDateTime(it) })
    return parts.reduce { first, second -> stringResource(R.string.common_dot_separated, first, second) }
}

@Composable
private fun tagText(tag: AlertTag, detail: String?): String = detail ?: stringResource(tag.labelRes())

@Composable
private fun ConflictVersion(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AgathaTheme.colors.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = AgathaTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = AgathaTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
