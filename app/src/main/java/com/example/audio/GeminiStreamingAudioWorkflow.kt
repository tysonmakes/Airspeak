package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Result data class emitted upon completion of an interactive AI speech turn
 */
data class StreamingTurnResult(
    val userSpeech: String,
    val aiReply: String,
    val audioBytes: ByteArray? = null,
    val correction: String? = null,
    val praise: String? = null,
    val latencyMs: Long = 0L,
    val wasWebSocketLiveStream: Boolean = false
)

/**
 * High-Performance Streaming Audio Processing Workflow for Gemini API.
 *
 * Core Capabilities:
 * 1. Low-Latency Microphone Capture: Records 16kHz 16-bit Mono PCM in ~100ms micro-chunks (3200 bytes).
 * 2. Instant Chunk Streaming: Dispatches short audio chunks to the Gemini Live API as they are recorded,
 *    eliminating the wait-for-recording-to-finish bottleneck.
 * 3. Bidirectional WebSocket Live Session: Connects to Gemini's BidiGenerateContent endpoint,
 *    allowing the AI model to ingest audio continuously while the user speaks.
 * 4. Incremental Audio Playback: Streams incoming 24kHz PCM audio chunks directly into AudioTrack (MODE_STREAM)
 *    so AI voice playback begins in under 300ms.
 * 5. Dynamic VAD & Fallback Pipeline: Robust voice activity detection with seamless fallback to
 *    in-memory pre-buffered fast direct voice synthesis if WebSocket hits network limits.
 */
