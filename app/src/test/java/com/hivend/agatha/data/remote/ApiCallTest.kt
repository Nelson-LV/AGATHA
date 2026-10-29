package com.hivend.agatha.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET

/** El cliente de [NetworkModule] contra un servidor local, para cada forma en que falla una llamada. */
class ApiCallTest {

    @Serializable
    data class ThingDto(val id: Int, val name: String)

    interface ThingApi {
        @GET("api/thing")
        suspend fun thing(): ThingDto
    }

    private val json = NetworkModule.provideJson()
    private val server = MockWebServer()

    @Before
    fun startServer() = server.start()

    @After
    fun stopServer() = server.close()

    private fun api(baseUrl: HttpUrl = server.url("/")): ThingApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(NetworkModule.provideOkHttpClient())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()

    private fun respond(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private fun call() = runBlocking { apiCall(json) { api().thing() } }

    @Test
    fun `a 200 is decoded into the DTO, ignoring unknown fields`() {
        respond(200, """{"id": 7, "name": "MP-11556", "extra": true}""")
        assertEquals(ApiResult.Success(ThingDto(7, "MP-11556")), call())
    }

    @Test
    fun `a 500 keeps the backend error body and is retryable`() {
        respond(500, """{"code": "INTERNAL", "message": "boom"}""")
        val error = (call() as ApiResult.Failure).error
        assertEquals(ApiError.Http(500, ApiErrorBody("INTERNAL", "boom")), error)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `a 400 is a rejection that is not retried`() {
        respond(400, """{"code": "INVALID_INPUT", "message": "startDate"}""")
        val error = (call() as ApiResult.Failure).error
        assertEquals(400, (error as ApiError.Http).code)
        assertFalse(error.isRetryable)
        assertFalse(error.isConflict)
    }

    @Test
    fun `a 409 is reported as a conflict`() {
        respond(409, """{"code": "CONFLICT", "message": "already classified"}""")
        val error = (call() as ApiResult.Failure).error
        assertTrue(error.isConflict)
        assertFalse(error.isRetryable)
    }

    @Test
    fun `an error body that is not the backend format is dropped, the code is kept`() {
        respond(502, "<html>Bad Gateway</html>")
        assertEquals(ApiResult.Failure(ApiError.Http(502, null)), call())
    }

    @Test
    fun `a response that does not match the DTO is an invalid response`() {
        respond(200, """{"id": "not a number"}""")
        assertEquals(ApiResult.Failure(ApiError.InvalidResponse), call())
    }

    @Test
    fun `an unreachable server is no connection, and retryable`() {
        val unreachable = server.url("/")
        server.close()
        val result = runBlocking { apiCall(json) { api(unreachable).thing() } }
        assertEquals(ApiResult.Failure(ApiError.NoConnection), result)
        assertTrue(ApiError.NoConnection.isRetryable)
    }
}
