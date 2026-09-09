package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.TextToSpeechHelper
import com.example.data.local.ChapterDailyTopic
import com.example.data.local.DailyChaptersCurriculum
import com.example.data.local.RoadmapStep
import com.example.data.remote.AiEngine
import com.example.data.remote.AiEngineManager
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ChapterMode {
    LISTEN,    // Screenshot 1: Tutor Portrait with peach/bronze dual-tone ring, floating player dock, 5s rewind/forward, translation overlay
    PRACTICE   // Screenshot 2: Glowing cyan tutor ring, downward arrow, practice card with book, speaker, bookmark, bold sentence & Hindi translation, giant mic with halo
}

data class ChapterEvaluationReport(
    val overallScore: Int,
    val fluencyScore: Int,
    val fillerCount: Int,
    val wpm: Int,
    val structureScore: Int,
    val missingWords: List<String>,
    val feedback: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterFullViewScreen(
    step: RoadmapStep,
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onBack: () -> Unit,
    onChapterMastered: () -> Unit,
    onLaunchRoleplay: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiEngineManager = remember { AiEngineManager(context) }

    // Resolve chapter details from the 100-chapter curriculum
    val chapter: ChapterDailyTopic = remember(step.stepNumber) {
        DailyChaptersCurriculum.chapters100.find { it.chapterNumber == step.stepNumber }
            ?: ChapterDailyTopic(
                chapterNumber = step.stepNumber,
                title = step.title,
                subtitle = step.subtitle,
                imageRes = step.imageRes ?: DailyChaptersCurriculum.getImageForChapter(step.stepNumber),
                vocabWord = "Common Ground",
                vocabDefinition = "Shared interests or beliefs between people",
                vocabExample = "We found common ground talking about favorite movies.",
                pronunciationSentence = step.targetSentence.ifBlank { "Hi Leo! My name is Alex. Nice to meet you." },
                pronunciationTip = step.phoneticTip.ifBlank { "Blend 'Nice to meet you' naturally." },
                grammarQuestion = "Choose the correct polite greeting:",
                grammarOptions = listOf(step.targetSentence, "Me greeting you now.", "You want speaking me?"),
                correctGrammarIndex = 0,
                grammarExplanation = "Polite greeting idioms are essential in friendly introductions.",
                roleplayPrompt = "Introduce yourself politely to someone new.",
                reviewKeyPhrase = step.targetSentence,
                hindiTranslation = "Hi Leo! Mera naam Alex hai. Tumse milkar khushi hui.",
                tutorName = "Alex"
            )
    }

    // Active mode: LISTEN (Screenshot 1) vs PRACTICE (Screenshot 2)
    var currentMode by remember { mutableStateOf(ChapterMode.LISTEN) }

    // TTS & Speech state
    val isSpeaking by ttsHelper.isSpeaking.collectAsState()
    val isListening by speechHelper.isListening.collectAsState()
    val spokenPartial by speechHelper.currentText.collectAsState()

    var spokenText by remember { mutableStateOf("") }
    var evaluationScore by remember { mutableIntStateOf(0) }
    var isPassed by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var missingWordsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var evalReport by remember { mutableStateOf<ChapterEvaluationReport?>(null) }
    var speechStartTime by remember { mutableStateOf(0L) }

    // Audio Player controls in Listen Mode (Microsoft Edge Neural Voice default ~0.88x pacing)
    var paceSpeed by remember { mutableFloatStateOf(0.88f) }
    var showSubtitleTranslation by remember { mutableStateOf(true) }

    // Bookmark state (persisted locally)
    val bookmarkPrefs = remember { context.getSharedPreferences("chapter_bookmarks", Context.MODE_PRIVATE) }
    var isBookmarked by remember {
        mutableStateOf(bookmarkPrefs.getBoolean("bookmark_${chapter.chapterNumber}", false))
    }

    // Dialog states
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Pacing updates
    LaunchedEffect(paceSpeed) {
        ttsHelper.setSpeechRate(paceSpeed)
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.stop()
            speechHelper.stopListening()
        }
    }

    // Speech Evaluation Trigger with Multi-Candidate Analysis
    fun triggerSpeakingEvaluation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechStartTime = System.currentTimeMillis()
            speechHelper.startListeningWithCandidates(silenceTimeoutMs = 2200L, continuous = false) { bestResult, allCandidates ->
                val chosenText = bestResult.trim()
                if (chosenText.isNotBlank()) {
                    val durationSec = maxOf(1L, (System.currentTimeMillis() - speechStartTime) / 1000L)
                    spokenText = chosenText
                    evaluatePronunciationWithCandidates(
                        target = chapter.pronunciationSentence,
                        primarySpoken = chosenText,
                        allCandidates = allCandidates.ifEmpty { listOf(chosenText) },
                        durationSec = durationSec
                    ) { report ->
                        evalReport = report
                        evaluationScore = report.overallScore
                        missingWordsList = report.missingWords
                        feedbackMessage = report.feedback
                        if (report.overallScore >= 60) {
                            isPassed = true
                        } else {
                            coroutineScope.launch {
                                repository.logWeakness(
                                    originalMistake = chosenText.ifBlank { "Unclear utterance" },
                                    correctedForm = chapter.pronunciationSentence,
                                    category = "Pronunciation",
                                    explanation = "Chapter ${chapter.chapterNumber} Score: ${report.overallScore}%. Missing words: [${report.missingWords.joinToString(", ")}]. Tip: ${chapter.pronunciationTip}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerSpeakingEvaluation()
        } else {
            Toast.makeText(context, "Microphone permission is needed for speech evaluation", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListeningFlow() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            triggerSpeakingEvaluation()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F1015)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF191716),
                            Color(0xFF111217),
                            Color(0xFF0B0C10)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ==========================================
                // TOP BAR (Matches Screenshot 1 & 2)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chapter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Center Content: Title in Listen Mode, Progress Bar in Practice Mode
                    if (currentMode == ChapterMode.LISTEN) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            maxLines = 1
                        )
                    } else {
                        // Sleek Horizontal Cyan Progress Bar (Screenshot 2)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1C2735))
                        ) {
                            val progressFraction = if (isPassed) 1.0f else if (spokenText.isNotBlank()) 0.65f else 0.33f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF4CC9F0))
                            )
                        }
                    }

                    // Right Rounded Pill with Settings (⚙) and Flag (⚑)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1C1E26),
                        border = BorderStroke(1.dp, Color(0xFF2C303F))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.size(32.dp).testTag("chapter_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Lesson Settings",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.size(32.dp).testTag("chapter_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Report Lesson",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Mode Switcher Tab (Listen vs Practice)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF161821),
                    border = BorderStroke(1.dp, Color(0xFF262A38)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentMode == ChapterMode.LISTEN) Color(0xFF2E3345) else Color.Transparent,
                            modifier = Modifier.clickable {
                                currentMode = ChapterMode.LISTEN
                            }
                        ) {
                            Text(
                                text = "🎧 Listen Lesson",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentMode == ChapterMode.LISTEN) Color.White else Color(0xFFA0A4B4),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentMode == ChapterMode.PRACTICE) Color(0xFF4CC9F0) else Color.Transparent,
                            modifier = Modifier.clickable {
                                currentMode = ChapterMode.PRACTICE
                            }
                        ) {
                            Text(
                                text = "🗣️ Speak Practice",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMode == ChapterMode.PRACTICE) Color(0xFF0F1015) else Color(0xFFA0A4B4),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // SCREEN BODY BASED ON SELECTED MODE
                // ==========================================
                if (currentMode == ChapterMode.LISTEN) {
                    // SCREENSHOT 1: Full-Screen Lesson Avatar + Floating Dock
                    ListenLessonContent(
                        chapter = chapter,
                        isSpeaking = isSpeaking,
                        paceSpeed = paceSpeed,
                        showSubtitleTranslation = showSubtitleTranslation,
                        ttsHelper = ttsHelper,
                        onPaceChanged = { paceSpeed = it },
                        onToggleTranslation = { showSubtitleTranslation = !showSubtitleTranslation },
                        onSwitchToPractice = { currentMode = ChapterMode.PRACTICE }
                    )
                } else {
                    // SCREENSHOT 2: Practice Card + Speech Recognition + Giant Mic
                    SpeakPracticeContent(
                        chapter = chapter,
                        isListening = isListening,
                        spokenPartial = spokenPartial,
                        spokenText = spokenText,
                        evalReport = evalReport,
                        evaluationScore = evaluationScore,
                        isPassed = isPassed,
                        missingWordsList = missingWordsList,
                        feedbackMessage = feedbackMessage,
                        isBookmarked = isBookmarked,
                        ttsHelper = ttsHelper,
                        paceSpeed = paceSpeed,
                        onToggleBookmark = {
                            isBookmarked = !isBookmarked
                            bookmarkPrefs.edit().putBoolean("bookmark_${chapter.chapterNumber}", isBookmarked).apply()
                            Toast.makeText(
                                context,
                                if (isBookmarked) "Phrase saved to Favorites!" else "Removed from Favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onStartListening = {
                            if (isListening) {
                                speechHelper.stopListening()
                            } else {
                                startListeningFlow()
                            }
                        },
                        onChapterMastered = onChapterMastered,
                        onSwitchToListen = { currentMode = ChapterMode.LISTEN }
                    )
                }
            }
        }
    }

    // ==========================================
    // IN-LESSON SETTINGS DIALOG (⚙)
    // ==========================================
    if (showSettingsDialog) {
        val currentEngine by aiEngineManager.currentActiveEngine.collectAsState()

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = Color(0xFF181A24),
            title = {
                Text(
                    text = "Lesson & AI Engine Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            text = "Select Active AI Engine:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CC9F0)
                        )
                    }

                    items(AiEngine.values().toList()) { engine ->
                        val isSelected = currentEngine == engine
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF262C3D) else Color(0xFF1E212B),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF4CC9F0) else Color(0xFF2E3242)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    aiEngineManager.setEngine(engine)
                                    Toast.makeText(context, "Active: ${engine.displayName}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { aiEngineManager.setEngine(engine) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4CC9F0),
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = engine.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${engine.provider} • ${engine.badge}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA0A4B4)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Voice Pace Speed: ${(paceSpeed * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CC9F0)
                        )
                        Slider(
                            value = paceSpeed,
                            onValueChange = { paceSpeed = it },
                            valueRange = 0.75f..1.25f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CC9F0),
                                activeTrackColor = Color(0xFF4CC9F0)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done", color = Color(0xFF4CC9F0), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ==========================================
    // FLAG / REPORT MISTAKE DIALOG (⚑)
    // ==========================================
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = Color(0xFF181A24),
            title = {
                Text(
                    text = "Report Lesson Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Help us improve Chapter ${chapter.chapterNumber}: '${chapter.title}'",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A4B4)
                    )

                    listOf(
                        "Pronunciation audio unclear",
                        "Hindi / Hinglish translation issue",
                        "Difficulty level too high / low",
                        "Other suggestion"
                    ).forEach { issue ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF222533),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showReportDialog = false
                                    Toast.makeText(context, "Thank you! Feedback reported for review.", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = Color(0xFFA0A4B4))
                }
            }
        )
    }
}

