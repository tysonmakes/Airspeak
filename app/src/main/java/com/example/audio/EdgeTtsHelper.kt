package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * High-Fidelity Open-Source Neural Human Voice Service
 * 
 * Multi-Tier Human-like Voice Engine:
 * Tier 1: Direct Microsoft Edge WebSocket Protocol (wss://speech.platform.bing.com) - 100% Free & Open
 * Tier 2: StreamElements Amazon Polly HD Studio Stream (Joanna, Brian, Kendra, Joey, Nicole)
 * Tier 3: Google Studio Neural TTS Stream
 * Tier 4: Local Android TTS Engine (Zero-network fallback)
 */
class EdgeTtsHelper(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Active voice profile settings
    var voiceName: String = "en-US-AnaNeural"
    var speechRate: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.7f, 1.5f)
            localTts?.setSpeechRate(field)
        }
    var pitch: Float = 1.0f

    // MediaPlayer for remote neural audio playback
    private var mediaPlayer: MediaPlayer? = null

    // Cache directory for downloaded neural audio files
    private val audioCacheDir: File = File(context.cacheDir, "edge_tts_cache").apply {
        if (!exists()) mkdirs()
    }

    // In-memory pre-fetched audio files for zero-latency instant playback
    private val prefetchCache = ConcurrentHashMap<String, File>()

    // Local fallback Android TTS engine
    private var localTts: TextToSpeech? = null
    private var isLocalTtsReady = false

    // Dedicated OkHttpClient with WebSocket support for low-latency synthesis
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    init {
        initLocalFallbackTts()
    }

    private fun initLocalFallbackTts() {
        try {
            localTts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    localTts?.language = Locale.US
                    localTts?.setSpeechRate(speechRate)
                    configureBestLocalVoice()
                    isLocalTtsReady = true

                    localTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "Fallback TTS init note: ${e.message}")
        }
    }

    private fun configureBestLocalVoice() {
        try {
            val tts = localTts ?: return
            val voices = tts.voices
            if (!voices.isNullOrEmpty()) {
                val englishVoices = voices.filter { it.locale.language.equals("en", ignoreCase = true) }
                val bestVoice = englishVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (voice.quality == android.speech.tts.Voice.QUALITY_VERY_HIGH || voice.quality == android.speech.tts.Voice.QUALITY_HIGH) &&
                    (name.contains("neural") || name.contains("natural") || name.contains("x-sfg") || name.contains("x-tpd"))
                } ?: englishVoices.firstOrNull { voice ->
                    voice.quality >= android.speech.tts.Voice.QUALITY_HIGH
                } ?: englishVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    !name.contains("robot") && !name.contains("low")
                } ?: englishVoices.firstOrNull()

                if (bestVoice != null) {
                    tts.voice = bestVoice
                    Log.d("EdgeTtsHelper", "Configured natural local voice: ${bestVoice.name}")
                }
            }
        } catch (e: Throwable) {
            Log.w("EdgeTtsHelper", "Local voice config note: ${e.message}")
        }
    }

    fun updateVoiceName(name: String) {
        this.voiceName = name
    }

    /**
     * Compute a stable MD5 key for caching synthesized audio files.
     */
    private fun computeCacheKey(text: String, voice: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest("$voice:$text".toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "${voice.hashCode()}_${text.hashCode()}"
        }
    }

    /**
     * Pre-fetch neural audio stream in the background so it plays instantly.
     */
    fun prefetch(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        scope.launch {
            try {
                fetchNeuralAudioFile(cleanText)
            } catch (e: Exception) {
                Log.d("EdgeTtsHelper", "Prefetch note: ${e.message}")
            }
        }
    }

    private fun generateSecMsGec(): String {
        val winEpoch = 11644473600L
        val seconds = (System.currentTimeMillis() / 1000L) + winEpoch
        val roundedSeconds = seconds - (seconds % 300L)
        val finalTicks = roundedSeconds * 10000000L
        val strToHash = "${finalTicks}6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(strToHash.toByteArray(Charsets.US_ASCII))
            bytes.joinToString("") { "%02x".format(it) }.uppercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Tier 1: Direct Microsoft Edge WebSocket Protocol
     * Connects directly to Microsoft's ReadAloud speech synthesis endpoint.
     * Returns synthesized MP3 bytes or null if unavailable.
     */
    private suspend fun fetchEdgeWebSocketAudio(text: String, voice: String): ByteArray? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { continuation ->
                val audioBuffer = ByteArrayOutputStream()
                val connectionId = UUID.randomUUID().toString().replace("-", "")
                val muid = UUID.randomUUID().toString().replace("-", "").uppercase()
                val secMsGec = generateSecMsGec()
                val trustedToken = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
                val url = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=$trustedToken&ConnectionId=$connectionId&Sec-MS-GEC=$secMsGec&Sec-MS-GEC-Version=1-143.0.3650.75"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
                    .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                    .header("Cookie", "muid=$muid;")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Pragma", "no-cache")
                    .header("Cache-Control", "no-cache")
                    .build()

                var isResumed = false

                val wsListener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        try {
                            // 1. Send speech config
                            val configMsg = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                            webSocket.send(configMsg)

                            // 2. Send SSML request
                            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("GMT")
                            }
                            val dateStr = dateFormat.format(Date())
                            val reqId = UUID.randomUUID().toString().replace("-", "")
                            val escapedText = text
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&apos;")
                            val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'><voice name='$voice'><prosody pitch='+0Hz' rate='+0%' volume='+0%'>$escapedText</prosody></voice></speak>"
                            val ssmlMsg = "X-Timestamp:$dateStr\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\nX-RequestId:$reqId\r\n\r\n$ssml"
                            webSocket.send(ssmlMsg)
                        } catch (e: Exception) {
                            Log.w("EdgeTtsHelper", "WebSocket send error: ${e.message}")
                            if (!isResumed) {
                                isResumed = true
                                continuation.resume(null)
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        try {
                            val byteArr = bytes.toByteArray()
                            if (byteArr.size > 2) {
                                val headerLen = ((byteArr[0].toInt() and 0xFF) shl 8) or (byteArr[1].toInt() and 0xFF)
                                val audioOffset = 2 + headerLen
                                if (byteArr.size > audioOffset) {
                                    val audioData = byteArr.copyOfRange(audioOffset, byteArr.size)
                                    audioBuffer.write(audioData)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("EdgeTtsHelper", "Audio chunk parse note: ${e.message}")
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, textMsg: String) {
                        if (textMsg.contains("Path:turn.end", ignoreCase = true)) {
                            val result = audioBuffer.toByteArray()
                            webSocket.close(1000, "Done")
                            if (!isResumed) {
                                isResumed = true
                                continuation.resume(if (result.isNotEmpty()) result else null)
                            }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.d("EdgeTtsHelper", "WebSocket edge note: ${t.message}")
                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(null)
                        }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (!isResumed) {
                            isResumed = true
                            val result = audioBuffer.toByteArray()
                            continuation.resume(if (result.isNotEmpty()) result else null)
                        }
                    }
                }

                val ws = httpClient.newWebSocket(request, wsListener)

                continuation.invokeOnCancellation {
                    ws.cancel()
                }
            }
        }
    }

    /**
     * Tier 2: Google Gemini Dedicated Studio Neural Voice
     * Synthesizes 24kHz ultra-realistic human voice (WAV).
     */
    private suspend fun fetchGeminiStudioAudio(text: String, voice: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (!com.example.data.remote.GeminiClient.hasValidApiKey()) return@withContext null
            val geminiVoice = when {
                voice.contains("Guy", ignoreCase = true) || voice.contains("David", ignoreCase = true) -> "Puck"
                voice.contains("Ryan", ignoreCase = true) || voice.contains("Arthur", ignoreCase = true) || voice.contains("GB", ignoreCase = true) -> "Charon"
                voice.contains("Marcus", ignoreCase = true) || voice.contains("Fenrir", ignoreCase = true) -> "Fenrir"
                voice.contains("Sophia", ignoreCase = true) || voice.contains("Kore", ignoreCase = true) -> "Kore"
                else -> "Aoede"
            }
            return@withContext com.example.data.remote.GeminiClient.synthesizeHumanSpeech(text, geminiVoice)
        } catch (e: Exception) {
            Log.d("EdgeTtsHelper", "Gemini Studio TTS note: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Tier 3: Google Studio Neural Pronunciation Stream
     */
    private suspend fun fetchGoogleTtsAudio(text: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(text.take(180), "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=en-US&client=tw-ob&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val bytes = resp.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    return@withContext bytes
                }
            }
        } catch (e: Exception) {
            Log.d("EdgeTtsHelper", "Google TTS note: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Fetch neural audio via Multi-Tier Human-like Voice Engine
     */
    private suspend fun fetchNeuralAudioFile(text: String): File? = withContext(Dispatchers.IO) {
        val key = computeCacheKey(text, voiceName)
        val cachedMp3 = File(audioCacheDir, "$key.mp3")
        if (cachedMp3.exists() && cachedMp3.length() > 500) {
            prefetchCache[key] = cachedMp3
            return@withContext cachedMp3
        }
        val cachedWav = File(audioCacheDir, "$key.wav")
        if (cachedWav.exists() && cachedWav.length() > 500) {
            prefetchCache[key] = cachedWav
            return@withContext cachedWav
        }

        // 1. Try Direct Microsoft Edge WebSocket Protocol (24kHz Studio MP3)
        try {
            val edgeBytes = fetchEdgeWebSocketAudio(text, voiceName)
            if (edgeBytes != null && edgeBytes.size > 500) {
                FileOutputStream(cachedMp3).use { fos -> fos.write(edgeBytes) }
                prefetchCache[key] = cachedMp3
                Log.d("EdgeTtsHelper", "Synthesized via Microsoft Edge Neural Voice (${edgeBytes.size} bytes)")
                return@withContext cachedMp3
            }
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "Edge WebSocket note: ${e.message}")
        }

        // 2. Try Google Gemini Dedicated Studio Neural Voice (24kHz Studio WAV)
        try {
            val geminiWav = fetchGeminiStudioAudio(text, voiceName)
            if (geminiWav != null && geminiWav.size > 500) {
                FileOutputStream(cachedWav).use { fos -> fos.write(geminiWav) }
                prefetchCache[key] = cachedWav
                Log.d("EdgeTtsHelper", "Synthesized via Gemini Studio Neural Human Voice (${geminiWav.size} bytes)")
                return@withContext cachedWav
            }
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "Gemini Studio stream note: ${e.message}")
        }

        // 3. Try Google Studio Neural Stream
        try {
            val googleBytes = fetchGoogleTtsAudio(text)
            if (googleBytes != null && googleBytes.size > 500) {
                FileOutputStream(cachedMp3).use { fos -> fos.write(googleBytes) }
                prefetchCache[key] = cachedMp3
                Log.d("EdgeTtsHelper", "Synthesized via Google Neural Stream (${googleBytes.size} bytes)")
                return@withContext cachedMp3
            }
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "Google stream note: ${e.message}")
        }

        return@withContext null
    }

    /**
     * Primary speech synthesis method:
     * Plays high-fidelity Natural Human Voice (MP3).
     * Automatically falls back to system TTS if offline.
     */
    fun speak(text: String, utteranceId: String = "edge_tts", onDone: (() -> Unit)? = null) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            onDone?.invoke()
            return
        }

        stop()
        _isSpeaking.value = true

        scope.launch {
            val audioFile = fetchNeuralAudioFile(cleanText)

            withContext(Dispatchers.Main) {
                if (audioFile != null && audioFile.exists()) {
                    playAudioFile(audioFile, onDone)
                } else {
                    // Fallback to local on-device TTS engine
                    speakFallback(cleanText, utteranceId, onDone)
                }
            }
        }
    }

    private fun playAudioFile(file: File, onDone: (() -> Unit)?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        Log.e("EdgeTtsHelper", "MediaPlayer start error", e)
                        _isSpeaking.value = false
                        onDone?.invoke()
                    }
                }
                setOnCompletionListener {
                    _isSpeaking.value = false
                    it.release()
                    mediaPlayer = null
                    onDone?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("EdgeTtsHelper", "MediaPlayer error: $what, $extra")
                    _isSpeaking.value = false
                    mediaPlayer?.release()
                    mediaPlayer = null
                    onDone?.invoke()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("EdgeTtsHelper", "Failed to play audio file: ${e.message}", e)
            _isSpeaking.value = false
            onDone?.invoke()
        }
    }

    private fun speakFallback(text: String, utteranceId: String, onDone: (() -> Unit)?) {
        if (isLocalTtsReady && localTts != null) {
            localTts?.setSpeechRate(speechRate)
            localTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            val estimatedDurationMs = ((text.split("\\s+".toRegex()).size / 2.2) * 1000).toLong().coerceIn(1200L, 8000L)
            mainHandler.postDelayed({
                if (_isSpeaking.value) {
                    _isSpeaking.value = false
                    onDone?.invoke()
                }
            }, estimatedDurationMs)
        } else {
            _isSpeaking.value = false
            onDone?.invoke()
        }
    }

    fun updateSpeechRate(rate: Float) {
        speechRate = rate
    }

    fun setVoiceProfile(locale: Locale = Locale.US, speechRate: Float = 0.88f, pitch: Float = 1.0f) {
        this.speechRate = speechRate
        this.pitch = pitch
        localTts?.language = locale
        localTts?.setSpeechRate(speechRate)
    }

    fun stop() {
        _isSpeaking.value = false
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "MediaPlayer stop note: ${e.message}")
        }

        try {
            localTts?.stop()
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "Local TTS stop note: ${e.message}")
        }
    }

    fun shutdown() {
        stop()
        try {
            localTts?.shutdown()
            localTts = null
            isLocalTtsReady = false
        } catch (e: Exception) {
            Log.w("EdgeTtsHelper", "Local TTS shutdown note: ${e.message}")
        }
    }
}
