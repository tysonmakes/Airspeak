package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Gemini REST Request Data Classes
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64 encoded audio/image
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPrebuiltVoiceConfig(
    val voiceName: String // "Puck", "Aoede", "Fenrir", "Kore", "Charon"
)

data class GeminiVoiceConfig(
    val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfig
)

data class GeminiSpeechConfig(
    val voiceConfig: GeminiVoiceConfig
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 2048,
    val responseMimeType: String? = null,
    val responseModalities: List<String>? = null, // e.g. ["AUDIO", "TEXT"]
    val speechConfig: GeminiSpeechConfig? = null
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

data class GeminiVoiceTurnResult(
    val audioBytes: ByteArray?,
    val audioMimeType: String?,
    val spokenText: String,
    val liveCorrection: String? = null,
    val livePraise: String? = null,
    val latencyMs: Long = 0L,
    val isNativeAudio: Boolean = false
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentDynamic(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // In-memory override for user-configured key from Settings
    @Volatile
    var customApiKeyOverride: String? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
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
     * Fast direct query helper for low-latency conversational calling
     */
    suspend fun queryGeminiText(
        prompt: String,
        systemInstruction: String? = null,
        model: String = "gemini-2.5-flash",
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
            val response = api.generateContentDynamic(model, apiKey, request)
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
        } catch (e: Exception) {
            Log.w("GeminiClient", "Gemini query error: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Direct Gemini Native Voice generation endpoint
     * Sends conversation context and receives native audio bytes (PCM/MP3) + transcript in one sub-second turn.
     */
    suspend fun queryGeminiNativeVoiceTurn(
        userText: String?,
        userAudioBase64: String? = null,
        userAudioMime: String = "audio/wav",
        systemInstruction: String,
        voiceName: String = "Aoede", // Puck, Aoede, Fenrir, Kore, Charon
        conversationHistory: List<GeminiContent> = emptyList()
    ): GeminiVoiceTurnResult? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext null
        val apiKey = getEffectiveApiKey()
        val startTime = System.currentTimeMillis()

        try {
            val userParts = mutableListOf<GeminiPart>()
            if (!userAudioBase64.isNullOrBlank()) {
                userParts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = userAudioMime, data = userAudioBase64)))
            }
            if (!userText.isNullOrBlank()) {
                userParts.add(GeminiPart(text = userText))
            }
            if (userParts.isEmpty()) return@withContext null

            val contents = conversationHistory.toMutableList()
            contents.add(GeminiContent(role = "user", parts = userParts))

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    responseModalities = listOf("AUDIO", "TEXT"),
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = voiceName)
                        )
                    )
                )
            )

            // Primary model: gemini-2.5-flash-native-audio-preview-12-2025 or gemini-2.5-flash
            val modelsToTry = listOf("gemini-2.5-flash-native-audio-preview-12-2025", "gemini-2.5-flash")
            var response: GeminiResponse? = null
            for (m in modelsToTry) {
                try {
                    response = api.generateContentDynamic(m, apiKey, request)
                    if (response.candidates?.firstOrNull()?.content?.parts?.isNotEmpty() == true) break
                } catch (e: Exception) {
                    Log.d("GeminiClient", "Native voice model $m attempt failed: ${e.message}")
                }
            }

            val parts = response?.candidates?.firstOrNull()?.content?.parts ?: return@withContext null
            val latency = System.currentTimeMillis() - startTime

            var audioBytes: ByteArray? = null
            var mimeType: String? = null
            var spokenText = ""

            for (part in parts) {
                if (part.inlineData != null && part.inlineData.data.isNotBlank()) {
                    mimeType = part.inlineData.mimeType
                    audioBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                }
                if (!part.text.isNullOrBlank()) {
                    spokenText += if (spokenText.isBlank()) part.text else "\n${part.text}"
                }
            }

            return@withContext GeminiVoiceTurnResult(
                audioBytes = audioBytes,
                audioMimeType = mimeType,
                spokenText = spokenText.trim(),
                latencyMs = latency,
                isNativeAudio = audioBytes != null
            )
        } catch (e: Exception) {
            Log.e("GeminiClient", "Gemini Native Voice turn failed", e)
            return@withContext null
        }
    }
}

