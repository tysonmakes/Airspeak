package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Microphone Health & Level Status indicator
 */
enum class MicCheckStatus {
    UNCHECKED,
    CHECKING,
    OPTIMAL,
    LOW_VOLUME,
    NOISY,
    MUTED,
    READY
}

/**
 * Ultra-Low Latency Speech Recognition Helper with Uninterrupted Continuous Listening,
 * Microphone Pre-check, and Automatic Gain Control (AGC).
 *
 * Key Architectural Highlights:
 * 1. Warm-Instance Reuse: Reuses SpeechRecognizer instances instead of expensive re-creation.
 * 2. On-Device STT: Leverages API 31+ On-Device SpeechRecognizer when available for zero-latency.
 * 3. Clause Accumulation: Prevents premature termination by combining sentence clauses across natural pauses.
 * 4. Dynamic VAD & AGC: Intelligent RMS-based Voice Activity Detection with Auto-Gain Control normalization.
 * 5. Microphone Calibration: Pre-checks microphone response, ambient noise floor, and gain factor.
 * 6. Instant Tap-to-Send: Zero-delay dispatch via completeSpeechNow().
 */
class SpeechRecognitionHelper(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val helperScope = CoroutineScope(Dispatchers.Default)
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _isAvailable = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _candidates = MutableStateFlow<List<String>>(emptyList())
    val candidates: StateFlow<List<String>> = _candidates.asStateFlow()

    // Microphone check & AGC status
    private val _micStatus = MutableStateFlow(MicCheckStatus.UNCHECKED)
    val micStatus: StateFlow<MicCheckStatus> = _micStatus.asStateFlow()

    private val _inputGainFactor = MutableStateFlow(1.0f)
    val inputGainFactor: StateFlow<Float> = _inputGainFactor.asStateFlow()

    private val _calibratedNoiseFloor = MutableStateFlow(1.0f)
    val calibratedNoiseFloor: StateFlow<Float> = _calibratedNoiseFloor.asStateFlow()

    // Multi-turn callbacks
    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onFinalCandidatesCallback: ((String, List<String>) -> Unit)? = null

    // Configuration
    private var currentSilenceTimeoutMs: Long = 1800L
    private var continuousMode: Boolean = true

    // Sentence accumulation across mid-sentence pauses
    private val accumulatedUtterance = StringBuilder()
    private var lastSpeechTimeMs: Long = 0L

    // Dynamic VAD timer: triggers dispatch exactly after natural silence threshold
    private var vadSilenceRunnable: Runnable? = null
    private var isDispatching = false
    private var isInternalListening = false

    // Hardware audio effects
    private var hwAgc: AutomaticGainControl? = null
    private var hwNs: NoiseSuppressor? = null
    private var hwAec: AcousticEchoCanceler? = null

    init {
        // Run a lightweight background microphone check/calibration upon instantiation if permission is granted
        if (hasRecordPermission()) {
            performMicCheck()
        }
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Performs a fast 250ms microphone probe to measure ambient noise floor,
     * calibrate optimal gain factor, and verify hardware input health.
     */
    fun performMicCheck(onComplete: ((MicCheckStatus) -> Unit)? = null) {
        if (!hasRecordPermission()) {
            _micStatus.value = MicCheckStatus.UNCHECKED
            onComplete?.invoke(MicCheckStatus.UNCHECKED)
            return
        }

        helperScope.launch {
            _micStatus.value = MicCheckStatus.CHECKING
            val status = measureMicLevelsAndCalibrate()
            _micStatus.value = status
            withContext(Dispatchers.Main) {
                onComplete?.invoke(status)
            }
        }
    }

    private suspend fun measureMicLevelsAndCalibrate(): MicCheckStatus = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = max(minBufSize, 4096)

        var audioRecord: AudioRecord? = null
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.w("SpeechRecognitionHelper", "AudioRecord initialization failed during mic check")
                return@withContext MicCheckStatus.READY
            }

            // Attach hardware AGC/Noise suppression if available on this session
            val audioSessionId = audioRecord.audioSessionId
            if (audioSessionId != 0) {
                if (AutomaticGainControl.isAvailable()) {
                    hwAgc = AutomaticGainControl.create(audioSessionId)?.apply { enabled = true }
                }
                if (NoiseSuppressor.isAvailable()) {
                    hwNs = NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    hwAec = AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
                }
            }

            audioRecord.startRecording()
            val buffer = ShortArray(1024)
            var totalRms = 0.0
            var sampleCount = 0
            val startTime = System.currentTimeMillis()

            // Sample for 250ms
            while (System.currentTimeMillis() - startTime < 250L) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = sqrt(sum / read)
                    val db = if (rms > 1.0) (20.0 * log10(rms)).toFloat() else 0f
                    totalRms += db
                    sampleCount++
                }
            }

            val avgDb = if (sampleCount > 0) (totalRms / sampleCount).toFloat() else 0f
            Log.d("SpeechRecognitionHelper", "Mic check calibrated ambient dB: $avgDb")

            // Auto-Gain Control (AGC) & Noise floor calibration:
            // Normal speech is ~40-70 dB in raw PCM; silence is ~10-30 dB.
            val calibratedStatus = when {
                avgDb <= 2.0f -> {
                    // Very low signal or muted mic
                    _inputGainFactor.value = 1.8f
                    _calibratedNoiseFloor.value = 0.8f
                    MicCheckStatus.LOW_VOLUME
                }
                avgDb in 2.1f..38.0f -> {
                    // Normal optimal ambient environment
                    _inputGainFactor.value = 1.3f
                    _calibratedNoiseFloor.value = (avgDb / 20.0f).coerceIn(0.5f, 2.0f)
                    MicCheckStatus.OPTIMAL
                }
                avgDb > 38.0f -> {
                    // Noisy environment; lower gain slightly to prevent background noise triggering
                    _inputGainFactor.value = 1.0f
                    _calibratedNoiseFloor.value = (avgDb / 15.0f).coerceIn(1.8f, 3.5f)
                    MicCheckStatus.NOISY
                }
                else -> {
                    _inputGainFactor.value = 1.2f
                    _calibratedNoiseFloor.value = 1.0f
                    MicCheckStatus.OPTIMAL
                }
            }

            return@withContext calibratedStatus
        } catch (e: Exception) {
            Log.w("SpeechRecognitionHelper", "Error during mic level calibration: ${e.message}")
            _inputGainFactor.value = 1.2f
            _calibratedNoiseFloor.value = 1.0f
            return@withContext MicCheckStatus.READY
        } finally {
            try {
                hwAgc?.release()
                hwNs?.release()
                hwAec?.release()
                hwAgc = null
                hwNs = null
                hwAec = null
            } catch (_: Exception) {}
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Start continuous speech recognition with natural sentence protection and AGC normalization.
     */
    fun startListening(
        silenceTimeoutMs: Long = 1800L,
        continuous: Boolean = true,
        onResult: (String) -> Unit
    ) {
        currentSilenceTimeoutMs = silenceTimeoutMs
        continuousMode = continuous
        onFinalResultCallback = onResult
        onFinalCandidatesCallback = null
        resetSessionState()

        mainHandler.post {
            ensureRecognizerReady(forceRecreate = (speechRecognizer == null))
            startListeningSession()
        }
    }

    /**
     * Start speech recognition returning all N-best candidate strings for evaluation.
     */
    fun startListeningWithCandidates(
        silenceTimeoutMs: Long = 2000L,
        continuous: Boolean = false,
        onResult: (best: String, candidates: List<String>) -> Unit
    ) {
        currentSilenceTimeoutMs = silenceTimeoutMs
        continuousMode = continuous
        onFinalResultCallback = { best -> onResult(best, _candidates.value) }
        onFinalCandidatesCallback = onResult
        resetSessionState()

        mainHandler.post {
            ensureRecognizerReady(forceRecreate = (speechRecognizer == null))
            startListeningSession()
        }
    }

    private fun resetSessionState() {
        cancelVadTimer()
        accumulatedUtterance.clear()
        _currentText.value = ""
        _candidates.value = emptyList()
        isDispatching = false
        lastSpeechTimeMs = System.currentTimeMillis()
    }

    private fun cancelVadTimer() {
        vadSilenceRunnable?.let { mainHandler.removeCallbacks(it) }
        vadSilenceRunnable = null
    }

    private fun scheduleVadSilenceDispatch(delayMs: Long) {
        cancelVadTimer()
        vadSilenceRunnable = Runnable {
            val text = getCombinedCurrentText()
            if (text.isNotBlank() && !isDispatching) {
                isDispatching = true
                Log.d("SpeechRecognitionHelper", "Natural sentence end reached ($delayMs ms). Dispatching: $text")
                dispatchFinalResult(text, _candidates.value.ifEmpty { listOf(text) })
            }
        }
        mainHandler.postDelayed(vadSilenceRunnable!!, delayMs)
    }

    /**
     * Combines accumulated clauses from previous pauses with the active partial utterance.
     */
    private fun getCombinedCurrentText(): String {
        val active = _currentText.value.trim()
        val accumulated = accumulatedUtterance.toString().trim()
        return when {
            accumulated.isNotBlank() && active.isNotBlank() -> {
                if (active.startsWith(accumulated, ignoreCase = true)) active
                else "$accumulated $active"
            }
            active.isNotBlank() -> active
            accumulated.isNotBlank() -> accumulated
            else -> ""
        }.trim()
    }

    /**
     * Initializes or warms up the SpeechRecognizer instance.
     * If forceRecreate is true, cleans up any previous instance and creates a fresh instance.
     */
    fun ensureRecognizerReady(forceRecreate: Boolean = false) {
        if (forceRecreate && speechRecognizer != null) {
            cleanupRecognizer()
        }
        if (speechRecognizer != null) return

        try {
            val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                Log.d("SpeechRecognitionHelper", "Using ultra-low-latency On-Device SpeechRecognizer")
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            recognizer.setRecognitionListener(createListener())
            speechRecognizer = recognizer
        } catch (e: Exception) {
            Log.w("SpeechRecognitionHelper", "Failed to create SpeechRecognizer, falling back", e)
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (ex: Exception) {
                Log.e("SpeechRecognitionHelper", "Fatal error initializing SpeechRecognizer", ex)
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                isInternalListening = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
                isInternalListening = true
                lastSpeechTimeMs = System.currentTimeMillis()
                cancelVadTimer()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Apply Software Auto-Gain Control (AGC) scaling factor to normalize soft speech
                val gain = _inputGainFactor.value
                val scaledRms = (rmsdB * gain).coerceIn(0f, 10f)
                _rmsDb.value = scaledRms

                // Dynamic VAD threshold adaptive to calibrated noise floor
                val noiseFloor = _calibratedNoiseFloor.value
                val speechThreshold = max(1.5f, noiseFloor + 0.8f)

                if (scaledRms > speechThreshold) {
                    lastSpeechTimeMs = System.currentTimeMillis()
                    cancelVadTimer()
                    
                    // Dynamic AGC: If user is consistently speaking softly, gently boost gain
                    if (scaledRms in speechThreshold..3.5f && _inputGainFactor.value < 2.0f) {
                        _inputGainFactor.value = min(2.0f, _inputGainFactor.value + 0.05f)
                    }
                } else if (_currentText.value.isNotBlank() && vadSilenceRunnable == null) {
                    // User paused; start silence window countdown
                    scheduleVadSilenceDispatch(currentSilenceTimeoutMs)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isInternalListening = false
                // Pause detected. Extend window to avoid cutting off mid-sentence thoughts.
                if (getCombinedCurrentText().isNotBlank() && !isDispatching) {
                    scheduleVadSilenceDispatch(currentSilenceTimeoutMs)
                }
            }

            override fun onError(error: Int) {
                isInternalListening = false
                val isNonFatalSilence = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                Log.d("SpeechRecognitionHelper", "Recognition error code: $error (isNonFatal: $isNonFatalSilence)")

                if (isNonFatalSilence) {
                    // User simply hasn't spoken yet or paused briefly
                    if (continuousMode && !isDispatching) {
                        mainHandler.postDelayed({
                            if (continuousMode && !isDispatching && !isInternalListening) {
                                startListeningSession()
                            }
                        }, 100L)
                    }
                    return
                }

                // If recoverable text exists, check if we should dispatch
                val fallbackText = getCombinedCurrentText()
                
                // For hardware/client/busy errors: destroy corrupted recognizer immediately
                cleanupRecognizer()

                if (fallbackText.isNotBlank() && !isDispatching && !continuousMode) {
                    isDispatching = true
                    dispatchFinalResult(fallbackText, listOf(fallbackText))
                } else if (continuousMode && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    // Re-arm cleanly with fresh instance
                    mainHandler.postDelayed({
                        if (continuousMode && !isDispatching) {
                            ensureRecognizerReady(forceRecreate = true)
                            startListeningSession()
                        }
                    }, 250L)
                } else {
                    _isListening.value = false
                }
            }

            override fun onResults(results: Bundle?) {
                isInternalListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                val allMatches = matches?.filter { it.isNotBlank() } ?: listOfNotNull(text.takeIf { it.isNotBlank() })

                if (text.isNotBlank()) {
                    _candidates.value = allMatches
                    // Append this clause to the accumulated utterance
                    if (accumulatedUtterance.isBlank()) {
                        accumulatedUtterance.append(text)
                    } else if (!accumulatedUtterance.contains(text, ignoreCase = true)) {
                        accumulatedUtterance.append(" ").append(text)
                    }
                    _currentText.value = accumulatedUtterance.toString().trim()
                }

                val fullText = getCombinedCurrentText()

                if (continuousMode) {
                    // For continuous mode, check if we reached natural silence or should re-arm session for next clause
                    val elapsedSinceSpeech = System.currentTimeMillis() - lastSpeechTimeMs
                    if (fullText.isNotBlank() && elapsedSinceSpeech >= currentSilenceTimeoutMs && !isDispatching) {
                        isDispatching = true
                        dispatchFinalResult(fullText, _candidates.value.ifEmpty { listOf(fullText) })
                    } else {
                        // Keep listening seamlessly for the rest of the user's sentence
                        mainHandler.postDelayed({
                            if (continuousMode && !isDispatching) {
                                startListeningSession()
                            }
                        }, 50L)
                    }
                } else {
                    // One-shot mode
                    if (fullText.isNotBlank() && !isDispatching) {
                        isDispatching = true
                        dispatchFinalResult(fullText, allMatches)
                    } else {
                        _isListening.value = false
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim()
                if (!text.isNullOrBlank()) {
                    lastSpeechTimeMs = System.currentTimeMillis()
                    val combined = if (accumulatedUtterance.isNotBlank()) {
                        if (text.startsWith(accumulatedUtterance.toString(), ignoreCase = true)) text
                        else "${accumulatedUtterance.toString().trim()} $text"
                    } else {
                        text
                    }
                    _currentText.value = combined
                    _candidates.value = matches.filter { it.isNotBlank() }
                    scheduleVadSilenceDispatch(currentSilenceTimeoutMs)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun startListeningSession() {
        if (!hasRecordPermission()) {
            _isListening.value = false
            return
        }

        try {
            ensureRecognizerReady()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                
                // Adaptive Locale handling: Support Indian English, US English, British English
                val sysTag = Locale.getDefault().toLanguageTag()
                val primaryLang = if (sysTag.startsWith("en", ignoreCase = true)) sysTag else "en-US"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLang)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "en-GB", "en-AU"))
                
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                
                // Generous silence thresholds to prevent cutting off speech mid-sentence
                val silenceComplete = maxOf(2500L, currentSilenceTimeoutMs + 600L)
                val silencePossible = maxOf(2000L, currentSilenceTimeoutMs + 200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceComplete)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silencePossible)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 250L)
                
                // Low latency preference if supported
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            isInternalListening = true
        } catch (e: Exception) {
            Log.e("SpeechRecognitionHelper", "Error starting listening session", e)
            _isListening.value = false
            isInternalListening = false
        }
    }

    private fun dispatchFinalResult(text: String, candidatesList: List<String> = emptyList()) {
        cancelVadTimer()
        _isListening.value = false
        isInternalListening = false
        val cleanText = text.trim()

        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        onFinalCandidatesCallback?.invoke(cleanText, candidatesList)
        onFinalResultCallback?.invoke(cleanText)
    }

    /**
     * Force immediate completion of current spoken turn (Tap-to-Send ⚡).
     */
    fun completeSpeechNow() {
        val text = getCombinedCurrentText()
        if (text.isNotBlank() && !isDispatching) {
            isDispatching = true
            dispatchFinalResult(text, _candidates.value.ifEmpty { listOf(text) })
        } else {
            stopListening()
        }
    }

    fun stopListening() {
        continuousMode = false
        cancelVadTimer()
        isDispatching = false
        isInternalListening = false
        _isListening.value = false
        _rmsDb.value = 0f
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w("SpeechRecognitionHelper", "Error stopping recognizer: ${e.message}")
            }
        }
    }

    private fun cleanupRecognizer() {
        try {
            hwAgc?.release()
            hwNs?.release()
            hwAec?.release()
            hwAgc = null
            hwNs = null
            hwAec = null
        } catch (_: Exception) {}

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w("SpeechRecognitionHelper", "Error cleaning recognizer: ${e.message}")
        }
        _isListening.value = false
        _rmsDb.value = 0f
        isInternalListening = false
    }

    fun shutdown() {
        stopListening()
        mainHandler.post {
            cleanupRecognizer()
        }
    }
}

