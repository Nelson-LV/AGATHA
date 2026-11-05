package com.hivend.agatha.data.remote.devices

import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.NetworkModule
import com.hivend.agatha.data.remote.apiCall
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** El registro del token con el `Json` real de la app, que no codifica valores por defecto. */
class DevicesApiTest {

    private val json = NetworkModule.provideJson()
    private val server = MockWebServer()

    @Before
    fun startServer() = server.start()

    @After
    fun stopServer() = server.close()

    @Test
    fun `the registration sends deviceId, token and platform and accepts a 204`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        val api: DevicesApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(NetworkModule.provideOkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()

        val result = apiCall(json) {
            api.registerPushToken(PushTokenRequestDto("CEL-1A2B3C", "fcm-token", PUSH_PLATFORM_ANDROID))
        }

        assertEquals(ApiResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/devices/push-token", request.url.encodedPath)
        val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("CEL-1A2B3C", body.getValue("deviceId").jsonPrimitive.content)
        assertEquals("fcm-token", body.getValue("token").jsonPrimitive.content)
        assertEquals("ANDROID", body.getValue("platform").jsonPrimitive.content)
    }
}
