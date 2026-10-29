package com.hivend.agatha.data.remote

import com.hivend.agatha.BuildConfig
import com.hivend.agatha.data.remote.dashboard.DashboardApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/**
 * Cliente HTTP compartido hacia el backend de AGATHA. Base: [BuildConfig.AGATHA_API_BASE_URL]
 * (solo HTTPS: Android bloquea el tráfico en claro desde la API 28 y la app no lo habilita).
 *
 * Cada API vive en su subpaquete (`data/remote/dashboard`, `data/remote/alert`…) con su
 * interfaz Retrofit, sus DTOs y sus mappers, y se registra aquí con un `@Provides` como
 * [DashboardApi]. Los repositorios llaman a la API dentro de [apiCall] para recibir un
 * [ApiResult] en lugar de excepciones.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AGATHA_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create()
}
