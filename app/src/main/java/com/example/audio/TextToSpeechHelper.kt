package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val completionCallbacks = mutableMapOf<String, () -> Unit>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f) // Optimized for language learning clarity
                tts?.setPitch(1.0f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        utteranceId?.let { id ->
                            completionCallbacks.remove(id)?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        utteranceId?.let { id ->
                            completionCallbacks.remove(id)
                        }
                    }
                })
            } else {
                Log.e("TextToSpeechHelper", "TTS Initialization failed: status $status")
            }
        }
    }

    fun setVoiceProfile(locale: Locale = Locale.US, speechRate: Float = 0.95f, pitch: Float = 1.0f) {
        if (!isInitialized || tts == null) return
        try {
            tts?.language = locale
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(pitch)
        } catch (e: Exception) {
            Log.w("TextToSpeechHelper", "Failed setting voice profile", e)
        }
    }

    fun speak(text: String, utteranceId: String = "airspeak_tts", onDone: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }
        stop()
        if (onDone != null) {
            completionCallbacks[utteranceId] = onDone
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized && tts != null) {
            completionCallbacks.clear()
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