// ==============================================================================
// 1. LISTEN LESSON CONTENT (Directly Implements Screenshot 1)
// ==============================================================================
@Composable
private fun ListenLessonContent(
    chapter: ChapterDailyTopic,
    isSpeaking: Boolean,
    paceSpeed: Float,
    showSubtitleTranslation: Boolean,
    ttsHelper: TextToSpeechHelper,
    onPaceChanged: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    onSwitchToPractice: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Level Badge & Title Header
        val levelLabel = when {
            chapter.chapterNumber <= 30 -> "Level 1 • Beginner"
            chapter.chapterNumber <= 60 -> "Level 2 • Conversational"
            chapter.chapterNumber <= 80 -> "Level 3 • Intermediate"
            else -> "Level 4 • Advanced Mastery"
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E212B),
            border = BorderStroke(1.dp, Color(0xFF323646))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 $levelLabel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CC9F0)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Center Tutor Avatar with Dual-Tone Arc Ring (Peach Top + Bronze Bottom)
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated pulsating wave ring while tutor is speaking
            if (isSpeaking) {
                val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(216.dp * pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFF4CBB2).copy(alpha = 0.12f))
                )
            }

            // Two-tone circular progress/accent arc ring:
            // Top Arc: Peach/cream (#F4CBB2)
            // Bottom Arc: Bronze/taupe (#8C6D58)
            Canvas(modifier = Modifier.size(216.dp)) {
                val strokeWidth = 7.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)

                // Top arc (180 degrees from -180° to 0°) - Peach / cream
                drawArc(
                    color = Color(0xFFF4CBB2),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Bottom arc (180 degrees from 0° to 180°) - Bronze / taupe
                drawArc(
                    color = Color(0xFF8C6D58),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Tutor portrait image
            Box(
                modifier = Modifier
                    .size(194.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = chapter.imageRes),
                    contentDescription = chapter.tutorName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Subtitle & Hinglish Translation Card (Toggleable via 文A) + Tap-to-Listen Vocabulary Card
        AnimatedVisibility(visible = showSubtitleTranslation) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF181B24).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF2C3040)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\"${chapter.pronunciationSentence}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chapter.hindiTranslation.ifBlank { "Mera naam Alex hai. Tumse milkar khushi hui." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0B4C4),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Tap-to-Listen Vocabulary Card
                if (chapter.vocabWord.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF141720),
                        border = BorderStroke(1.dp, Color(0xFF262A38)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎯 Key Vocab: ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8E92A0)
                                    )
                                    Text(
                                        text = chapter.vocabWord,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CC9F0)
                                    )
                                }
                                Text(
                                    text = "${chapter.vocabDefinition} • Ex: \"${chapter.vocabExample}\"",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA0A4B4),
                                    maxLines = 2
                                )
                            }
                            IconButton(
                                onClick = {
                                    ttsHelper.speak("${chapter.vocabWord}. ${chapter.vocabExample}")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Listen to vocab",
                                    tint = Color(0xFF4CC9F0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // FLOATING PLAYER DOCK (Screenshot 1)
        // [ 文A ]  [ ↺ 5 ]  [  ▶ / ⏸  ]  [ ↻ 5 ]  [ 1.0x ]
        // ==========================================
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1B1D26),
            border = BorderStroke(1.dp, Color(0xFF2B2F3E)),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Translation Toggle (文A)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onToggleTranslation() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "文A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showSubtitleTranslation) Color(0xFF4CC9F0) else Color(0xFFA0A4B4)
                    )
                }

                // 2. Rewind 5s (↺ 5)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            ttsHelper.stop()
                            ttsHelper.speak(chapter.pronunciationSentence)
                            Toast.makeText(context, "↺ Replaying model phrase", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↺",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "5",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 3. Large White Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (isSpeaking) {
                                ttsHelper.stop()
                            } else {
                                val fullIntro = "Hi! I am ${chapter.tutorName}. Let's learn: ${chapter.pronunciationSentence}."
                                ttsHelper.speak(fullIntro)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isSpeaking) "Pause" else "Play",
                        tint = Color(0xFF0F1015),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 4. Forward 5s (↻ 5)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            ttsHelper.stop()
                            ttsHelper.speak("Remember this tip: ${chapter.pronunciationTip}")
                            Toast.makeText(context, "Tip: ${chapter.pronunciationTip}", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "5",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "↻",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 5. Speed Indicator / Toggle (0.88x / 1.0x / 0.8x / 1.1x)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            val nextPace = when (paceSpeed) {
                                0.88f -> 1.0f
                                1.0f -> 1.15f
                                1.15f -> 0.78f
                                else -> 0.88f
                            }
                            onPaceChanged(nextPace)
                            Toast.makeText(context, "Voice Pace: ${nextPace}x", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (paceSpeed == 0.88f) "0.88x" else "${paceSpeed}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent button to smoothly transition to Speaking Practice (Screenshot 2)
        Button(
            onClick = onSwitchToPractice,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CC9F0)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_speaking_practice_button")
        ) {
            Text(
                text = "Next: Practice Speaking ➔",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F1015)
            )
        }
    }
}

