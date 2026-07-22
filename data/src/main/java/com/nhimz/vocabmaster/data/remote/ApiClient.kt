package com.nhimz.vocabmaster.data.remote

import com.nhimz.vocabmaster.data.BuildConfig
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ApiClient @Inject constructor(
    authInterceptor: AuthInterceptor
) {
    private val BASE_URL = BuildConfig.API_BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val placementApi: PlacementApiService by lazy {
        retrofit.create(PlacementApiService::class.java)
    }

    val vocabularyApi: VocabularyApiService by lazy {
        retrofit.create(VocabularyApiService::class.java)
    }

    open val syncApi: SyncApiService by lazy {
        retrofit.create(SyncApiService::class.java)
    }
}