package com.hivend.agatha.ui.history

import androidx.annotation.StringRes
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode

data class HistorySite(
    val id: String,
    @param:StringRes val nameRes: Int,
)

data class HistorySensorOption(
    val id: String,
    val label: String,
)

/**
 * Temporary local device-to-site catalog. Replace this single map with the backend catalog
 * when the API exposes the site associated with each measuring point.
 */
internal object HistorySiteCatalog {
    val GUEPSA = HistorySite("guepsa-san-jose-de-pare", R.string.history_site_guepsa)

    // TODO(backend): Replace with the authoritative site/device catalog; these IDs come from the current map sample.
    private val deviceIdsBySite: Map<HistorySite, Set<String>> = mapOf(
        GUEPSA to setOf("MP-11556", "MP-1189", "MP-1122"),
    )

    val sites: List<HistorySite> get() = deviceIdsBySite.keys.toList()

    fun site(siteId: String?): HistorySite? = deviceIdsBySite.keys.firstOrNull { it.id == siteId }

    fun siteForSensor(sensorId: String?): HistorySite? = sensorId?.let { id ->
        deviceIdsBySite.entries.firstOrNull { id in it.value }?.key
    }

    fun sensorIds(site: HistorySite?): Set<String> = site?.let(deviceIdsBySite::get).orEmpty()

    fun sensorOptions(site: HistorySite?, nodes: List<SensorNode>): List<HistorySensorOption> =
        sensorIds(site).sorted().map { id ->
            HistorySensorOption(
                id = id,
                label = nodes.firstOrNull { it.id == id }?.name?.takeIf(String::isNotBlank) ?: id,
            )
        }

    /** Unknown devices reached from Detail remain visible with their real ID and no guessed site. */
    fun sensorOptionsForUnknownSite(sensorId: String?, nodes: List<SensorNode>): List<HistorySensorOption> =
        sensorId?.let { id ->
            listOf(HistorySensorOption(id, nodes.firstOrNull { it.id == id }?.name ?: id))
        }.orEmpty()
}

internal fun List<HistoryEvent>.forSensor(sensorId: String?): List<HistoryEvent> =
    if (sensorId == null) emptyList() else filter { event -> event.nodeId == sensorId }
