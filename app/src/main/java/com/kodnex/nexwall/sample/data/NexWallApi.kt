package com.kodnex.nexwall.sample.data

import com.kodnex.nexwall.sample.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

const val DEFAULT_BASE_URL = "https://nexwall.kodnextech.com/api/developer/v1"

/** Retrofit definition of the NexWall Developer API v1 endpoints used here. */
interface NexWallService {
    @GET("categories")
    suspend fun categories(): Response<CategoriesResponse>

    /** [perPage] 1-100, [sort] newest | oldest | popular | random. */
    @GET("wallpapers")
    suspend fun wallpapers(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
        @Query("category_id") categoryId: Int? = null,
        @Query("sort") sort: String = "newest",
        @Query("type") type: String = "image",
        @Query("search") search: String? = null,
    ): Response<WallpapersResponse>

    @GET("categories/{categoryId}/wallpapers")
    suspend fun categoryWallpapers(
        @Path("categoryId") categoryId: Int,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
    ): Response<WallpapersResponse>
}

/** Any non-2xx API response. */
class NexWallException(
    val code: Int,
    override val message: String,
    /** Seconds until the quota resets (HTTP 429 only). */
    val retryAfterSeconds: Long? = null,
) : Exception(message) {
    val userMessage: String
        get() = when (code) {
            401 -> "Invalid or missing API key. Add NEXWALL_API_KEY to local.properties. " +
                "Free key: https://nexwall.kodnextech.com/developers/register"
            404 -> "Not found, or not available on your plan."
            422 -> "Invalid request: $message"
            429 -> "API quota exceeded." +
                (retryAfterSeconds?.let { " Try again in ${it}s." } ?: "")
            else -> message
        }
}

/**
 * Thin wrapper that adds auth headers, turns errors into [NexWallException]
 * and exposes the remaining daily quota.
 */
class NexWallRepository(
    apiKey: String = BuildConfig.NEXWALL_API_KEY,
    baseUrl: String = BuildConfig.NEXWALL_BASE_URL,
) {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
                .build()
            chain.proceed(request)
        }
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val service: NexWallService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NexWallService::class.java)

    private val _remainingToday = MutableStateFlow<Int?>(null)

    /** `remaining_requests_today` from the most recent response. */
    val remainingToday: StateFlow<Int?> = _remainingToday.asStateFlow()

    /** With a custom base URL (your proxy) the key lives on the server instead. */
    private val keyRequired = apiKey.isBlank() && baseUrl.startsWith(DEFAULT_BASE_URL)

    suspend fun categories(): List<Category> {
        val body = call { service.categories() }
        body.remainingRequestsToday?.let { _remainingToday.value = it }
        return body.data
    }

    suspend fun wallpapers(page: Int, categoryId: Int?, sort: String): WallpapersResponse {
        val body = call { service.wallpapers(page = page, categoryId = categoryId, sort = sort) }
        body.remainingRequestsToday?.let { _remainingToday.value = it }
        return body
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): T {
        if (keyRequired) {
            throw NexWallException(401, "NEXWALL_API_KEY is empty")
        }
        val response = block()
        val body = response.body()
        if (response.isSuccessful && body != null) return body

        if (response.code() == 429) _remainingToday.value = 0
        val raw = response.errorBody()?.string().orEmpty()
        val message = runCatching {
            json.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull() ?: "HTTP ${response.code()}"
        throw NexWallException(
            code = response.code(),
            message = message,
            retryAfterSeconds = response.headers()["Retry-After"]?.toLongOrNull(),
        )
    }
}
