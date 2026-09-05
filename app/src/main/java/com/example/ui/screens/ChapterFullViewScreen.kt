package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.local.entity.WeaknessItem
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Full Immersive Screen for a Learning Chapter.
 * Features:
 * 1. AI Teacher persona speaking naturally at a friendly, normal human pace (Hindi/Hinglish friendly explanation).
 * 2. Explains clearly what today's lesson is about, where it is used in real life.
 * 3. Guided sentence practice with real speech recognition accuracy & word-by-word pronunciation correction.
 * 4. Automatic error logging to the user's personal Weakness Log when mistakes happen.
 * 5. App decides completion: Unlock only occurs when the user successfully practices and achieves passing score.
 */
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

    // Find the full chapter details from the curriculum
    val chapter: ChapterDailyTopic = remember(step.stepNumber) {
        DailyChaptersCurriculum.chapters100.find { it.chapterNumber == step.stepNumber }
            ?: ChapterDailyTopic(
                chapterNumber = step.stepNumber,
                title = step.title,
                subtitle = step.subtitle,
                imageRes = step.imageRes ?: DailyChaptersCurriculum.getImageForChapter(step.stepNumber),
                vocabWord = "Essential Expression",
                vocabDefinition = "Key practical phrase used in daily conversations",
                vocabExample = step.targetSentence,
                pronunciationSentence = step.targetSentence,
                pronunciationTip = step.phoneticTip.ifBlank { "Speak smoothly and clearly with natural pauses." },
                grammarQuestion = "Which sentence sounds most polite and natural?",
                grammarOptions = listOf(
                    step.targetSentence,
                    "I want this immediately now.",
                    "Me going for saying this."
                ),
                correctGrammarIndex = 0,
                grammarExplanation = "Polite phrasing with modal verbs is standard in everyday English.",
                roleplayPrompt = "Practice this real-life scenario with confidence.",
                reviewKeyPhrase = step.targetSentence
            )
    }

    // AI Teacher speech states
    val isSpeaking by ttsHelper.isSpeaking.collectAsState()
    val isListening by speechHelper.isListening.collectAsState()
    val spokenPartial by speechHelper.currentText.collectAsState()

    var spokenText by remember { mutableStateOf("") }
    var evaluationScore by remember { mutableIntStateOf(0) }
    var isPassed by remember { mutableStateOf(false) }
    var teacherSpokenIntro by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: AI Overview & Explanation, 1: Speaking & Pronunciation Practice, 2: Grammar & Context Check
    var feedbackMessage by remember { mutableStateOf("") }
    var missingWordsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var paceSpeed by remember { mutableStateOf(0.88f) } // Default friendly, clear, human pace

    // Set voice pace on enter
    LaunchedEffect(paceSpeed) {
        ttsHelper.setSpeechRate(paceSpeed)
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.stop()
            speechHelper.stopListening()
        }
    }

    // Teacher Introduction Text (English + friendly explanation of where this is used)
    val teacherExplanation = remember(chapter) {
        "Hello! Aaj hum Chapter ${chapter.chapterNumber}: '${chapter.title}' seekhenge. " +
        "Yeh real life mein tab kaam aata hai jab aap ${chapter.subtitle.lowercase()}. " +
        "Sabse zaroori sentence hai: \"${chapter.pronunciationSentence}\". " +
        "Dhyaan rahe, ${chapter.pronunciationTip}. Chalo ab practice karte hain!"
    }

    val teacherEnglishIntro = remember(chapter) {
        "Welcome to Chapter ${chapter.chapterNumber}: ${chapter.title}. " +
        "In this chapter, you'll learn how to handle: ${chapter.subtitle}. " +
        "We'll practice the exact phrases native speakers use. Let's listen first, then practice speaking together!"
    }

    // Microphone permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechEvaluation(
                speechHelper = speechHelper,
                targetSentence = chapter.pronunciationSentence,
                onSpoken = { result ->
                    spokenText = result
                    val score = evaluatePronunciation(
                        target = chapter.pronunciationSentence,
                        spoken = result,
                        onResult = { calculatedScore, missing, feedback ->
                            evaluationScore = calculatedScore
                            missingWordsList = missing
                            feedbackMessage = feedback
                            if (calculatedScore >= 65) {
                                isPassed = true
                            } else {
                                // Auto-log pronunciation weakness to Room DB
                                coroutineScope.launch {
                                    repository.logWeakness(
                                        originalMistake = result.ifBlank { "Unclear pronunciation" },
                                        correctedForm = chapter.pronunciationSentence,
                                        category = "Pronunciation",
                                        explanation = "Pronunciation error in Chapter ${chapter.chapterNumber}: Missed words [${missing.joinToString(", ")}]. ${chapter.pronunciationTip}"
                                    )
                                }
                            }
                        }
                    )
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is needed to evaluate your speech", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chapter ${chapter.chapterNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9D4EDD),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Speech Pace Control toggle (Normal, Slower for clear learning)
                    IconButton(
                        onClick = {
                            paceSpeed = if (paceSpeed < 0.95f) 0.98f else 0.82f
                            ttsHelper.setSpeechRate(paceSpeed)
                            Toast.makeText(
                                context,
                                if (paceSpeed < 0.90f) "Voice Pace: Relaxed & Human (0.82x)" else "Voice Pace: Natural Conversational (0.98x)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Adjust Voice Pace",
                            tint = if (paceSpeed < 0.90f) EmeraldSuccess else AmberTertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14151B)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chapter Hero Card with Context Banner & AI Avatar
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A1B22),
                    border = BorderStroke(1.dp, Color(0xFF2E313D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Image(
                                painter = painterResource(id = chapter.imageRes),
                                contentDescription = chapter.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xDD1A1B22), Color(0xFF1A1B22))
                                        )
                                    )
                            )
                            // Chapter badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF9D4EDD),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "CHAPTER ${chapter.chapterNumber} • REAL-LIFE MASTERY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chapter.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB4B7C5)
                            )
                        }
                    }
                }
            }

            // AI Teacher Persona Box (Explains kya padhenge, kaha use hoga, normal human pace)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF181B26),
                    border = BorderStroke(1.5.dp, if (isSpeaking) Color(0xFF9D4EDD) else Color(0xFF282C3D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9D4EDD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "AI Coach",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Voice Coach (Teacher)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isSpeaking) "Speaking now (Pace: ${(paceSpeed * 100).roundToInt()}%) • Listen carefully" else "Tap below to listen & understand",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSpeaking) Color(0xFFC77DFF) else Color(0xFF8E92A0)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isSpeaking) {
                                        ttsHelper.stop()
                                    } else {
                                        teacherSpokenIntro = true
                                        ttsHelper.setSpeechRate(paceSpeed)
                                        ttsHelper.speak(teacherExplanation)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = "Listen to AI Teacher",
                                    tint = Color(0xFFC77DFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Real-life purpose explanation
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF222533)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = AmberTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Yeh Kaha Use Hoga (Real-World Use):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = AmberTertiary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Jab aap daily life mein ${chapter.subtitle.lowercase()} karenge, toh yeh conversation flow exact native standard par kaam aayega.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD3D5E0),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button to speak explanation in human pace
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    ttsHelper.setSpeechRate(paceSpeed)
                                    ttsHelper.speak(teacherExplanation)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Explain in Hindi/English", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    ttsHelper.setSpeechRate(paceSpeed)
                                    ttsHelper.speak(teacherEnglishIntro)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("English Native", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Key Vocabulary for this Chapter
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B1C23),
                    border = BorderStroke(1.dp, Color(0xFF2C2F3C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFF48CAE4),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Key Chapter Vocabulary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF48CAE4)
                                )
                            }

                            IconButton(
                                onClick = {
                                    ttsHelper.speak("${chapter.vocabWord}. Definition: ${chapter.vocabDefinition}. Example: ${chapter.vocabExample}")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Listen to vocab",
                                    tint = Color(0xFF48CAE4)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = chapter.vocabWord,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = chapter.vocabDefinition,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A4B4)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF242735),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Example: \"${chapter.vocabExample}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E4EE),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Practice Sentence & Speech Recognition Section
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1A1C25),
                    border = BorderStroke(
                        1.5.dp,
                        if (isPassed) EmeraldSuccess else if (isListening) RoseError else Color(0xFF333748)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Speaking & Pronunciation Test",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPassed) EmeraldSuccess.copy(alpha = 0.2f) else AmberTertiary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isPassed) "✅ UNLOCKED" else "🔒 REQUIRED TO PASS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPassed) EmeraldSuccess else AmberTertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target line to speak
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF232636),
                            border = BorderStroke(1.dp, Color(0xFF373B50)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Target Sentence to Speak:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC77DFF)
                                    )

                                    IconButton(
                                        onClick = {
                                            ttsHelper.setSpeechRate(paceSpeed)
                                            ttsHelper.speak(chapter.pronunciationSentence)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Listen to Model",
                                            tint = Color(0xFFC77DFF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${chapter.pronunciationSentence}\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AmberTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pronunciation Tip: ${chapter.pronunciationTip}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD0D3E2)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mic Button and Real-Time Feedback
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    if (isListening) {
                                        speechHelper.stopListening()
                                    } else {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            startSpeechEvaluation(
                                                speechHelper = speechHelper,
                                                targetSentence = chapter.pronunciationSentence,
                                                onSpoken = { result ->
                                                    spokenText = result
                                                    evaluatePronunciation(
                                                        target = chapter.pronunciationSentence,
                                                        spoken = result,
                                                        onResult = { score, missing, feedback ->
                                                            evaluationScore = score
                                                            missingWordsList = missing
                                                            feedbackMessage = feedback
                                                            if (score >= 65) {
                                                                isPassed = true
                                                            } else {
                                                                coroutineScope.launch {
                                                                    repository.logWeakness(
                                                                        originalMistake = result.ifBlank { "Speech unclear" },
                                                                        correctedForm = chapter.pronunciationSentence,
                                                                        category = "Pronunciation",
                                                                        explanation = "Chapter ${chapter.chapterNumber} check failed ($score/100). Missing: [${missing.joinToString(", ")}]. ${chapter.pronunciationTip}"
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            )
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) RoseError else Color(0xFF9D4EDD)
                                ),
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("chapter_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Speak Sentence",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isListening) "Listening... speak now" else "Tap Mic & Speak the sentence",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) Color(0xFFFF758F) else Color(0xFFC77DFF)
                            )
                        }

                        // Real-time spoken text preview
                        if (isListening && spokenPartial.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF262835),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hearing: \"$spokenPartial\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E4EE),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Evaluated Result Display
                        if (spokenText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFF2E3244))
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "You said: \"$spokenText\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pronunciation & Clarity: $evaluationScore / 100",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPassed) EmeraldSuccess else RoseError,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isPassed) "PASSED ✅" else "TRY AGAIN (Needed: 65%)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPassed) EmeraldSuccess else AmberTertiary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (evaluationScore / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isPassed) EmeraldSuccess else AmberTertiary,
                                trackColor = Color(0xFF2A2D3C)
                            )

                            if (feedbackMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = feedbackMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPassed) EmeraldSuccess else AmberTertiary
                                )
                            }

                            if (!isPassed && missingWordsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RoseError.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ Auto-logged to Mistake Bank: Practice these words: [${missingWordsList.joinToString(", ")}]",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF94A4),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Grammar Check Section (to confirm understanding of where it is used)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1C24),
                    border = BorderStroke(1.dp, Color(0xFF2B2E3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF06D6A0),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Natural Grammar & Context Check",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF06D6A0),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = chapter.grammarQuestion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var selectedOption by remember { mutableStateOf<Int?>(null) }
                        var answeredCorrectly by remember { mutableStateOf<Boolean?>(null) }

                        chapter.grammarOptions.forEachIndexed { index, option ->
                            val isChosen = selectedOption == index
                            val isThisCorrect = index == chapter.correctGrammarIndex
                            val optionBorder = when {
                                selectedOption == null -> Color(0xFF323647)
                                isChosen && isThisCorrect -> EmeraldSuccess
                                isChosen && !isThisCorrect -> RoseError
                                isThisCorrect -> EmeraldSuccess.copy(alpha = 0.6f)
                                else -> Color(0xFF282B38)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChosen) Color(0xFF25293A) else Color(0xFF1E212D),
                                border = BorderStroke(1.dp, optionBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        if (selectedOption == null) {
                                            selectedOption = index
                                            val correct = (index == chapter.correctGrammarIndex)
                                            answeredCorrectly = correct
                                            if (!correct) {
                                                coroutineScope.launch {
                                                    repository.logWeakness(
                                                        originalMistake = option,
                                                        correctedForm = chapter.grammarOptions[chapter.correctGrammarIndex],
                                                        category = "Grammar",
                                                        explanation = "Chapter ${chapter.chapterNumber}: ${chapter.grammarExplanation}"
                                                    )
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Text(
                                    text = "${('A' + index)}. $option",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (selectedOption != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = chapter.grammarExplanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (answeredCorrectly == true) EmeraldSuccess else AmberTertiary
                            )
                        }
                    }
                }
            }

            // Bottom Complete & Unlock Action (Controlled strictly by App evaluation)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isPassed) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF202330),
                            border = BorderStroke(1.dp, Color(0xFF333748)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AmberTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Speak the sentence above with 65%+ clarity to unlock this chapter. The app will grade your pronunciation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4D7E4)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            if (isPassed) {
                                onChapterMastered()
                                Toast.makeText(context, "🎉 Chapter ${chapter.chapterNumber} Mastered & Unlocked!", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please complete the speaking test with at least 65% score before completing!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("chapter_complete_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPassed) EmeraldSuccess else Color(0xFF4A4E62)
                        ),
                        enabled = isPassed
                    ) {
                        Icon(
                            imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Lock,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPassed) "Chapter Mastered • Unlock Next" else "Locked • Speak Sentence to Unlock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // Optional Full Conversation Roleplay button
                    if (chapter.roleplayPrompt.isNotBlank() && onLaunchRoleplay != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                onLaunchRoleplay("scenario_daily_${chapter.chapterNumber}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFC77DFF)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Conversation Practice with AI", color = Color(0xFFC77DFF))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Starts speech recognition with fallback to ensure the user is heard properly.
 */
private fun startSpeechEvaluation(
    speechHelper: SpeechRecognitionHelper,
    targetSentence: String,
    onSpoken: (String) -> Unit
) {
    speechHelper.startListening { result ->
        if (result.isNotBlank()) {
            onSpoken(result.trim())
        }
    }
}

/**
 * Evaluates spoken result against target sentence using normalized word-level and phonetic matching.
 * Provides granular accuracy score, detected missing words, and helpful advice.
 */
private fun evaluatePronunciation(
    target: String,
    spoken: String,
    onResult: (score: Int, missingWords: List<String>, feedback: String) -> Unit
): Int {
    val cleanTarget = target.lowercase().replace(Regex("[^a-z0-9\\s]"), "")
    val cleanSpoken = spoken.lowercase().replace(Regex("[^a-z0-9\\s]"), "")

    val targetWords = cleanTarget.split(Regex("\\s+")).filter { it.isNotBlank() }
    val spokenWords = cleanSpoken.split(Regex("\\s+")).filter { it.isNotBlank() }

    if (spokenWords.isEmpty()) {
        onResult(0, targetWords, "No clear speech detected. Please speak clearly closer to the microphone.")
        return 0
    }

    val spokenSet = spokenWords.toSet()
    val missingWords = mutableListOf<String>()
    var matchedCount = 0

    targetWords.forEach { word ->
        if (spokenSet.contains(word)) {
            matchedCount++
        } else {
            // Check near match (Levenshtein distance <= 2 for pronunciation tolerance)
            val nearMatch = spokenWords.any { s -> isNearMatch(word, s) }
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

    val calculatedScore = (matchRatio * 100).roundToInt().coerceIn(10, 100)

    val feedback = when {
        calculatedScore >= 85 -> "Excellent pronunciation! Very clear and natural."
        calculatedScore >= 65 -> "Good job! You passed. Try repeating once more to smooth out cadence."
        else -> "Needs improvement on words: ${missingWords.take(3).joinToString(", ")}. Tap Listen to hear the native guide."
    }

    onResult(calculatedScore, missingWords, feedback)
    return calculatedScore
}

private fun isNearMatch(s1: String, s2: String): Boolean {
    if (s1 == s2) return true
    if (kotlin.math.abs(s1.length - s2.length) > 2) return false
    val dist = levenshteinDistance(s1, s2)
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
