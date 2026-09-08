package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.components.NativeNaturalizerView
import com.example.ui.components.PronunciationLabView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.R
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.TextToSpeechHelper
import com.example.data.local.DefaultData
import com.example.data.local.SpeakingTopic
import com.example.data.local.entity.SpeakingSession
import com.example.data.local.entity.WeaknessItem
import com.example.data.remote.EvaluationResult
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.ScoreDial
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SpeakingSubTab(val title: String, val icon: ImageVector) {
    FLUENCY("Fluency & Pace", Icons.Default.Mic),
    PRONUNCIATION("Pronunciation Lab", Icons.Default.Psychology),
    NATURALIZER("Native Naturalizer", Icons.Default.AutoAwesome)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakingScreen(
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onNavigateToWeaknessLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeSubTab by remember { mutableStateOf(SpeakingSubTab.FLUENCY) }
    var selectedTopic by remember { mutableStateOf(DefaultData.speakingTopics.first()) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var transcriptText by remember { mutableStateOf("") }
    var isManualEditMode by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) }
    var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }
    var hasSavedToWeakness by remember { mutableStateOf(false) }
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionDeniedNotice = false
            startSpeechRecording(
                speechHelper = speechHelper,
                onStart = {
                    isRecording = true
                    recordingSeconds = 0
                    evaluationResult = null
                    hasSavedToWeakness = false
                },
                onTranscriptUpdate = { newText -> transcriptText = newText }
            )
        } else {
            permissionDeniedNotice = true
        }
    }

    // Timer effect while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_speaking_hero_1788577044196),
                        contentDescription = "Speaking fluency coach",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "ZERO-AUTH AI (KEYLESS)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pollinations.ai + Local STT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Real-Time Accent, Grammar & Fluency Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Speak for 20-60 seconds on a prompt. Get instant WPM pace, filler-word counts, and side-by-side corrected grammar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Interactive Mode Switcher
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpeakingSubTab.entries.forEach { tab ->
                        val isSelected = activeSubTab == tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeSubTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeSubTab == SpeakingSubTab.PRONUNCIATION) {
            item {
                PronunciationLabView(
                    repository = repository,
                    ttsHelper = ttsHelper,
                    speechHelper = speechHelper
                )
            }
        } else if (activeSubTab == SpeakingSubTab.NATURALIZER) {
            item {
                NativeNaturalizerView(
                    ttsHelper = ttsHelper,
                    speechHelper = speechHelper
                )
            }
        } else {
            // Topic Carousel
            item {
                Text(
                text = "Choose a Speaking Prompt",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(DefaultData.speakingTopics) { topic ->
                    val isSelected = topic.id == selectedTopic.id
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .clickable {
                                selectedTopic = topic
                                evaluationResult = null
                            }
                            .testTag("topic_${topic.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isSelected)
                            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                        else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = topic.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topic.prompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Active Prompt Display
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTopic.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedTopic.prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Speech Recording & Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRecording) {
                        Text(
                            text = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = RoseError
                        )
                        Text(
                            text = "Listening to your voice...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AudioVisualizerWave(
                            isListening = true,
                            amplitudeDb = 7f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = if (transcriptText.isNotBlank()) "Ready for AI Evaluation" else "Tap Mic to Start Speaking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (recordingSeconds > 0) "Spoken for $recordingSeconds seconds" else "Recommended: 20-45 seconds",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Large Mic Button
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isRecording) {
                                    // Stop recording
                                    speechHelper.stopListening()
                                    isRecording = false
                                } else {
                                    // Request microphone permission and record
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        startSpeechRecording(
                                            speechHelper = speechHelper,
                                            onStart = {
                                                isRecording = true
                                                recordingSeconds = 0
                                                evaluationResult = null
                                                hasSavedToWeakness = false
                                            },
                                            onTranscriptUpdate = { newText -> transcriptText = newText }
                                        )
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(76.dp)
                                .testTag("record_speech_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) RoseError else MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start speech recording",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Transcript Box
                    OutlinedTextField(
                        value = transcriptText,
                        onValueChange = { transcriptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transcript_input_field"),
                        label = { Text("Your Spoken Transcript") },
                        placeholder = { Text("Words will appear here as you speak, or you can type here to test...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (transcriptText.isNotBlank()) {
                                IconButton(onClick = { ttsHelper.speak(transcriptText) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Play transcript", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (transcriptText.isBlank()) {
                                    // Quick sample speech if user hasn't recorded
                                    transcriptText = "I am agree with this point because yesterday I go to the meeting and we have many informations. Um, like, basically I want to explain the architecture."
                                }
                                isEvaluating = true
                                coroutineScope.launch {
                                    val duration = if (recordingSeconds > 3) recordingSeconds else 25
                                    val result = repository.evaluateSpeech(
                                        topic = selectedTopic.title,
                                        transcript = transcriptText,
                                        durationSeconds = duration
                                    )
                                    evaluationResult = result
                                    isEvaluating = false

                                    // Save speaking session history
                                    repository.saveSpeakingSession(
                                        SpeakingSession(
                                            topic = selectedTopic.title,
                                            userTranscript = transcriptText,
                                            overallScore = result.overallScore,
                                            fluencyScore = result.fluencyScore,
                                            grammarScore = result.grammarScore,
                                            vocabScore = result.vocabScore,
                                            pronunciationScore = result.pronunciationScore,
                                            wordsPerMinute = result.wordsPerMinute,
                                            fillerWordCount = result.fillerWords.sumOf { it.count },
                                            feedbackSummary = result.executiveSummary
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("evaluate_speech_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isRecording && !isEvaluating
                        ) {
                            if (isEvaluating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...")
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Speech")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                transcriptText = "I believe remote work offers incredible flexibility, but in-person collaboration creates spontaneous ideas that are harder to replicate online."
                                recordingSeconds = 22
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sample Text", fontSize = 12.sp)
                        }
                    }

                    if (permissionDeniedNotice) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Microphone permission is needed for live speech. You can also type speech directly into the box above!",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseError,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // EVALUATION RESULT BREAKDOWN
        evaluationResult?.let { result ->
            item {
                Text(
                    text = "Speech Performance Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Score Dial
            item {
                ScoreDial(
                    overallScore = result.overallScore,
                    fluencyScore = result.fluencyScore,
                    grammarScore = result.grammarScore,
                    vocabScore = result.vocabScore,
                    pronunciationScore = result.pronunciationScore
                )
            }

            // Fluency & Pace Card (WPM & Filler Words)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "PACE & FLUENCY",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                text = "${result.wordsPerMinute} WPM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rhythm Rating: ${result.paceRating}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filler Words
                        Text(
                            text = "Detected Verbal Fillers:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (result.fillerWords.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zero filler words detected! Super clean flow.", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                result.fillerWords.forEach { filler ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RoseError.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "\"${filler.word}\" × ${filler.count}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Grammar & Syntax Corrections
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grammar & Syntax Enhancements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (result.grammarCorrections.isEmpty()) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${result.grammarCorrections.size} Notes",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.grammarCorrections.isEmpty()) EmeraldSuccess else RoseError
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (result.grammarCorrections.isEmpty()) {
                            Text(
                                text = "✨ Excellent grammatical structure! No noticeable syntax defects found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldSuccess
                            )
                        } else {
                            result.grammarCorrections.forEachIndexed { idx, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.errorCategory,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { ttsHelper.speak(item.correctedPhrase) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = "Hear correction", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "❌ You said: \"${item.originalPhrase}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RoseError
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "✅ Better: \"${item.correctedPhrase}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.ruleExplanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Better Word Choices (Vocabulary Enhancement)
            if (result.betterWordChoices.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vocabulary Enhancements",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "${result.betterWordChoices.size} Suggestions",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            result.betterWordChoices.forEach { choice ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "\"${choice.originalWord}\"",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = " ➔ ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "\"${choice.suggestedAlternative}\"",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { ttsHelper.speak(choice.suggestedAlternative) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.VolumeUp,
                                                    contentDescription = "Hear word suggestion",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = choice.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pronunciation & Clarity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pronunciation & Phonetic Focus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        result.pronunciationTips.forEach { tip ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tip.word}  ${tip.ipaPhonetic}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = tip.stressNote,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = tip.audioTip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { ttsHelper.speak(tip.word) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Hear audio", tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Executive Summary & Weakness Log Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Coach Roadmap:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.positivePraise,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.executiveSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val itemsToSave = result.grammarCorrections.map { corr ->
                                            WeaknessItem(
                                                category = corr.errorCategory,
                                                userSaid = corr.originalPhrase,
                                                correction = corr.correctedPhrase,
                                                explanation = corr.ruleExplanation,
                                                sourceSession = "Speaking: ${selectedTopic.title}"
                                            )
                                        } + result.fillerWords.map { filler ->
                                            WeaknessItem(
                                                category = "Filler Words",
                                                userSaid = "Spoke '${filler.word}' ${filler.count} times",
                                                correction = "Silent pause and breath",
                                                explanation = "Replace vocalized fillers with 1 second of calm silence.",
                                                sourceSession = "Speaking: ${selectedTopic.title}"
                                            )
                                        }
                                        if (itemsToSave.isNotEmpty()) {
                                            repository.insertWeaknesses(itemsToSave)
                                        }
                                        hasSavedToWeakness = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_to_weakness_button"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !hasSavedToWeakness
                            ) {
                                Icon(
                                    imageVector = if (hasSavedToWeakness) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (hasSavedToWeakness) "Saved to Weakness Log" else "Save to Weakness Log")
                            }

                            OutlinedButton(
                                onClick = onNavigateToWeaknessLog,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("View Log")
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private fun startSpeechRecording(
    speechHelper: SpeechRecognitionHelper,
    onStart: () -> Unit,
    onTranscriptUpdate: (String) -> Unit
) {
    onStart()
    speechHelper.startListening { finalResult ->
        onTranscriptUpdate(finalResult)
    }
}
