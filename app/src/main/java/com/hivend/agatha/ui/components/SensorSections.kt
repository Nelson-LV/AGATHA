package com.hivend.agatha.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hivend.agatha.R
import com.hivend.agatha.ui.theme.AgathaColors
import com.hivend.agatha.ui.theme.AgathaTheme

private data class SensorInfo(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val detailRes: Int,
    val color: AgathaColors.() -> Color,
)

private val sensors = listOf(
    SensorInfo(Icons.Filled.Sensors, R.string.sensor_motion_title, R.string.sensor_motion_detail) { critical },
    SensorInfo(Icons.Filled.GraphicEq, R.string.sensor_acoustic_title, R.string.sensor_acoustic_detail) { warning },
    SensorInfo(Icons.Filled.Thermostat, R.string.sensor_environment_title, R.string.sensor_environment_detail) { brand },
    SensorInfo(Icons.Filled.BatteryChargingFull, R.string.sensor_power_title, R.string.sensor_power_detail) { positive },
)

/**
 * Secciones de sensores de un dispositivo (movimiento, acústica, ambiente, energía). Antes
 * vivían en el detalle de la alerta; desde el 14/10/2026 están en la pestaña Dispositivos. Cada
 * una deja el espacio de la gráfica que llegará con los datos reales del nodo.
 */
@Composable
fun SensorSections(modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        sensors.forEach { sensor -> SensorRow(sensor) }
    }
}

@Composable
private fun SensorRow(sensor: SensorInfo) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgathaTheme.colors.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(sensor.icon, contentDescription = null, tint = sensor.color(AgathaTheme.colors), modifier = Modifier.size(20.dp))
            Text(
                stringResource(sensor.titleRes),
                color = AgathaTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = AgathaTheme.colors.textSecondary,
            )
        }
        if (expanded) {
            Text(
                stringResource(sensor.detailRes),
                color = AgathaTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(96.dp)
                    .border(1.dp, AgathaTheme.colors.border, RoundedCornerShape(10.dp)),
            ) {
                Text(
                    stringResource(R.string.sensor_chart_placeholder),
                    color = AgathaTheme.colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
