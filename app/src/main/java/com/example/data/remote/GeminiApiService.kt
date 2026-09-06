package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Gemini REST Request Data Classes
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.4f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 2048,
    val responseMimeType: String? = null
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

// Gemini REST Response Data Classes
data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApi {
    // Engine 1: Official Direct Gemini 1.5 Flash Endpoint
    // https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR_GEMINI_API_KEY
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentDynamic(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    // Direct official Gemini 1.5 Flash Endpoint
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // In-memory override for user-configured key from Settings
    @Volatile
    var customApiKeyOverride: String? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    /**
     * Resolves the active Gemini API Key:
     * 1. Manual user override in Settings (if provided)
     * 2. BuildConfig.GEMINI_API_KEY
     * 3. YOUR_GEMINI_API_KEY placeholder check
     */
    fun getEffectiveApiKey(): String {
        val custom = customApiKeyOverride?.trim()
        if (!custom.isNullOrBlank()) return custom
        return BuildConfig.GEMINI_API_KEY ?: ""
    }

    fun hasValidApiKey(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "YOUR_GEMINI_API_KEY" && key.length > 10
    }

    /**
     * Fast direct query helper for low-latency conversational calling using Gemini 1.5 Flash
     */
    suspend fun queryGeminiText(
        prompt: String,
        systemInstruction: String? = null,
        model: String = "gemini-1.5-flash",
        maxTokens: Int = 180,
        temperature: Float = 0.7f
    ): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext null
        val apiKey = getEffectiveApiKey()
        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                systemInstruction = systemInstruction?.let {
                    GeminiContent(parts = listOf(GeminiPart(text = it)))
                },
                generationConfig = GeminiGenerationConfig(
                    temperature = temperature,
                    maxOutputTokens = maxTokens
                )
            )
            val response = try {
                api.generateContentDynamic(model, apiKey, request)
            } catch (e: Exception) {
                api.generateContent(apiKey, request)
            }
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
        } catch (e: Exception) {
            android.util.Log.w("GeminiClient", "Gemini query error: ${e.message}")
            return@withContext null
        }
    }
}
