package com.hivend.agatha.data.repository

import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.data.remote.ApiErrorBody
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.data.remote.alerts.AlertsApi
import com.hivend.agatha.data.remote.alerts.fileNameFromContentDisposition
import com.hivend.agatha.data.remote.alerts.toDomain
import com.hivend.agatha.data.remote.alerts.toQuery
import com.hivend.agatha.data.remote.apiCall
import com.hivend.agatha.domain.model.AlertExportFile
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.AlertSearchPage
import com.hivend.agatha.domain.repository.AlertSearchRepository
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Historial de alertas con filtros contra la API de gestión de alertas, sin caché: cada cambio de
 * filtro o de página se pide al servidor, que es quien cuenta el total y arma el Excel.
 */
@Singleton
class RemoteAlertSearchRepository @Inject constructor(
    private val api: AlertsApi,
    private val json: Json,
) : AlertSearchRepository {

    override suspend fun search(filters: AlertSearchFilters, page: Int): AlertResult<AlertSearchPage> {
        val query = filters.toQuery()
        val result = apiCall(json) {
            api.alerts(
                device = query.device,
                page = page,
                from = query.from,
                to = query.to,
                maxLevel = query.maxLevel,
                event = query.event,
                status = query.status,
            )
        }
        return when (result) {
            is ApiResult.Success -> AlertResult.Success(
                AlertSearchPage(
                    total = result.value.total,
                    page = result.value.page,
                    pageSize = result.value.pageSize,
                    alerts = result.value.items.mapNotNull(AlertDto::toDomain),
                ),
            )
            is ApiResult.Failure -> AlertResult.Failure(result.error.toAlertFailure())
        }
    }

    override suspend fun export(filters: AlertSearchFilters): AlertResult<AlertExportFile> {
        val query = filters.toQuery()
        return try {
            val response = api.export(
                device = query.device,
                from = query.from,
                to = query.to,
                maxLevel = query.maxLevel,
                event = query.event,
                status = query.status,
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return AlertResult.Failure(AlertFailure.Server)
                val bytes = withContext(Dispatchers.IO) { body.use { it.bytes() } }
                val name = fileNameFromContentDisposition(response.headers()[CONTENT_DISPOSITION])
                    ?: defaultFileName()
                AlertResult.Success(AlertExportFile(name, bytes))
            } else {
                val error = withContext(Dispatchers.IO) { response.errorBody()?.use { it.string() } }
                    ?.let { text -> runCatching { json.decodeFromString<ApiErrorBody>(text) }.getOrNull() }
                AlertResult.Failure(ApiError.Http(response.code(), error).toAlertFailure())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            AlertResult.Failure(AlertFailure.NoConnection)
        } catch (_: SerializationException) {
            AlertResult.Failure(AlertFailure.Server)
        }
    }

    /** Como lo nombra el servidor, por si el header no llega: `alertas_aaaaMMdd_HHmm.xlsx`. */
    private fun defaultFileName(): String =
        "alertas_" + LocalDateTime.now(COLOMBIA).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx"

    private companion object {
        const val CONTENT_DISPOSITION = "Content-Disposition"
        val COLOMBIA: ZoneId = ZoneId.of("America/Bogota")
    }
}

/** 404 = no existe; 5xx o respuesta ilegible = falla del servidor; otro 4xx trae el mensaje para mostrar. */
internal fun ApiError.toAlertFailure(): AlertFailure = when (this) {
    ApiError.NoConnection -> AlertFailure.NoConnection
    ApiError.InvalidResponse -> AlertFailure.Server
    is ApiError.Http -> when {
        code == 404 -> AlertFailure.NotFound
        code >= 500 -> AlertFailure.Server
        else -> AlertFailure.Rejected(body?.code, body?.message)
    }
}
