package com.hivend.agatha.data.remote.api

import com.hivend.agatha.data.remote.dto.SensorNodeDto
import retrofit2.http.GET

interface SensorNodeApi {
    @GET("api/sensor-nodes")
    suspend fun getSensorNodes(): List<SensorNodeDto>
}
