package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognitionHelper(private val context: Context) {

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

    fun startListening(
        silenceTimeoutMs: Long = 650L,
        onResult: (String) -> Unit
    ) {
        onFinalResultCallback = onResult
        _currentText.value = ""

        try {
            stopListening()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDb.value = rmsdB.coerceIn(0f, 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
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
                        Log.w("SpeechRecognitionHelper", message)
                        // If we captured partial text before an error or timeout, deliver it!
                        val fallback = _currentText.value.trim()
                        if (fallback.isNotBlank()) {
                            onFinalResultCallback?.invoke(fallback)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: _currentText.value
                        if (text.isNotBlank()) {
                            _currentText.value = text
                            onFinalResultCallback?.invoke(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            _currentText.value = text
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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeoutMs.coerceAtMost(550L))
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognitionHelper", "Failed to start speech recognition", e)
            _isListening.value = false
        }
    }

    /**
     * Force immediate completion of the current spoken turn (Zero waiting for silence).
     */
    fun completeSpeechNow() {
        val text = _currentText.value.trim()
        stopListening()
        if (text.isNotBlank()) {
            onFinalResultCallback?.invoke(text)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w("SpeechRecognitionHelper", "Error stopping speech recognizer", e)
        }
        _isListening.value = false
        _rmsDb.value = 0f
    }

    fun shutdown() {
        stopListening()
    }
}
