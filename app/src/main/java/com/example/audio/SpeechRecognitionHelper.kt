package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Robust Speech Recognition Helper with Dynamic Voice Activity Detection (VAD).
 * Features:
 * - 1.2s dynamic silence threshold so turns trigger naturally without long delays
 * - Continuous self-healing auto-restart mechanism that eliminates deadlocks
 * - Prevents SpeechRecognizer busy errors via serialized main-thread lifecycle
 */
class SpeechRecognitionHelper(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _isAvailable = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var currentSilenceTimeoutMs: Long = 1200L
    private var continuousMode: Boolean = false

    // Dynamic VAD timer: triggers dispatch exactly after silence threshold
    private var vadSilenceRunnable: Runnable? = null
    private var isDispatching = false

    fun startListening(
        silenceTimeoutMs: Long = 1200L,
        continuous: Boolean = true,
        onResult: (String) -> Unit
    ) {
        currentSilenceTimeoutMs = silenceTimeoutMs
        continuousMode = continuous
        onFinalResultCallback = onResult
        _currentText.value = ""
        isDispatching = false
        cancelVadTimer()

        mainHandler.post {
            safelyStartInternal(silenceTimeoutMs)
        }
    }

    private fun cancelVadTimer() {
        vadSilenceRunnable?.let { mainHandler.removeCallbacks(it) }
        vadSilenceRunnable = null
    }

    private fun scheduleVadSilenceDispatch(delayMs: Long) {
        cancelVadTimer()
        vadSilenceRunnable = Runnable {
            val text = _currentText.value.trim()
            if (text.isNotBlank() && !isDispatching) {
                isDispatching = true
                Log.d("SpeechRecognitionHelper", "Dynamic VAD silence threshold reached. Sending speech turn: $text")
                dispatchFinalResult(text)
            }
        }
        mainHandler.postDelayed(vadSilenceRunnable!!, delayMs)
    }

    private fun safelyStartInternal(silenceTimeoutMs: Long) {
        try {
            cleanupRecognizer()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                        cancelVadTimer()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDb.value = rmsdB.coerceIn(0f, 10f)
                        // If user is actively producing sound, reset dynamic VAD timer
                        if (rmsdB > 2.5f) {
                            cancelVadTimer()
                        } else if (_currentText.value.isNotBlank() && vadSilenceRunnable == null) {
                            // User paused after saying something; trigger silence countdown
                            scheduleVadSilenceDispatch(silenceTimeoutMs)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        // Once speech ends, schedule dispatch within 300ms if not already triggered
                        if (_currentText.value.isNotBlank() && !isDispatching) {
                            scheduleVadSilenceDispatch(300L)
                        }
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Recognition error: $error"
                        }
                        Log.d("SpeechRecognitionHelper", "Status: $message (code $error)")

                        val fallback = _currentText.value.trim()
                        if (fallback.isNotBlank() && !isDispatching) {
                            isDispatching = true
                            dispatchFinalResult(fallback)
                        } else if (continuousMode && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_CLIENT)) {
                            // Self-healing continuous listener: auto-restart immediately without dropping mic
                            mainHandler.postDelayed({
                                if (continuousMode && !_isListening.value && !isDispatching) {
                                    _currentText.value = ""
                                    safelyStartInternal(currentSilenceTimeoutMs)
                                }
                            }, 250L)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: _currentText.value
                        if (text.isNotBlank() && !isDispatching) {
                            isDispatching = true
                            _currentText.value = text
                            dispatchFinalResult(text)
                        } else if (continuousMode) {
                            // If blank results received in continuous mode, restart loop
                            mainHandler.postDelayed({
                                if (continuousMode && !_isListening.value && !isDispatching) {
                                    _currentText.value = ""
                                    safelyStartInternal(currentSilenceTimeoutMs)
                                }
                            }, 250L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            _currentText.value = text
                            // User said something new; restart dynamic VAD timer
                            scheduleVadSilenceDispatch(silenceTimeoutMs)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // 1.2s dynamic silence threshold as requested by user
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeoutMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeoutMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognitionHelper", "Failed to start speech recognition", e)
            _isListening.value = false
        }
    }

    private fun dispatchFinalResult(text: String) {
        cancelVadTimer()
        cleanupRecognizer()
        onFinalResultCallback?.invoke(text)
    }

    /**
     * Force immediate completion of current spoken turn (Tap-to-Send ⚡).
     */
    fun completeSpeechNow() {
        val text = _currentText.value.trim()
        if (text.isNotBlank() && !isDispatching) {
            isDispatching = true
            dispatchFinalResult(text)
        } else {
            stopListening()
        }
    }

    fun stopListening() {
        continuousMode = false
        cancelVadTimer()
        mainHandler.post {
            cleanupRecognizer()
        }
    }

    private fun cleanupRecognizer() {
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
    }

    fun shutdown() {
        stopListening()
    }
}