// ==============================================================================
// 2. SPEAK PRACTICE CONTENT (Directly Implements Screenshot 2)
// ==============================================================================
@Composable
private fun SpeakPracticeContent(
    chapter: ChapterDailyTopic,
    isListening: Boolean,
    spokenPartial: String,
    spokenText: String,
    evalReport: ChapterEvaluationReport?,
    evaluationScore: Int,
    isPassed: Boolean,
    missingWordsList: List<String>,
    feedbackMessage: String,
    isBookmarked: Boolean,
    ttsHelper: TextToSpeechHelper,
    paceSpeed: Float,
    onToggleBookmark: () -> Unit,
    onStartListening: () -> Unit,
    onChapterMastered: () -> Unit,
    onSwitchToListen: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Circular Tutor Avatar with Glowing Cyan Border & Downward Pointer Tick
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tutor Avatar (~130dp) with cyan border (Screenshot 2)
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .border(3.dp, Color(0xFF4CC9F0), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = chapter.imageRes),
                    contentDescription = chapter.tutorName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Downward Pointer Triangle Tick
            Canvas(modifier = Modifier.size(16.dp, 8.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(path = path, color = Color(0xFF1C1E28))
            }

            // Speech Badge: "Try speaking this sentence"
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1C1E28),
                border = BorderStroke(1.dp, Color(0xFF2C3040))
            ) {
                Text(
                    text = "Try speaking this sentence",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Practice Card with Cyan Border (Screenshot 2)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF13151D),
            border = BorderStroke(1.5.dp, Color(0xFF4CC9F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top Header inside Practice Card:
                // Left: "📖 Tap on Mic and Read"
                // Right: [🔊] Listen  [🔖] Bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📖", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tap on Mic and Read",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Speaker button (cyan circle)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CC9F0).copy(alpha = 0.18f))
                                .clickable {
                                    ttsHelper.setSpeechRate(paceSpeed)
                                    ttsHelper.speak(chapter.pronunciationSentence)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen to Model",
                                tint = Color(0xFF4CC9F0),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Bookmark button (cyan circle)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CC9F0).copy(alpha = 0.18f))
                                .clickable { onToggleBookmark() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Phrase",
                                tint = Color(0xFF4CC9F0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bold Target Sentence in White (Tap any word to pronounce individually!)
                val words = chapter.pronunciationSentence.split(" ")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Column {
                        Text(
                            text = "\"${chapter.pronunciationSentence}\"",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Roman Hindi / Hinglish translation
                        Text(
                            text = chapter.hindiTranslation.ifBlank { "Mera naam Alex hai. Tumse milkar khushi hui." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9AA0B2)
                        )
                    }
                }

                // Interactive feature: Word-by-word pronunciation pills
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    words.take(5).forEach { word ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2230),
                            modifier = Modifier.clickable {
                                ttsHelper.speak(word.replace(Regex("[^a-zA-Z]"), ""))
                            }
                        ) {
                            Text(
                                text = word,
                                fontSize = 11.sp,
                                color = Color(0xFF4CC9F0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Spoken Speech Feedback & 4-Pillar Score Display
                if (spokenText.isNotBlank() && evalReport != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF262A3A))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "You said: \"$spokenText\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E5F0)
                            )
                            Text(
                                text = feedbackMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPassed) EmeraldSuccess else AmberTertiary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPassed) EmeraldSuccess.copy(alpha = 0.2f) else RoseError.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$evaluationScore / 100",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPassed) EmeraldSuccess else RoseError,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 4 Pillars Chips
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScorePillarChip("Fluency", "${evalReport.fluencyScore}%")
                        ScorePillarChip("Fillers", "${evalReport.fillerCount}")
                        ScorePillarChip("Pace", "${evalReport.wpm} WPM")
                        ScorePillarChip("Structure", "${evalReport.structureScore}%")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time audio hearing transcript preview
        if (isListening && spokenPartial.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E2333),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Hearing: \"$spokenPartial\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CC9F0),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ==========================================
        // GIANT INTERACTIVE MIC BUTTON (Screenshot 2)
        // Outer dark circular halo ring + Inner bright cyan mic
        // ==========================================
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer Halo Ring with animated breathing pulse when listening
            val infiniteTransition = rememberInfiniteTransition(label = "haloTransition")
            val haloAlpha by infiniteTransition.animateFloat(
                initialValue = if (isListening) 0.35f else 0.15f,
                targetValue = if (isListening) 0.85f else 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "haloAlpha"
            )

            Box(
                modifier = Modifier
                    .size(122.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF192433).copy(alpha = haloAlpha))
                    .border(2.dp, Color(0xFF4CC9F0).copy(alpha = if (isListening) 0.8f else 0.25f), CircleShape)
            )

            // Inner Cyan Mic Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isListening) RoseError else Color(0xFF4CC9F0))
                    .clickable { onStartListening() }
                    .testTag("giant_practice_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Recording" else "Speak Now",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Action Buttons: If passed -> Continue/Mastered button, else Switch to Listen option
        if (isPassed) {
            Button(
                onClick = onChapterMastered,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("next_chapter_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Passed (Score: $evaluationScore%) • Continue Next Chapter ➔",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            OutlinedButton(
                onClick = onSwitchToListen,
                border = BorderStroke(1.dp, Color(0xFF2C3244)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "🎧 Back to Listen Mode",
                    fontSize = 13.sp,
                    color = Color(0xFFA0A4B4)
                )
            }
        }
    }
}

@Composable
private fun ScorePillarChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E2230)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, color = Color(0xFF8E92A4))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==============================================================================
// 3. ADVANCED FORGIVING MULTI-CANDIDATE PRONUNCIATION EVALUATION ALGORITHM
// ==============================================================================
private fun evaluatePronunciationWithCandidates(
    target: String,
    primarySpoken: String,
    allCandidates: List<String>,
    durationSec: Long = 3L,
    onResult: (report: ChapterEvaluationReport) -> Unit
): Int {
    // Try all recognition candidates and pick the best score
    val candidatesToEvaluate = (listOf(primarySpoken) + allCandidates).distinct().filter { it.isNotBlank() }
    
    if (candidatesToEvaluate.isEmpty()) {
        val emptyReport = ChapterEvaluationReport(
            overallScore = 0,
            fluencyScore = 0,
            fillerCount = 0,
            wpm = 0,
            structureScore = 0,
            missingWords = normalizeSpeechWords(target),
            feedback = "No clear speech detected. Please tap mic and speak clearly."
        )
        onResult(emptyReport)
        return 0
    }

    var bestReport: ChapterEvaluationReport? = null
    var bestScore = -1

    for (candidate in candidatesToEvaluate) {
        val report = evaluateSingleUtterance(target, candidate, durationSec)
        if (report.overallScore > bestScore) {
            bestScore = report.overallScore
            bestReport = report
        }
    }

    val finalReport = bestReport ?: evaluateSingleUtterance(target, primarySpoken, durationSec)
    onResult(finalReport)
    return finalReport.overallScore
}

private fun evaluateSingleUtterance(
    target: String,
    spoken: String,
    durationSec: Long = 3L
): ChapterEvaluationReport {
    val targetWords = normalizeSpeechWords(target)
    val spokenWords = normalizeSpeechWords(spoken)

    if (spokenWords.isEmpty()) {
        return ChapterEvaluationReport(
            overallScore = 0,
            fluencyScore = 0,
            fillerCount = 0,
            wpm = 0,
            structureScore = 0,
            missingWords = targetWords,
            feedback = "No clear speech detected. Please tap mic and speak clearly."
        )
    }

    // 1. Detect filler words ("um", "uh", "er", "like", "actually", "basically", "you know")
    val fillerRegex = Regex("\\b(um|uh|er|ah|like|actually|basically|you know)\\b", RegexOption.IGNORE_CASE)
    val fillerCount = fillerRegex.findAll(spoken).count()

    // 2. Estimate Pace (WPM)
    val safeDuration = maxOf(1L, durationSec)
    val calculatedWpm = ((spokenWords.size * 60L) / safeDuration).toInt().coerceIn(45, 210)

    // 3. Sentence Structure & Word Accuracy
    val spokenSet = spokenWords.toSet()
    val missingWords = mutableListOf<String>()
    var matchedCount = 0

    targetWords.forEach { word ->
        if (spokenSet.contains(word)) {
            matchedCount++
        } else {
            val nearMatch = spokenWords.any { s -> isPhoneticOrNearMatch(word, s) }
            if (nearMatch) {
                matchedCount++
            } else {
                missingWords.add(word)
            }
        }
    }

    val matchRatio = if (targetWords.isNotEmpty()) {
        matchedCount.toFloat() / targetWords.size.toFloat()
    } else 1.0f

    val structureScore = (matchRatio * 100).roundToInt().coerceIn(10, 100)

    // 4. Fluency Score based on Pace and Filler words
    val paceScore = when (calculatedWpm) {
        in 100..170 -> 95
        in 80..99, in 171..195 -> 85
        else -> 70
    }
    val fillerPenalty = (fillerCount * 5).coerceAtMost(25)
    val fluencyScore = (paceScore - fillerPenalty).coerceIn(25, 100)

    // Overall blended score (60% required to pass)
    val calculatedScore = ((structureScore * 0.70f) + (fluencyScore * 0.30f)).roundToInt().coerceIn(10, 100)

    val feedback = when {
        calculatedScore >= 85 -> "Outstanding! Natural pace ($calculatedWpm WPM), clean pronunciation & fluent rhythm."
        calculatedScore >= 60 -> "Passed ($calculatedScore%)! Great accuracy. Ready to proceed!"
        else -> "Score: $calculatedScore% (Need 60%+ to pass). Missed: ${missingWords.take(3).joinToString(", ")}. Tap 🔊 to listen again."
    }

    return ChapterEvaluationReport(
        overallScore = calculatedScore,
        fluencyScore = fluencyScore,
        fillerCount = fillerCount,
        wpm = calculatedWpm,
        structureScore = structureScore,
        missingWords = missingWords,
        feedback = feedback
    )
}

/**
 * Normalizes speech text by expanding contractions, numbers, and stripping punctuation
 */
private fun normalizeSpeechWords(input: String): List<String> {
    var s = input.lowercase().trim()
    
    // Contractions expansion
    s = s.replace("i'm", "i am")
        .replace("i'd", "i would")
        .replace("i'll", "i will")
        .replace("i've", "i have")
        .replace("you're", "you are")
        .replace("you'll", "you will")
        .replace("you've", "you have")
        .replace("we're", "we are")
        .replace("they're", "they are")
        .replace("it's", "it is")
        .replace("that's", "that is")
        .replace("there's", "there is")
        .replace("what's", "what is")
        .replace("let's", "let us")
        .replace("can't", "can not")
        .replace("cannot", "can not")
        .replace("won't", "will not")
        .replace("don't", "do not")
        .replace("doesn't", "does not")
        .replace("didn't", "did not")
        .replace("couldn't", "could not")
        .replace("wouldn't", "would not")
        .replace("shouldn't", "should not")
        .replace("hasn't", "has not")
        .replace("haven't", "have not")
        .replace("isn't", "is not")
        .replace("aren't", "are not")
        .replace("oat milk", "oatmilk")
        .replace("wifi", "wi fi")
        .replace("checkout", "check out")
        .replace("12%", "twelve percent")
        .replace("12 percent", "twelve percent")
        .replace("412", "four twelve")
        .replace("flight 412", "flight four twelve")
        .replace("2:00", "two")
        .replace("2 pm", "two pm")
        .replace(" 2 ", " two ")
        .replace(" 4 ", " four ")
        .replace(" 1 ", " one ")
        .replace(" 3 ", " three ")
        .replace(" 5 ", " five ")

    val cleaned = s.replace(Regex("[^a-z0-9\\s]"), " ")
    return cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
}

private fun isPhoneticOrNearMatch(targetWord: String, spokenWord: String): Boolean {
    if (targetWord == spokenWord) return true
    
    // Equivalent phonetic or accent variations
    val synonyms = mapOf(
        "latte" to listOf("late", "latti", "coffee"),
        "decaf" to listOf("d caf", "decaffeinated", "d-caf", "dee caf"),
        "uptown" to listOf("up town", "up-town"),
        "downtown" to listOf("down town", "down-town"),
        "slack" to listOf("slac", "lack"),
        "oatmilk" to listOf("oat", "milk"),
        "twelve" to listOf("12", "twelf"),
        "two" to listOf("2", "too", "to"),
        "four" to listOf("4", "for", "fore"),
        "their" to listOf("there", "they're"),
        "there" to listOf("their", "they're"),
        "gym" to listOf("jim", "gim"),
        "route" to listOf("rout"),
        "yeah" to listOf("yes", "ya"),
        "hi" to listOf("hello", "hey")
    )
    if (synonyms[targetWord]?.contains(spokenWord) == true) return true

    if (kotlin.math.abs(targetWord.length - spokenWord.length) > 2) return false
    val dist = levenshteinDistance(targetWord, spokenWord)
    return dist <= 2
}

private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
    val lhsLen = lhs.length
    val rhsLen = rhs.length
    var cost = Array(lhsLen + 1) { it }
    var newCost = Array(lhsLen + 1) { 0 }

    for (i in 1..rhsLen) {
        newCost[0] = i
        for (j in 1..lhsLen) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1
            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }
        val swap = cost
        cost = newCost
        newCost = swap
    }
    return cost[lhsLen]
}
