package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GeminiNativeAudioPlayer
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.TextToSpeechHelper
import com.example.data.local.CallSender
import com.example.data.local.LiveCallCorrectionItem
import com.example.data.local.LiveCallSummaryReport
import com.example.data.local.LiveCallTranscriptItem
import com.example.data.local.LiveCallTutor
import com.example.data.local.TutorCatalog
import com.example.data.local.entity.WeaknessItem
import com.example.data.remote.AiEngine
import com.example.data.remote.AiEngineManager
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import com.example.data.remote.PollinationsApiService
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.components.AiEngineSelectionDialog
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class CallState {
    IDLE_LOBBY,
    CONNECTING,
    ACTIVE,
    REPORT_DIALOG
}

@Composable
fun LiveCallScreen(
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onNavigateToWeaknessLog: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pollinations = remember { PollinationsApiService() }
    val aiEngineManager = remember { AiEngineManager(context) }
    val geminiNativePlayer = remember { GeminiNativeAudioPlayer(context) }
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val currentEngine by aiEngineManager.currentEngine.collectAsState()
    var showEngineSelectorDialog by remember { mutableStateOf(false) }
    var lastTurnLatencyMs by remember { mutableLongStateOf(0L) }
    var lastFallbackNotice by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Microphone enabled for Live Voice Call!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission needed for voice. You can also use Keyboard.", Toast.LENGTH_LONG).show()
        }
    }

    var selectedTutor by remember { mutableStateOf(TutorCatalog.tutors.first()) }
    var selectedTopic by remember { mutableStateOf(selectedTutor.defaultTopics.first()) }
    var callState by remember { mutableStateOf(CallState.IDLE_LOBBY) }

    // Live Call Session State
    var callDurationSeconds by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var showSubtitles by remember { mutableStateOf(true) }
    var showHintsSheet by remember { mutableStateOf(false) }
    var showKeyboardInputDialog by remember { mutableStateOf(false) }
    var typedTurnInput by remember { mutableStateOf("") }
    var isAiSpeaking by remember { mutableStateOf(false) }
    var isAiThinking by remember { mutableStateOf(false) }

    // Dynamic metrics & coach prompts
    var activeCoachTip by remember { mutableStateOf<String?>(null) }
    var activeCorrection by remember { mutableStateOf<LiveCallCorrectionItem?>(null) }
    var latestPraise by remember { mutableStateOf<String?>(null) }
    var totalWordsSpoken by remember { mutableIntStateOf(0) }
    var fillerWordsCount by remember { mutableIntStateOf(0) }

    val transcriptItems = remember { mutableStateListOf<LiveCallTranscriptItem>() }
    val sessionCorrections = remember { mutableStateListOf<LiveCallCorrectionItem>() }
    var finalReport by remember { mutableStateOf<LiveCallSummaryReport?>(null) }

    val isListening by speechHelper.isListening.collectAsState()
    val currentSpeechText by speechHelper.currentText.collectAsState()
    val rmsDb by speechHelper.rmsDb.collectAsState()

    // Timer coroutine during active call
    LaunchedEffect(callState) {
        if (callState == CallState.ACTIVE) {
            callDurationSeconds = 0L
            while (callState == CallState.ACTIVE) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    // Helper: Turn processing
    fun processUserTurn(userText: String) {
        if (userText.isBlank()) return
        val clean = userText.trim()

        // If AI was speaking, immediately interrupt and stop playback
        
        geminiNativePlayer.stop()
        isAiSpeaking = false

        val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        totalWordsSpoken += words.size

        // Count filler words
        val fillers = listOf("um", "uh", "like", "actually", "basically", "you know")
        for (f in fillers) {
            val count = Regex("\\b$f\\b", RegexOption.IGNORE_CASE).findAll(clean).count()
            fillerWordsCount += count
        }

        transcriptItems.add(
            LiveCallTranscriptItem(
                sender = CallSender.USER,
                text = clean
            )
        )

        isAiThinking = true

        coroutineScope.launch {
            val history = transcriptItems.takeLast(6).joinToString("\n") {
                "${it.sender.name}: ${it.text}"
            }

            // Direct Gemini Native Voice (sub-second audio streaming)
            val systemPrompt = """
                You are ${selectedTutor.name}, ${selectedTutor.bio}. You are speaking in an interactive English speaking call with a learner discussing '${selectedTopic}'.
                Respond warmly, naturally in 1-2 spoken sentences. Give gentle feedback or keep the dialogue flowing.
                IMPORTANT: If the user makes a grammatical or fluency mistake, include "*Correction:* [your correction]" at the end of your text response.
                If they say something exceptionally well, include "*Praise:* [your praise]" at the end.
                Do NOT speak the correction or praise out loud in your audio if possible, but include it in the text.
            """.trimIndent()
            val conversationHistory = transcriptItems.takeLast(4).map {
                GeminiContent(
                    role = if (it.sender == CallSender.USER) "user" else "model",
                    parts = listOf(GeminiPart(text = it.text))
                )
            }

            val nativeResult = if (GeminiClient.hasValidApiKey()) {
                GeminiClient.queryGeminiNativeVoiceTurn(
                    userText = clean,
                    systemInstruction = systemPrompt,
                    voiceName = selectedTutor.geminiVoiceName,
                    conversationHistory = conversationHistory
                )
            } else null

            if (nativeResult != null && nativeResult.audioBytes != null && nativeResult.audioBytes.isNotEmpty()) {
                // Direct Gemini Native Voice Success!
                isAiThinking = false
                lastTurnLatencyMs = nativeResult.latencyMs
                lastFallbackNotice = null

                val spokenReply = nativeResult.spokenText.ifBlank { "That's great! Let's continue talking about this." }
                
                // Parse potential coach tips from the text if Gemini included them (e.g. *Correction:* or *Praise:*)
                var cleanReply = spokenReply
                var correction: String? = null
                var praise: String? = null
                
                if (spokenReply.contains("*Correction:*")) {
                    correction = spokenReply.substringAfter("*Correction:*").substringBefore("*Praise:*").substringBefore("\n").trim()
                    cleanReply = cleanReply.replace("*Correction:* $correction", "").trim()
                }
                if (spokenReply.contains("*Praise:*")) {
                    praise = spokenReply.substringAfter("*Praise:*").substringBefore("\n").trim()
                    cleanReply = cleanReply.replace("*Praise:* $praise", "").trim()
                }

                if (!correction.isNullOrBlank()) {
                    val corrItem = LiveCallCorrectionItem(
                        originalSaid = clean,
                        correctedVersion = correction,
                        reason = "Live syntax / fluency correction"
                    )
                    activeCorrection = corrItem
                    sessionCorrections.add(corrItem)
                } else {
                    activeCorrection = null
                }

                latestPraise = praise
                activeCoachTip = if (!correction.isNullOrBlank()) "💡 $correction" else praise

                transcriptItems.add(
                    LiveCallTranscriptItem(
                        sender = CallSender.AI,
                        text = cleanReply,
                        liveCorrection = correction,
                        livePraise = praise
                    )
                )

                isAiSpeaking = true
                geminiNativePlayer.playNativeAudio(nativeResult.audioBytes, nativeResult.audioMimeType) {
                    isAiSpeaking = false
                    if (callState == CallState.ACTIVE && !isMuted) {
                        speechHelper.startListening(silenceTimeoutMs = 1100L, continuous = true) { nextSpeech ->
                            processUserTurn(nextSpeech)
                        }
                    }
                }
            } else {
                isAiThinking = false
                val convHist = transcriptItems.takeLast(4).joinToString("\n") { "${it.sender}: ${it.text}" }
                val fallbackTurn = aiEngineManager.generateLiveCallTurn(
                    tutorName = selectedTutor.name,
                    tutorPersona = selectedTutor.bio,
                    userSpokenText = clean,
                    callTopic = selectedTopic,
                    conversationHistory = convHist
                )

                val rawReply = fallbackTurn.spokenReply.ifBlank {
                    "That's very interesting! Could you tell me a little bit more about that?"
                }
                val cleanReplyText = rawReply
                    .replace(Regex("""^(\*{0,2}\(?[A-Za-z0-9_\- ]+\)?\*{0,2}:?\s*)+"""), "")
                    .replace(Regex("""^\*+|\*+$"""), "")
                    .replace(Regex("""^\(.*?\)\s*"""), "")
                    .trim()
                    .ifBlank { "That sounds great! Could you share more thoughts on this?" }

                val correction = fallbackTurn.liveCorrection
                val praise = fallbackTurn.livePraise

                if (!correction.isNullOrBlank()) {
                    val corrItem = LiveCallCorrectionItem(
                        originalSaid = clean,
                        correctedVersion = correction,
                        reason = "Syntax & fluency coaching"
                    )
                    activeCorrection = corrItem
                    sessionCorrections.add(corrItem)
                }
                latestPraise = praise
                activeCoachTip = if (!correction.isNullOrBlank()) "💡 $correction" else praise

                transcriptItems.add(
                    LiveCallTranscriptItem(
                        sender = CallSender.AI,
                        text = cleanReplyText,
                        liveCorrection = correction,
                        livePraise = praise
                    )
                )

                isAiSpeaking = true
                ttsHelper.setVoiceName(selectedTutor.edgeVoiceName)
                ttsHelper.speak(cleanReplyText) {
                    isAiSpeaking = false
                    if (callState == CallState.ACTIVE && !isMuted) {
                        speechHelper.startListening(silenceTimeoutMs = 1100L, continuous = true) { nextSpeech ->
                            processUserTurn(nextSpeech)
                        }
                    }
                }
            }
        }
    }

    fun startCall() {
        if (!speechHelper.hasRecordPermission()) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        callState = CallState.CONNECTING
        transcriptItems.clear()
        sessionCorrections.clear()
        totalWordsSpoken = 0
        fillerWordsCount = 0
        activeCorrection = null
        activeCoachTip = null
        latestPraise = null
        lastFallbackNotice = null
        lastTurnLatencyMs = 0L

        coroutineScope.launch {
            delay(1000) // Fast realistic ringing / connection hand-shake
            callState = CallState.ACTIVE

            // Tutor delivers greeting
            val greeting = selectedTutor.greeting
            transcriptItems.add(
                LiveCallTranscriptItem(
                    sender = CallSender.AI,
                    text = greeting
                )
            )

            // Try Direct Native Voice for greeting
            val nativeGreeting = if (GeminiClient.hasValidApiKey()) {
                GeminiClient.queryGeminiNativeVoiceTurn(
                    userText = "Introduce yourself and say hello to start the call.",
                    systemInstruction = "You are ${selectedTutor.name}. Greet the user with: '${greeting}' in a natural spoken conversational voice.",
                    voiceName = selectedTutor.geminiVoiceName
                )
            } else null

            var greetingFinished = false
            val startUserListening = {
                if (!greetingFinished) {
                    greetingFinished = true
                    isAiSpeaking = false
                    if (callState == CallState.ACTIVE && !isMuted) {
                        speechHelper.startListening(silenceTimeoutMs = 1100L, continuous = true) { spoken ->
                            processUserTurn(spoken)
                        }
                    }
                }
            }

            if (nativeGreeting?.audioBytes != null && nativeGreeting.audioBytes.isNotEmpty()) {
                isAiSpeaking = true
                geminiNativePlayer.playNativeAudio(nativeGreeting.audioBytes, nativeGreeting.audioMimeType) {
                    startUserListening()
                }
            } else {
                isAiSpeaking = true
                ttsHelper.setVoiceName(selectedTutor.edgeVoiceName)
                ttsHelper.speak(greeting) {
                    startUserListening()
                }
            }

            // Safety guard: Ensure speech recognition automatically starts listening within 6 seconds max even if audio engine stalls
            delay(6000)
            if (!greetingFinished && callState == CallState.ACTIVE) {
                startUserListening()
            }
        }
    }

    fun endCall() {
        speechHelper.stopListening()
        geminiNativePlayer.stop()
        ttsHelper.stop()
        
        isAiSpeaking = false
        isAiThinking = false

        // Compute report metrics
        val durationMins = (callDurationSeconds.coerceAtLeast(1) / 60.0).coerceAtLeast(0.3)
        val computedWpm = (totalWordsSpoken / durationMins).toInt().coerceIn(60, 190)
        val score = (85 + (totalWordsSpoken.coerceAtMost(80) / 10) - (sessionCorrections.size * 3) - (fillerWordsCount * 2)).coerceIn(65, 96)
        val band = when {
            score >= 90 -> "C1 (Advanced Fluent)"
            score >= 80 -> "B2 (High Intermediate)"
            score >= 70 -> "B1 (Intermediate)"
            else -> "A2 (Developing)"
        }

        finalReport = LiveCallSummaryReport(
            tutorName = selectedTutor.name,
            tutorRole = selectedTutor.roleTitle,
            topic = selectedTopic,
            callDurationSeconds = callDurationSeconds,
            turnsExchanged = transcriptItems.size,
            averageWpm = computedWpm,
            overallFluencyScore = score,
            cefrBand = band,
            fillerWordsCount = fillerWordsCount,
            correctionsCount = sessionCorrections.size,
            correctionsList = sessionCorrections.toList(),
            highlights = listOf(
                "Kept active speaking dialogue for ${formatSeconds(callDurationSeconds)}",
                "Spoke $totalWordsSpoken words at ~$computedWpm WPM conversational pace",
                if (fillerWordsCount <= 2) "Clean phrasing with minimal filler pauses" else "Used $fillerWordsCount filler words (um/uh/like)"
            ),
            finalCoachAdvice = "${selectedTutor.name} says: 'You did a fantastic job expressing yourself openly. Focus on prepositions and past tense consistency in your next session!'"
        )

        callState = CallState.REPORT_DIALOG
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (callState) {
            CallState.IDLE_LOBBY -> {
                LobbyView(
                    tutors = TutorCatalog.tutors,
                    selectedTutor = selectedTutor,
                    onSelectTutor = {
                        selectedTutor = it
                        selectedTopic = it.defaultTopics.first()
                    },
                    selectedTopic = selectedTopic,
                    onSelectTopic = { selectedTopic = it },
                    currentEngine = currentEngine,
                    onOpenEngineSelector = { showEngineSelectorDialog = true },
                    onStartCall = { startCall() }
                )
            }
            CallState.CONNECTING -> {
                ConnectingView(
                    tutor = selectedTutor,
                    topic = selectedTopic,
                    onCancel = {
                        callState = CallState.IDLE_LOBBY
                        geminiNativePlayer.stop()
                        
                    }
                )
            }
            CallState.ACTIVE -> {
                ActiveCallView(
                    tutor = selectedTutor,
                    topic = selectedTopic,
                    callDurationSeconds = callDurationSeconds,
                    isAiSpeaking = isAiSpeaking,
                    isAiThinking = isAiThinking,
                    isListening = isListening,
                    isMuted = isMuted,
                    currentSpeechText = currentSpeechText,
                    rmsDb = rmsDb,
                    activeCoachTip = activeCoachTip,
                    activeCorrection = activeCorrection,
                    transcriptItems = transcriptItems,
                    showSubtitles = showSubtitles,
                    currentEngine = currentEngine,
                    lastTurnLatencyMs = lastTurnLatencyMs,
                    lastFallbackNotice = lastFallbackNotice,
                    onOpenEngineSelector = { showEngineSelectorDialog = true },
                    onCompleteSpeechNow = { speechHelper.completeSpeechNow() },
                    onInterruptTutor = {
                        geminiNativePlayer.stop()
                        
                        isAiSpeaking = false
                        if (!isMuted) {
                            speechHelper.startListening(silenceTimeoutMs = 1100L, continuous = true) { spoken ->
                                processUserTurn(spoken)
                            }
                        }
                    },
                    onToggleSubtitles = { showSubtitles = !showSubtitles },
                    onToggleMute = {
                        isMuted = !isMuted
                        if (isMuted) {
                            speechHelper.stopListening()
                        } else if (!isAiSpeaking && !isAiThinking) {
                            speechHelper.startListening(silenceTimeoutMs = 1100L, continuous = true) { spoken ->
                                processUserTurn(spoken)
                            }
                        }
                    },
                    onTriggerHint = { showHintsSheet = true },
                    onOpenKeyboardInput = { showKeyboardInputDialog = true },
                    onRepeatTutor = {
                        val lastAiMsg = transcriptItems.lastOrNull { it.sender == CallSender.AI }?.text
                        if (!lastAiMsg.isNullOrBlank()) {
                            isAiSpeaking = true
                            coroutineScope.launch {
                                val repeatAudio = if (GeminiClient.hasValidApiKey()) {
                                     GeminiClient.queryGeminiNativeVoiceTurn(
                                         userText = "Read this text out loud exactly: '$lastAiMsg'",
                                         systemInstruction = "You are an AI voice synthesis engine. Do not answer questions or chat. Read the provided text exactly as it is written.",
                                         voiceName = selectedTutor.geminiVoiceName
                                     )
                                } else null
            
                                if (repeatAudio?.audioBytes != null && repeatAudio.audioBytes.isNotEmpty()) {
                                    geminiNativePlayer.playNativeAudio(repeatAudio.audioBytes, repeatAudio.audioMimeType) {
                                        isAiSpeaking = false
                                    }
                                } else {
                                    isAiSpeaking = false
                                    Toast.makeText(context, "Voice playback unavailable.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onSaveCorrection = { corr ->
                        coroutineScope.launch {
                            repository.insertWeakness(
                                WeaknessItem(
                                    category = "Live Call Grammar",
                                    userSaid = corr.originalSaid,
                                    correction = corr.correctedVersion,
                                    explanation = corr.reason,
                                    sourceSession = "Live Call with ${selectedTutor.name}"
                                )
                            )
                            Toast.makeText(context, "Saved to Mistake Bank!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEndCall = { endCall() }
                )
            }
            CallState.REPORT_DIALOG -> {
                finalReport?.let { report ->
                    PostCallReportDialog(
                        report = report,
                        onDismiss = {
                            callState = CallState.IDLE_LOBBY
                        },
                        onSaveAllCorrections = {
                            coroutineScope.launch {
                                for (corr in report.correctionsList) {
                                    repository.insertWeakness(
                                        WeaknessItem(
                                            category = "Live Call Grammar",
                                            userSaid = corr.originalSaid,
                                            correction = corr.correctedVersion,
                                            explanation = corr.reason,
                                            sourceSession = "Live Call with ${report.tutorName}"
                                        )
                                    )
                                }
                                Toast.makeText(context, "Saved ${report.correctionsList.size} corrections to Mistake Bank!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToMistakes = {
                            callState = CallState.IDLE_LOBBY
                            onNavigateToWeaknessLog()
                        }
                    )
                }
            }
        }

        // Live Call Keyboard Input Fallback Dialog
        if (showKeyboardInputDialog) {
            AlertDialog(
                onDismissRequest = { showKeyboardInputDialog = false },
                title = {
                    Text(
                        text = "Type to ${selectedTutor.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Speak or type in English — ${selectedTutor.name} will answer with live spoken voice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = typedTurnInput,
                            onValueChange = { typedTurnInput = it },
                            placeholder = { Text("Type what you want to say in English...", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_call_text_input"),
                            singleLine = false,
                            maxLines = 4
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val text = typedTurnInput.trim()
                            if (text.isNotBlank()) {
                                showKeyboardInputDialog = false
                                typedTurnInput = ""
                                processUserTurn(text)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send & Speak")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showKeyboardInputDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1B1D28)
            )
        }

        // Hints Dialog
        if (showHintsSheet) {
            QuickHintsDialog(
                topic = selectedTopic,
                onSelectHint = { hintText ->
                    showHintsSheet = false
                    processUserTurn(hintText)
                },
                onDismiss = { showHintsSheet = false }
            )
        }

        // AI Engine Selection Dialog
        if (showEngineSelectorDialog) {
            AiEngineSelectionDialog(
                currentEngine = currentEngine,
                aiEngineManager = aiEngineManager,
                onSelectEngine = { engine ->
                    aiEngineManager.setEngine(engine)
                    showEngineSelectorDialog = false
                    Toast.makeText(context, "Switched to ${engine.displayName}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showEngineSelectorDialog = false }
            )
        }
    }
}

@Composable
private fun LobbyView(
    tutors: List<LiveCallTutor>,
    selectedTutor: LiveCallTutor,
    onSelectTutor: (LiveCallTutor) -> Unit,
    selectedTopic: String,
    onSelectTopic: (String) -> Unit,
    currentEngine: AiEngine,
    onOpenEngineSelector: () -> Unit,
    onStartCall: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("live_call_lobby"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Live AI Voice Call",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "REALTIME",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                        Text(
                            text = "Practice speaking naturally with native AI coaches. Receive real-time pronunciation & grammar corrections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            // AI Engine Switcher & Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenEngineSelector() }
                    .testTag("ai_engine_selector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Active AI: ${currentEngine.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentEngine.speedTier} • ${currentEngine.badge} • Tap to switch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Change",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Choose Your Native Speaking Coach",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Tutor Cards List
        items(tutors) { tutor ->
            val isSelected = tutor.id == selectedTutor.id
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTutor(tutor) }
                    .testTag("tutor_card_${tutor.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = tutor.avatarRes),
                        contentDescription = tutor.name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tutor.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "${tutor.roleTitle} • ${tutor.origin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tutor.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            tutor.specialties.take(2).forEach { spec ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = spec,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Select Call Topic",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedTutor.defaultTopics) { topic ->
                    val isChosen = topic == selectedTopic
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onSelectTopic(topic) }
                    ) {
                        Text(
                            text = topic,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStartCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_live_call_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Start Live Voice Call with ${selectedTutor.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConnectingView(
    tutor: LiveCallTutor,
    topic: String,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
            Image(
                painter = painterResource(id = tutor.avatarRes),
                contentDescription = tutor.name,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = tutor.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = tutor.roleTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) {
            Text(
                text = "Topic: $topic",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(EmeraldSuccess)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Connecting HD Audio Line...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(RoseError)
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Cancel Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ActiveCallView(
    tutor: LiveCallTutor,
    topic: String,
    callDurationSeconds: Long,
    isAiSpeaking: Boolean,
    isAiThinking: Boolean,
    isListening: Boolean,
    isMuted: Boolean,
    currentSpeechText: String,
    rmsDb: Float,
    activeCoachTip: String?,
    activeCorrection: LiveCallCorrectionItem?,
    transcriptItems: List<LiveCallTranscriptItem>,
    showSubtitles: Boolean,
    currentEngine: AiEngine,
    lastTurnLatencyMs: Long,
    lastFallbackNotice: String?,
    onOpenEngineSelector: () -> Unit,
    onCompleteSpeechNow: () -> Unit,
    onInterruptTutor: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleMute: () -> Unit,
    onTriggerHint: () -> Unit,
    onOpenKeyboardInput: () -> Unit,
    onRepeatTutor: () -> Unit,
    onSaveCorrection: (LiveCallCorrectionItem) -> Unit,
    onEndCall: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(transcriptItems.size) {
        if (transcriptItems.isNotEmpty()) {
            listState.animateScrollToItem(transcriptItems.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top In-Call Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = tutor.avatarRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tutor.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatSeconds(callDurationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = " • HD Voice",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Engine & Latency chip (tap to change AI engine anytime during call)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { onOpenEngineSelector() }
                        .testTag("in_call_engine_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (lastTurnLatencyMs in 1..800 || !currentEngine.isCloud) EmeraldSuccess else AmberTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lastTurnLatencyMs > 0) "${lastTurnLatencyMs}ms" else currentEngine.speedTier,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (lastTurnLatencyMs in 1..800 || !currentEngine.isCloud) EmeraldSuccess else AmberTertiary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Engine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                IconButton(onClick = onToggleSubtitles) {
                    Icon(
                        imageVector = if (showSubtitles) Icons.Filled.Subtitles else Icons.Outlined.Subtitles,
                        contentDescription = "Subtitles",
                        tint = if (showSubtitles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRepeatTutor) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Repeat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Active fallback notice if one occurred
        if (lastFallbackNotice != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberTertiary.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AmberTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lastFallbackNotice,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Voice Status Indicator Pill
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = when {
                isAiSpeaking -> MaterialTheme.colorScheme.primaryContainer
                isAiThinking -> MaterialTheme.colorScheme.tertiaryContainer
                isListening -> EmeraldSuccess.copy(alpha = 0.15f)
                isMuted -> RoseError.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isAiSpeaking -> Icons.Default.RecordVoiceOver
                        isAiThinking -> Icons.Default.Psychology
                        isListening -> Icons.Default.GraphicEq
                        isMuted -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isAiSpeaking -> MaterialTheme.colorScheme.primary
                        isAiThinking -> MaterialTheme.colorScheme.tertiary
                        isListening -> EmeraldSuccess
                        isMuted -> RoseError
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isAiSpeaking -> "${tutor.name} is speaking..."
                        isAiThinking -> "⚡ ${currentEngine.displayName} replying..."
                        isListening -> "Listening to you... (Speak naturally)"
                        isMuted -> "Microphone is Muted"
                        else -> "Ready to listen"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isAiSpeaking -> MaterialTheme.colorScheme.primary
                        isAiThinking -> MaterialTheme.colorScheme.tertiary
                        isListening -> EmeraldSuccess
                        isMuted -> RoseError
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Animated Audio Waveform (Reacts to speech & audio)
        WaveformVisualizer(
            isSpeaking = isAiSpeaking || isListening,
            amplitude = if (isListening) rmsDb else if (isAiSpeaking) 6f else 0.5f,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        // Live Floating Fluency Coach Tip / Correction Toast
        AnimatedVisibility(
            visible = activeCorrection != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            activeCorrection?.let { corr ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💡 Live Correction Tip",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = corr.correctedVersion,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = { onSaveCorrection(corr) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save mistake",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Teleprompter / Transcript Subtitles View
        if (showSubtitles) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transcriptItems) { item ->
                    val isUser = item.sender == CallSender.USER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isUser) "You" else tutor.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Currently partial transcribed speech
                if (currentSpeechText.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = currentSpeechText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Real-time Dynamic Conversational Action Pill (Zero-lag tap-to-send or interrupt)
        AnimatedVisibility(visible = isListening && !isMuted) {
            Button(
                onClick = onCompleteSpeechNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentSpeechText.isNotBlank()) EmeraldSuccess else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("send_speech_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentSpeechText.isNotBlank()) "Done Speaking (Send Now ⚡)" else "Listening... (Tap when done)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        AnimatedVisibility(visible = isAiSpeaking) {
            FilledTonalButton(
                onClick = onInterruptTutor,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AmberTertiary.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("interrupt_tutor_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = AmberTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Interrupt & Talk ✋",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // In-Call Action Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hint / "What to say next"
            IconButton(
                onClick = onTriggerHint,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Sentence Hints",
                    tint = AmberTertiary
                )
            }

            // Keyboard / Type Input
            IconButton(
                onClick = onOpenKeyboardInput,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("live_call_keyboard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Type text to AI",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mute / Unmute
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) RoseError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) RoseError else MaterialTheme.colorScheme.onSurface
                )
            }

            // End Call (Big Red Button)
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RoseError)
                    .testTag("end_live_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(
    isSpeaking: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = 32
        val barWidth = width / (barCount * 1.6f)
        val step = width / barCount

        val primaryColor = Color(0xFF4338CA)
        val accentColor = Color(0xFF0284C7)

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            val wave = if (isSpeaking) {
                val amp = (amplitude / 10f).coerceIn(0.2f, 1.0f)
                (sin(phase + normalizedX * 4f) * 0.4f + 0.6f) * amp
            } else {
                0.08f
            }
            val barHeight = (height * 0.85f * wave).coerceAtLeast(4f)
            val x = i * step + step / 4f

            drawRoundRect(
                brush = Brush.verticalGradient(listOf(primaryColor, accentColor)),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
private fun PostCallReportDialog(
    report: LiveCallSummaryReport,
    onDismiss: () -> Unit,
    onSaveAllCorrections: () -> Unit,
    onNavigateToMistakes: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Call Summary Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = report.cefrBand,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }
                Text(
                    text = "Session with ${report.tutorName} (${report.tutorRole})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Overall Score Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.overallFluencyScore}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Fluency Score",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.averageWpm}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = CyanSecondary
                                )
                                Text(
                                    text = "Avg WPM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatSeconds(report.callDurationSeconds),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldSuccess
                                )
                                Text(
                                    text = "Duration",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Coach Advice Card
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Tutor's Personalized Note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.finalCoachAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Session Corrections List
                if (report.correctionsList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Grammar & Fluency Corrections (${report.correctionsList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(report.correctionsList) { corr ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "❌ \"${corr.originalSaid}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoseError
                                )
                                Text(
                                    text = "✅ \"${corr.correctedVersion}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            if (report.correctionsList.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onSaveAllCorrections,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Mistake Bank")
                }
            }
        }
    )
}

@Composable
private fun QuickHintsDialog(
    topic: String,
    onSelectHint: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleStarters = listOf(
        "In my personal experience, I've found that...",
        "That's a very interesting point! What I think is...",
        "To be honest, I haven't thought about that deeply, but...",
        "Could you elaborate on what you mean by that?",
        "From my perspective, the most critical aspect is..."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sentence Starters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tap a phrase to continue the call conversation smoothly:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                sampleStarters.forEach { starter ->
                    OutlinedCard(
                        onClick = { onSelectHint(starter) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "\"$starter\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatSeconds(totalSecs: Long): String {
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun getFallbackResponse(tutor: LiveCallTutor, userMsg: String): String {
    val clean = userMsg.lowercase()
    return when {
        clean.contains("hello") || clean.contains("hi") ->
            "Hello! It is truly great to hear your voice. What would you like to discuss today?"
        clean.contains("interview") || clean.contains("job") ->
            "In professional interviews, concise structure is king. Can you describe a challenge you recently resolved?"
        clean.contains("travel") || clean.contains("country") ->
            "Traveling broadens your perspectives like nothing else. Which destination left the strongest impression on you?"
        else ->
            "That's a thoughtful point. How do you usually handle that in your day-to-day life?"
    }
}
