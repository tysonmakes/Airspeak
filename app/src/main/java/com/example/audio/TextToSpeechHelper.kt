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
                // Relaxed, natural conversational human pace (0.88f) instead of fast robotic 0.95f
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(1.02f)

                // Select the highest quality natural human voice available on the device
                try {
                    val voices = tts?.voices
                    if (!voices.isNullOrEmpty()) {
                        // Look for natural, high-quality, non-network latency voices
                        val bestVoice = voices.find { voice ->
                            voice.locale.language == Locale.US.language &&
                            !voice.isNetworkConnectionRequired &&
                            (voice.quality >= android.speech.tts.Voice.QUALITY_HIGH || voice.name.contains("en-us-x", ignoreCase = true))
                        } ?: voices.find { it.locale == Locale.US }
                        if (bestVoice != null) {
                            tts?.voice = bestVoice
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TextToSpeechHelper", "Default voice used: ${e.message}")
                }

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

    fun setSpeechRate(rate: Float) {
        if (!isInitialized || tts == null) return
        try {
            tts?.setSpeechRate(rate.coerceIn(0.6f, 1.5f))
        } catch (e: Exception) {
            Log.w("TextToSpeechHelper", "Failed setting speech rate", e)
        }
    }

    fun setVoiceProfile(locale: Locale = Locale.US, speechRate: Float = 0.88f, pitch: Float = 1.0f) {
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