class GeminiStreamingAudioWorkflow(
    private val context: Context,
    private val nativeAudioPlayer: GeminiNativeAudioPlayer
) {
    private val TAG = "GeminiAudioStream"
    private val workflowScope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Audio recording hardware configuration
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val CHUNK_SAMPLES = 1600 // 100ms at 16kHz
    private val CHUNK_BYTES = CHUNK_SAMPLES * 2 // 3200 bytes

    // State Flows
    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    private val _isUserSpeaking = MutableStateFlow(false)
    val isUserSpeaking: StateFlow<Boolean> = _isUserSpeaking.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _currentLiveTranscript = MutableStateFlow("")
    val currentLiveTranscript: StateFlow<String> = _currentLiveTranscript.asStateFlow()

    private val _aiStreamingReply = MutableStateFlow("")
    val aiStreamingReply: StateFlow<String> = _aiStreamingReply.asStateFlow()

    private val _isAiSpeaking = MutableStateFlow(false)
    val isAiSpeaking: StateFlow<Boolean> = _isAiSpeaking.asStateFlow()

    private val _chunksStreamedCount = MutableStateFlow(0)
    val chunksStreamedCount: StateFlow<Int> = _chunksStreamedCount.asStateFlow()

    private val _streamingLatencyMs = MutableStateFlow(0L)
    val streamingLatencyMs: StateFlow<Long> = _streamingLatencyMs.asStateFlow()

    private val _workflowStatus = MutableStateFlow("Idle")
    val workflowStatus: StateFlow<String> = _workflowStatus.asStateFlow()

    // Recording & WebSocket handles
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var webSocket: WebSocket? = null
    private var isWebSocketConnected = false
    private var isSessionConfigured = false

    // Hardware effects
    private var hwAec: AcousticEchoCanceler? = null
    private var hwNs: NoiseSuppressor? = null
    private var hwAgc: AutomaticGainControl? = null

    // Rolling audio buffer during recording
    private val audioBufferAccumulator = ByteArrayOutputStream()
    private var speechStartTimestamp = 0L
    private var speechEndTimestamp = 0L
    private var lastSpeechDetectedTime = 0L
    private var firstAiChunkTimestamp = 0L

    // Active session metadata
    private var currentTutorName = "Sophia"
    private var currentVoiceName = "Aoede"
    private var currentTopic = "General English"
    private var currentSystemPrompt = ""
    private var onTurnCompleteCallback: ((StreamingTurnResult) -> Unit)? = null

    // VAD Configuration
    private val VAD_SILENCE_THRESHOLD_MS = 1000L
    private val VAD_MIN_SPEECH_DURATION_MS = 400L
    private var hasDetectedSpeechInCurrentTurn = false
    private var isProcessingTurn = false

    // OkHttp Client configured for low-latency WebSocket streaming
    private val webSocketClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read for persistent stream
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start the streaming audio workflow for an active live call.
     * Connects the Gemini WebSocket session and begins background chunk streaming.
     */
    fun startStreamingWorkflow(
        tutorName: String,
        tutorPersona: String,
        topic: String,
        voiceName: String = "Aoede",
        onTurnComplete: (StreamingTurnResult) -> Unit
    ) {
        if (!hasRecordPermission()) {
            Log.w(TAG, "Cannot start streaming audio: Record permission not granted")
            return
        }

        stopStreamingWorkflow()

        currentTutorName = tutorName
        currentVoiceName = voiceName
        currentTopic = topic
        onTurnCompleteCallback = onTurnComplete

        currentSystemPrompt = """
            You are $tutorName, $tutorPersona.
            You are participating in an interactive, ultra-low latency conversational English speaking call with a learner on '$topic'.
            Speak naturally and warmly in 1-2 concise sentences.
            If the user makes a clear grammatical or vocabulary mistake, append: "*Correction:* [brief tip]".
            If they express something exceptionally well, append: "*Praise:* [brief praise]".
        """.trimIndent()

        _isStreamingActive.value = true
        _workflowStatus.value = "Connecting to Gemini Live..."

        // Connect Gemini Live WebSocket
        connectGeminiLiveWebSocket()

        // Start listening to microphone and streaming chunks
        startMicrophoneCaptureLoop()
    }

    /**
     * Establishes the real-time Gemini Live WebSocket connection.
     */
    private fun connectGeminiLiveWebSocket() {
        val apiKey = GeminiClient.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "No valid Gemini API key for Live WebSocket, will use hybrid fallback.")
            _workflowStatus.value = "Using Hybrid Fast Stream"
            return
        }

        // Target Gemini Live BidiGenerateContent endpoint
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocketClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket Connected!")
                webSocket = ws
                isWebSocketConnected = true
                sendInitialSetupFrame(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingGeminiLiveMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Gemini WebSocket closing: $code / $reason")
                isWebSocketConnected = false
                isSessionConfigured = false
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Gemini WebSocket failure (${t.message}), switching to hybrid stream.", t)
                isWebSocketConnected = false
                isSessionConfigured = false
                GeminiClient.checkAndRecordQuotaException(t)
                _workflowStatus.value = "Hybrid Fast Stream Active"
            }
        })
    }

    /**
     * Sends the initial Setup JSON frame to Gemini Live API
     */
    private fun sendInitialSetupFrame(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                val setupObj = JSONObject().apply {
                    // Gemini native audio live models
                    put("model", "models/gemini-2.5-flash-native-audio-preview-12-2025")

                    val genConfig = JSONObject().apply {
                        val modalities = JSONArray().apply {
                            put("AUDIO")
                            put("TEXT")
                        }
                        put("responseModalities", modalities)

                        val speechConfig = JSONObject().apply {
                            val voiceConfig = JSONObject().apply {
                                val prebuilt = JSONObject().apply {
                                    put("voiceName", currentVoiceName)
                                }
                                put("prebuiltVoiceConfig", prebuilt)
                            }
                            put("voiceConfig", voiceConfig)
                        }
                        put("speechConfig", speechConfig)
                    }
                    put("generationConfig", genConfig)

                    val sysInstr = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val p = JSONObject().apply {
                                put("text", currentSystemPrompt)
                            }
                            put(p)
                        }
                        put("parts", parts)
                    }
                    put("systemInstruction", sysInstr)
                }
                put("setup", setupObj)
            }

            ws.send(setupJson.toString())
            Log.d(TAG, "Sent Gemini Live setup frame with voice $currentVoiceName")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending setup frame", e)
        }
    }

    /**
     * Parse incoming messages from Gemini Live API
     */
    private fun handleIncomingGeminiLiveMessage(messageJson: String) {
        try {
            val root = JSONObject(messageJson)

            if (root.has("setupComplete")) {
                isSessionConfigured = true
                _workflowStatus.value = "Gemini Live Stream Ready ⚡"
                Log.d(TAG, "Gemini Live Session Configured & Ready for Audio Chunks")
                return
            }

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")

                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Check for text tokens
                            if (part.has("text")) {
                                val chunkText = part.getString("text")
                                if (firstAiChunkTimestamp == 0L) {
                                    firstAiChunkTimestamp = System.currentTimeMillis()
                                    if (speechEndTimestamp > 0) {
                                        val latency = firstAiChunkTimestamp - speechEndTimestamp
                                        _streamingLatencyMs.value = latency
                                    }
                                }
                                mainHandler.post {
                                    _aiStreamingReply.value += chunkText
                                }
                            }

                            // Check for direct audio PCM chunks
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Audio = inlineData.optString("data")
                                if (!base64Audio.isNullOrBlank()) {
                                    if (firstAiChunkTimestamp == 0L) {
                                        firstAiChunkTimestamp = System.currentTimeMillis()
                                        if (speechEndTimestamp > 0) {
                                            val latency = firstAiChunkTimestamp - speechEndTimestamp
                                            _streamingLatencyMs.value = latency
                                        }
                                    }
                                    val pcmChunk = Base64.decode(base64Audio, Base64.DEFAULT)
                                    if (pcmChunk.isNotEmpty()) {
                                        _isAiSpeaking.value = true
                                        nativeAudioPlayer.writeStreamingChunk(pcmChunk)
                                    }
                                }
                            }
                        }
                    }
                }

                val turnComplete = serverContent.optBoolean("turnComplete", false)
                val interrupted = serverContent.optBoolean("interrupted", false)

                if (interrupted) {
                    Log.d(TAG, "User interrupted AI speech")
                    nativeAudioPlayer.stop()
                    _isAiSpeaking.value = false
                }

                if (turnComplete) {
                    val fullReply = _aiStreamingReply.value.trim()
                    Log.d(TAG, "Turn complete from Gemini Live! Text: $fullReply")

                    nativeAudioPlayer.finishStreamingMode {
                        _isAiSpeaking.value = false
                        completeTurn(fullReply, wasWebSocket = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing incoming Gemini Live message: ${e.message}")
        }
    }

    /**
     * Initializes and starts the microphone capture loop.
     * Reads short 100ms chunks and immediately dispatches them to Gemini.
     */
    private fun startMicrophoneCaptureLoop() {
        recordingJob?.cancel()
        recordingJob = workflowScope.launch {
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = max(minBufSize, CHUNK_BYTES * 2)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                // Attach acoustic effects where supported
                val audioSessionId = audioRecord?.audioSessionId ?: 0
                if (audioSessionId != 0) {
                    if (AcousticEchoCanceler.isAvailable()) {
                        hwAec = AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        hwNs = NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
                    }
                    if (AutomaticGainControl.isAvailable()) {
                        hwAgc = AutomaticGainControl.create(audioSessionId)?.apply { enabled = true }
                    }
                }

                audioRecord?.startRecording()
                Log.d(TAG, "Microphone capture loop started (16kHz, 100ms chunks)")

                val chunkBuffer = ByteArray(CHUNK_BYTES)

                while (isActive && _isStreamingActive.value) {
                    val bytesRead = audioRecord?.read(chunkBuffer, 0, CHUNK_BYTES) ?: -1

                    if (bytesRead > 0) {
                        // 1. Calculate RMS volume & update level meter
                        val rms = calculateRms(chunkBuffer, bytesRead)
                        val db = if (rms > 1.0) 20 * log10(rms) else 0.0
                        val normalizedDb = (db.toFloat() - 30f).coerceIn(0f, 10f)

                        _rmsDb.value = normalizedDb

                        // 2. Real-time Voice Activity Detection (VAD)
                        val isVoicing = db > 38.0 // Threshold for active speech

                        val now = System.currentTimeMillis()

                        if (isVoicing) {
                            if (!hasDetectedSpeechInCurrentTurn) {
                                hasDetectedSpeechInCurrentTurn = true
                                speechStartTimestamp = now
                                _isUserSpeaking.value = true
                                _currentLiveTranscript.value = "Listening to your speech..."
                            }
                            lastSpeechDetectedTime = now

                            // Accumulate into rolling buffer
                            audioBufferAccumulator.write(chunkBuffer, 0, bytesRead)

                            // 3. STREAM AUDIO CHUNK IMMEDIATELY TO GEMINI
                            sendAudioChunkToGemini(chunkBuffer, bytesRead)

                        } else if (hasDetectedSpeechInCurrentTurn) {
                            // Silence during speech turn: check if speech has naturally concluded
                            audioBufferAccumulator.write(chunkBuffer, 0, bytesRead)

                            // Still stream the boundary chunk for natural trailing audio
                            sendAudioChunkToGemini(chunkBuffer, bytesRead)

                            val silenceDuration = now - lastSpeechDetectedTime
                            val totalSpeechDuration = now - speechStartTimestamp

                            if (silenceDuration >= VAD_SILENCE_THRESHOLD_MS &&
                                totalSpeechDuration >= VAD_MIN_SPEECH_DURATION_MS &&
                                !isProcessingTurn
                            ) {
                                // User has finished speaking!
                                Log.d(TAG, "Natural silence detected ($silenceDuration ms). Triggering turn processing.")
                                isProcessingTurn = true
                                _isUserSpeaking.value = false
                                speechEndTimestamp = now
                                firstAiChunkTimestamp = 0L

                                handleUserFinishedSpeaking()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Microphone capture loop failed", e)
            } finally {
                releaseAudioRecord()
            }
        }
    }

    /**
     * Sends a short audio PCM chunk immediately to the Gemini Live WebSocket.
     */
    private fun sendAudioChunkToGemini(chunk: ByteArray, bytesRead: Int) {
        _chunksStreamedCount.value += 1

        val ws = webSocket
        if (ws != null && isWebSocketConnected && isSessionConfigured) {
            try {
                val base64Data = Base64.encodeToString(chunk, 0, bytesRead, Base64.NO_WRAP)
                val chunkJson = JSONObject().apply {
                    val realtimeInput = JSONObject().apply {
                        val mediaChunks = JSONArray().apply {
                            val media = JSONObject().apply {
                                put("mimeType", "audio/pcm;rate=16000")
                                put("data", base64Data)
                            }
                            put(media)
                        }
                        put("mediaChunks", mediaChunks)
                    }
                    put("realtimeInput", realtimeInput)
                }

                ws.send(chunkJson.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send chunk over WebSocket: ${e.message}")
            }
        }
    }

    /**
     * Invoked the exact millisecond the user stops speaking.
     * If the WebSocket stream is active, Gemini Live responds directly.
     * If not, the pre-buffered audio chunks in memory are dispatched instantly to Gemini REST without disk delays.
     */
    private fun handleUserFinishedSpeaking() {
        workflowScope.launch {
            _workflowStatus.value = "Gemini thinking..."

            // If Gemini Live WebSocket is actively handling the turn, we wait for turnComplete
            if (isWebSocketConnected && isSessionConfigured) {
                Log.d(TAG, "Waiting for Gemini Live streaming response...")
                return@launch
            }

            // Otherwise, trigger the zero-disk-delay hybrid fast audio turn!
            val rawPcmBytes = audioBufferAccumulator.toByteArray()
            if (rawPcmBytes.isEmpty()) {
                resetTurnState()
                return@launch
            }

            val wavBytes = GeminiClient.convertPcmToWav(rawPcmBytes, sampleRate = 16000, channels = 1)
            val audioBase64 = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

            val turnStartTime = System.currentTimeMillis()
            val nativeResult = GeminiClient.queryGeminiNativeVoiceTurn(
                userText = null,
                userAudioBase64 = audioBase64,
                userAudioMime = "audio/wav",
                systemInstruction = currentSystemPrompt,
                voiceName = currentVoiceName
            )

            val turnLatency = System.currentTimeMillis() - turnStartTime
            _streamingLatencyMs.value = turnLatency

            if (nativeResult != null && nativeResult.audioBytes != null && nativeResult.audioBytes.isNotEmpty()) {
                _isAiSpeaking.value = true
                nativeAudioPlayer.playNativeAudio(nativeResult.audioBytes, nativeResult.audioMimeType) {
                    _isAiSpeaking.value = false
                    completeTurn(nativeResult.spokenText, wasWebSocket = false, audioBytes = nativeResult.audioBytes)
                }
            } else {
                completeTurn("I heard what you said! Let's keep talking about $currentTopic.", wasWebSocket = false)
            }
        }
    }

    /**
     * Completes an AI response turn, extracts corrections/praise, and notifies the caller.
     */
    private fun completeTurn(
        aiReply: String,
        wasWebSocket: Boolean,
        audioBytes: ByteArray? = null
    ) {
        var cleanReply = aiReply
        var correction: String? = null
        var praise: String? = null

        if (aiReply.contains("*Correction:*")) {
            correction = aiReply.substringAfter("*Correction:*").substringBefore("*Praise:*").substringBefore("\n").trim()
            cleanReply = cleanReply.replace("*Correction:* $correction", "").trim()
        }
        if (aiReply.contains("*Praise:*")) {
            praise = aiReply.substringAfter("*Praise:*").substringBefore("\n").trim()
            cleanReply = cleanReply.replace("*Praise:* $praise", "").trim()
        }

        val result = StreamingTurnResult(
            userSpeech = _currentLiveTranscript.value,
            aiReply = cleanReply.ifBlank { "That's very interesting! Tell me more." },
            audioBytes = audioBytes,
            correction = correction,
            praise = praise,
            latencyMs = _streamingLatencyMs.value,
            wasWebSocketLiveStream = wasWebSocket
        )

        mainHandler.post {
            onTurnCompleteCallback?.invoke(result)
            resetTurnState()
        }
    }

    /**
     * Resets internal turn counters ready for the next user speech.
     */
    private fun resetTurnState() {
        audioBufferAccumulator.reset()
        hasDetectedSpeechInCurrentTurn = false
        isProcessingTurn = false
        speechStartTimestamp = 0L
        speechEndTimestamp = 0L
        firstAiChunkTimestamp = 0L
        _currentLiveTranscript.value = ""
        _aiStreamingReply.value = ""
        _workflowStatus.value = if (isWebSocketConnected) "Gemini Live Stream Ready ⚡" else "Listening..."
    }

    /**
     * Manually triggers completion of the user's speech immediately without waiting for VAD silence timeout.
     */
    fun completeSpeechNow() {
        if (hasDetectedSpeechInCurrentTurn && !isProcessingTurn) {
            isProcessingTurn = true
            _isUserSpeaking.value = false
            speechEndTimestamp = System.currentTimeMillis()
            handleUserFinishedSpeaking()
        }
    }

    /**
     * Immediately interrupts tutor speech and pauses playback.
     */
    fun interruptTutor() {
        nativeAudioPlayer.stop()
        _isAiSpeaking.value = false
        resetTurnState()
    }

    /**
     * Stops the entire streaming workflow and releases microphone and network resources.
     */
    fun stopStreamingWorkflow() {
        _isStreamingActive.value = false
        _isUserSpeaking.value = false
        _isAiSpeaking.value = false
        _workflowStatus.value = "Stopped"

        recordingJob?.cancel()
        recordingJob = null

        try {
            webSocket?.close(1000, "Session ended")
        } catch (_: Exception) {}
        webSocket = null
        isWebSocketConnected = false
        isSessionConfigured = false

        releaseAudioRecord()
        audioBufferAccumulator.reset()
    }

    private fun releaseAudioRecord() {
        try {
            hwAec?.release()
            hwNs?.release()
            hwAgc?.release()
            hwAec = null
            hwNs = null
            hwAgc = null
        } catch (_: Exception) {}

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
        } catch (_: Exception) {}
    }

    private fun calculateRms(buffer: ByteArray, bytesRead: Int): Double {
        var sum = 0.0
        val numSamples = bytesRead / 2
        for (i in 0 until bytesRead step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            sum += sample.toShort() * sample.toShort()
        }
        return if (numSamples > 0) sqrt(sum / numSamples) else 0.0
    }
}
