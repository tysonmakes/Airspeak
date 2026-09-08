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

    private val edgeTts = EdgeTtsHelper(context)
    private var localTts: TextToSpeech? = null
    private var isLocalInitialized = false

    val isSpeaking: StateFlow<Boolean> = edgeTts.isSpeaking

    private var currentSpeechRate: Float = 1.0f
    private var currentVoiceName: String = "en-US-AnaNeural"

    init {
        edgeTts.voiceName = currentVoiceName
        edgeTts.speechRate = currentSpeechRate

        try {
            localTts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    localTts?.language = Locale.US
                    localTts?.setSpeechRate(currentSpeechRate)
                    isLocalInitialized = true
                }
            }
        } catch (e: Exception) {
            Log.w("TextToSpeechHelper", "Local TTS init note: ${e.message}")
        }
    }

    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.7f, 1.5f)
        edgeTts.speechRate = currentSpeechRate
        localTts?.setSpeechRate(currentSpeechRate)
    }

    fun setVoiceName(voice: String) {
        currentVoiceName = voice
        edgeTts.voiceName = voice
    }

    fun setVoiceProfile(locale: Locale = Locale.US, speechRate: Float = 1.0f, pitch: Float = 1.0f) {
        currentSpeechRate = speechRate
        edgeTts.speechRate = speechRate
        edgeTts.pitch = pitch
        localTts?.language = locale
        localTts?.setSpeechRate(speechRate)
        localTts?.setPitch(pitch)
    }

    fun speak(text: String, utteranceId: String = "airspeak_tts", onDone: (() -> Unit)? = null) {
        val clean = text.trim()
        if (clean.isBlank()) {
            onDone?.invoke()
            return
        }

        // Play via Microsoft Edge Neural Voice (0.9x pacing) with automatic fallback
        edgeTts.speak(clean, utteranceId = utteranceId, onDone = onDone)
    }

    fun prefetch(text: String) {
        edgeTts.prefetch(text)
    }

    fun stop() {
        edgeTts.stop()
        localTts?.stop()
    }

    fun shutdown() {
        stop()
        localTts?.shutdown()
        localTts = null
        isLocalInitialized = false
    }
}
