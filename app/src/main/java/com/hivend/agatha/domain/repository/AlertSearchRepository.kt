package com.hivend.agatha.domain.repository

import com.hivend.agatha.domain.model.AlertExportFile
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.AlertSearchPage

/**
 * Historial de alertas de la plataforma con los filtros de la web (HE-09): la lista paginada y su
 * exportación a Excel usan los mismos [AlertSearchFilters], así el archivo coincide con lo que se ve.
 */
interface AlertSearchRepository {

    /** La página [page] (desde 1, 10 por página), la alerta más reciente primero. */
    suspend fun search(filters: AlertSearchFilters, page: Int): AlertResult<AlertSearchPage>

    /** Todas las alertas que cumplen [filters], en el Excel del servidor (máximo 10.000). */
    suspend fun export(filters: AlertSearchFilters): AlertResult<AlertExportFile>
}
