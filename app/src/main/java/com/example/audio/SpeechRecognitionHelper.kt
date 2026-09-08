package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Robust Speech Recognition Helper with Always-On Continuous Listening (Gemini Live Style).
 * Features:
 * - Dynamic Voice Activity Detection (VAD) with silence countdown
 * - Continuous self-healing auto-restart mechanism that never drops the mic
 * - Graceful permission checking & error recovery
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
    private var continuousMode: Boolean = true

    // Dynamic VAD timer: triggers dispatch exactly after silence threshold
    private var vadSilenceRunnable: Runnable? = null
    private var isDispatching = false

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

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
                Log.d("SpeechRecognitionHelper", "Silence reached. Auto-sending: $text")
                dispatchFinalResult(text)
            }
        }
        mainHandler.postDelayed(vadSilenceRunnable!!, delayMs)
    }

    private fun safelyStartInternal(silenceTimeoutMs: Long) {
        if (!hasRecordPermission()) {
            Log.w("SpeechRecognitionHelper", "RECORD_AUDIO permission not granted")
            _isListening.value = false
            return
        }

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
                        if (rmsdB > 2.2f) {
                            cancelVadTimer()
                        } else if (_currentText.value.isNotBlank() && vadSilenceRunnable == null) {
                            // User paused after saying something; trigger silence countdown
                            scheduleVadSilenceDispatch(silenceTimeoutMs)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // Once speech pauses, schedule dispatch within 350ms if speech was detected
                        if (_currentText.value.isNotBlank() && !isDispatching) {
                            scheduleVadSilenceDispatch(350L)
                        }
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Recognition code: $error"
                        }
                        Log.d("SpeechRecognitionHelper", "Status: $message (code $error)")

                        val fallback = _currentText.value.trim()
                        if (fallback.isNotBlank() && !isDispatching) {
                            isDispatching = true
                            dispatchFinalResult(fallback)
                        } else if (continuousMode && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            // Self-healing continuous listener: restart loop smoothly
                            mainHandler.postDelayed({
                                if (continuousMode && !isDispatching) {
                                    _currentText.value = ""
                                    safelyStartInternal(currentSilenceTimeoutMs)
                                }
                            }, 300L)
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
                            // Restart loop seamlessly if continuous
                            mainHandler.postDelayed({
                                if (continuousMode && !isDispatching) {
                                    _currentText.value = ""
                                    safelyStartInternal(currentSilenceTimeoutMs)
                                }
                            }, 200L)
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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeoutMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeoutMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 250L)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
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
