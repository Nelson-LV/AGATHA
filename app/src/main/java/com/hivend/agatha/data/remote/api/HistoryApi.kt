package com.hivend.agatha.data.remote.api

import com.hivend.agatha.data.remote.dto.HistoryEventDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HistoryApi {
    @GET("api/sensor-nodes/{nodeId}/history")
    suspend fun getHistoryForNode(
        @Path("nodeId") nodeId: String,
        @Query("from") from: String,
        @Query("through") through: String,
    ): List<HistoryEventDto>
}
