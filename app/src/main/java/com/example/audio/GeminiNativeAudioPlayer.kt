package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance Direct Audio Player for Gemini Native Voice
 * Supports direct raw PCM byte streaming (via AudioTrack) and MP3/WAV playback (via MediaPlayer)
 * Achieving sub-second zero-transcoding latency.
 */
class GeminiNativeAudioPlayer(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    fun playNativeAudio(
        audioBytes: ByteArray,
        mimeType: String? = null,
        onComplete: () -> Unit
    ) {
        stop()
        _isPlaying.value = true

        coroutineScope.launch {
            try {
                // If it's raw PCM audio (typical from Gemini 24000Hz or 16000Hz)
                if (mimeType?.contains("pcm", ignoreCase = true) == true || (mimeType == null && isLikelyPcm(audioBytes))) {
                    playPcmBytes(audioBytes, sampleRate = extractSampleRate(mimeType), onComplete = {
                        _isPlaying.value = false
                        onComplete()
                    })
                } else {
                    // MP3, AAC, or WAV format
                    playEncodedBytes(audioBytes, onComplete = {
                        _isPlaying.value = false
                        onComplete()
                    })
                }
            } catch (e: Exception) {
                Log.e("GeminiNativeAudioPlayer", "Error playing native audio: ${e.message}", e)
                _isPlaying.value = false
                onComplete()
            }
        }
    }

    private suspend fun playPcmBytes(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        onComplete: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufSize, pcmData.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack = track
            track.write(pcmData, 0, pcmData.size)
            track.play()

            // Calculate playback duration in ms
            val durationMs = (pcmData.size.toDouble() / (sampleRate * 2) * 1000).toLong()
            kotlinx.coroutines.delay(durationMs + 100)

            track.stop()
            track.release()
            audioTrack = null

            withContext(Dispatchers.Main) {
                onComplete()
            }
        } catch (e: Exception) {
            Log.e("GeminiNativeAudioPlayer", "PCM playback failed, falling back to temp file", e)
            playEncodedBytes(pcmData, onComplete)
        }
    }

    private suspend fun playEncodedBytes(
        audioBytes: ByteArray,
        onComplete: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "gemini_voice_temp_${System.currentTimeMillis()}.mp3")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    setOnCompletionListener {
                        _isPlaying.value = false
                        it.release()
                        mediaPlayer = null
                        tempFile.delete()
                        onComplete()
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlaying.value = false
                        tempFile.delete()
                        onComplete()
                        true
                    }
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiNativeAudioPlayer", "Encoded playback error", e)
            withContext(Dispatchers.Main) {
                _isPlaying.value = false
                onComplete()
            }
        }
    }

    private fun extractSampleRate(mimeType: String?): Int {
        if (mimeType == null) return 24000
        val regex = "rate=(\\d+)".toRegex()
        val match = regex.find(mimeType)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 24000
    }

    private fun isLikelyPcm(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // Check if starts with ID3 (MP3 header) or RIFF (WAV header)
        val isMp3 = bytes[0] == 0x49.toByte() && bytes[1] == 0x44.toByte() && bytes[2] == 0x33.toByte()
        val isWav = bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()
        return !isMp3 && !isWav
    }

    fun stop() {
        try {
            streamingJob?.cancel()
            streamingJob = null
            streamingAudioTrack?.apply {
                stop()
                release()
            }
            streamingAudioTrack = null
        } catch (e: Exception) {
            Log.w("GeminiNativeAudioPlayer", "Error stopping streamingAudioTrack: ${e.message}")
        }

        try {
            audioTrack?.apply {
                stop()
                release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.w("GeminiNativeAudioPlayer", "Error stopping audioTrack: ${e.message}")
        }

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w("GeminiNativeAudioPlayer", "Error stopping mediaPlayer: ${e.message}")
        }

        _isPlaying.value = false
    }

    private var streamingAudioTrack: AudioTrack? = null
    private var streamingSampleRate: Int = 24000
    private var streamingJob: Job? = null

    /**
     * Initializes streaming mode for incoming incremental audio PCM chunks (e.g. from Gemini Live API)
     */
    fun startStreamingMode(sampleRate: Int = 24000) {
        stop()
        _isPlaying.value = true
        streamingSampleRate = sampleRate
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufSize * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            streamingAudioTrack = track
            track.play()
        } catch (e: Exception) {
            Log.e("GeminiNativeAudioPlayer", "Failed to init streaming AudioTrack", e)
        }
    }

    /**
     * Writes an incoming chunk of PCM audio directly into the streaming AudioTrack for instant playback.
     */
    fun writeStreamingChunk(pcmBytes: ByteArray) {
        try {
            if (streamingAudioTrack == null) {
                startStreamingMode(streamingSampleRate)
            }
            streamingAudioTrack?.write(pcmBytes, 0, pcmBytes.size)
        } catch (e: Exception) {
            Log.e("GeminiNativeAudioPlayer", "Error writing audio chunk", e)
        }
    }

    /**
     * Completes the streaming playback session once all chunks have finished playing.
     */
    fun finishStreamingMode(onComplete: () -> Unit) {
        streamingJob?.cancel()
        streamingJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                // Wait for audio track buffer to finish playing
                val track = streamingAudioTrack
                if (track != null) {
                    kotlinx.coroutines.delay(250)
                    track.stop()
                    track.release()
                    streamingAudioTrack = null
                }
            } catch (e: Exception) {
                Log.w("GeminiNativeAudioPlayer", "Error finishing stream track: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isPlaying.value = false
                    onComplete()
                }
            }
        }
    }

    fun release() {
        stop()
    }
}
