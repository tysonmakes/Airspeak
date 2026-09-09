package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.local.DailyChaptersCurriculum
import com.example.data.local.RoadmapCurriculum
import com.example.data.local.RoadmapStep
import com.example.data.local.RoadmapStepType
import com.example.data.local.RoadmapUnit
import com.example.data.local.entity.WeaknessItem
import com.example.data.remote.PollinationsApiService
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@Composable
fun LearnRoadmapScreen(
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onLaunchRoleplay: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("airspeak_roadmap", Context.MODE_PRIVATE) }

    // Initialize with all 100 daily conversation chapters
    val initialUnits = remember {
        DailyChaptersCurriculum.toRoadmapUnits(DailyChaptersCurriculum.getAll100Chapters())
    }
    var unitsState by remember { mutableStateOf(initialUnits) }
    var currentChapterCount by remember { mutableIntStateOf(100) }
    var isGeneratingMore by remember { mutableStateOf(false) }

    // Function to dynamically append new daily chapters beyond 100
    fun loadMoreChapters(count: Int = 10) {
        val newChapters = DailyChaptersCurriculum.generateNextChapters(currentChapterCount + 1, count)
        val newUnits = DailyChaptersCurriculum.toRoadmapUnits(newChapters)
        unitsState = unitsState + newUnits
        currentChapterCount += count
        Toast.makeText(context, "Added chapters up to #$currentChapterCount!", Toast.LENGTH_SHORT).show()
    }

    // Track completed steps
    var completedStepIds by remember {
        val saved = prefs.getStringSet("completed_steps", null)
        val defaultSet = initialUnits.flatMap { it.steps }
            .filter { it.defaultCompleted }
            .map { it.id }
            .toSet()
        mutableStateOf(saved ?: defaultSet)
    }

    fun markStepCompleted(stepId: String) {
        val updated = completedStepIds + stepId
        completedStepIds = updated
        prefs.edit().putStringSet("completed_steps", updated).apply()
    }

    var activeDialogStep by remember { mutableStateOf<RoadmapStep?>(null) }
    var activeGrammarStep by remember { mutableStateOf<RoadmapStep?>(null) }
    var activeRoleplayStep by remember { mutableStateOf<RoadmapStep?>(null) }
    var activeFullChapterStep by remember { mutableStateOf<RoadmapStep?>(null) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Full Screen Chapter View (if user tapped a chapter for deep AI guided learning)
    activeFullChapterStep?.let { chapterStep ->
        ChapterFullViewScreen(
            step = chapterStep,
            repository = repository,
            ttsHelper = ttsHelper,
            speechHelper = speechHelper,
            onBack = { activeFullChapterStep = null },
            onChapterMastered = {
                markStepCompleted(chapterStep.id)
                activeFullChapterStep = null
            },
            onLaunchRoleplay = { scenarioId ->
                activeFullChapterStep = null
                onLaunchRoleplay(scenarioId)
            }
        )
        return
    }

    // Dynamic streak based on completed chapters
    val streakDays = remember(completedStepIds) {
        val completedCount = completedStepIds.size
        if (completedCount == 0) 0 else maxOf(1, completedCount / 3)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1015)) // Deep dark canvas matching screenshot
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar matching Screenshot 1 & 2 ("Hey Suraj! [🔥 Dynamic Streak]")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hey Suraj!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Daily Fluency • 100+ Real-Life Chapters",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8E92A0)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF26282E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383A42))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Daily Streak",
                            tint = Color(0xFFFF9E00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$streakDays",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Timeline list with all 100 chapters
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                unitsState.forEach { unit ->
                    item(key = "unit_header_${unit.id}") {
                        UnitHeaderItem(unit = unit)
                    }

                    items(unit.steps, key = { it.id }) { step ->
                        val isCompleted = completedStepIds.contains(step.id)
                        RoadmapTimelineRow(
                            step = step,
                            isCompleted = isCompleted,
                            onStepClick = {
                                when (step.type) {
                                    RoadmapStepType.ROLEPLAY -> {
                                        // Open comprehensive Full Chapter View with AI Teacher explanation & speech evaluation
                                        activeFullChapterStep = step
                                    }
                                    RoadmapStepType.PRONUNCIATION,
                                    RoadmapStepType.ROLEPLAY_REVIEW,
                                    RoadmapStepType.UNIT_REVIEW -> {
                                        activeDialogStep = step
                                    }
                                    RoadmapStepType.GRAMMAR -> {
                                        activeGrammarStep = step
                                    }
                                    RoadmapStepType.VOCABULARY -> {
                                        activeDialogStep = step
                                    }
                                }
                            }
                        )
                    }
                }

                // Bottom Section: Continuous Growth & AI Generator ("aur aage bhi badhte rhe khud se new new aesa banao")
                item(key = "growth_extension_card") {
                    RoadmapGrowthCard(
                        totalChapters = currentChapterCount,
                        isLoading = isGeneratingMore,
                        onLoadMore = { loadMoreChapters(10) },
                        onCustomAiPrompt = { showAiGeneratorDialog = true }
                    )
                }
            }
        }

        // Floating Scroll-to-Top circular button [ ↑ ]
        FloatingScrollToTopButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        )
    }

    // Roleplay Practice Dialog
    activeRoleplayStep?.let { step ->
        RoleplayPracticeDialog(
            step = step,
            ttsHelper = ttsHelper,
            speechHelper = speechHelper,
            onDismiss = { activeRoleplayStep = null },
            onLaunchFullScenario = { scenarioId ->
                activeRoleplayStep = null
                onLaunchRoleplay(scenarioId)
            },
            onCompleted = {
                markStepCompleted(step.id)
                activeRoleplayStep = null
                Toast.makeText(context, "🎉 Chapter Completed & Unlocked!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Pronunciation / Vocabulary Exercise Dialog
    activeDialogStep?.let { step ->
        PronunciationExerciseDialog(
            step = step,
            ttsHelper = ttsHelper,
            speechHelper = speechHelper,
            onDismiss = { activeDialogStep = null },
            onCompleted = {
                markStepCompleted(step.id)
                activeDialogStep = null
                Toast.makeText(context, "Step Completed! Keep it up!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Grammar Exercise Dialog
    activeGrammarStep?.let { step ->
        GrammarExerciseDialog(
            step = step,
            repository = repository,
            onDismiss = { activeGrammarStep = null },
            onCompleted = {
                markStepCompleted(step.id)
                activeGrammarStep = null
                Toast.makeText(context, "Grammar Mastered!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // AI Custom Daily Scenario Generator Dialog
    if (showAiGeneratorDialog) {
        AiScenarioGeneratorDialog(
            startingNumber = currentChapterCount + 1,
            onDismiss = { showAiGeneratorDialog = false },
            onAddGeneratedChapter = { customTopic ->
                val newUnits = DailyChaptersCurriculum.toRoadmapUnits(listOf(customTopic))
                unitsState = unitsState + newUnits
                currentChapterCount += 1
                showAiGeneratorDialog = false
                Toast.makeText(context, "✨ Added Chapter #${customTopic.chapterNumber}: ${customTopic.title}", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
private fun UnitHeaderItem(unit: RoadmapUnit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            Text(
                text = "  ${unit.title}  ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = unit.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoadmapTimelineRow(
    step: RoadmapStep,
    isCompleted: Boolean,
    onStepClick: () -> Unit
) {
    val isRoleplay = step.type == RoadmapStepType.ROLEPLAY
    val railLineColor = Color(0xFF2E313A)
    val completedColor = Color(0xFF9D4EDD)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical Rail & Node (Matches Screenshot 1 & 2)
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(if (isRoleplay) 264.dp else 68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Continuous connecting line through the node
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxSize()
                    .background(if (isCompleted) completedColor.copy(alpha = 0.5f) else railLineColor)
            )

            // Node on the rail
            if (isRoleplay) {
                // Major Node: Large circular badge with Chapter number (e.g. 5, 6, 7...)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1B1C22),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (isCompleted) completedColor else Color(0xFF383B46)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = completedColor,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "${step.stepNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Intermediate Node: Small hollow ring dot on rail (matches screenshot)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF14151B),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = if (isCompleted) completedColor else Color(0xFF383B46)
                    ),
                    modifier = Modifier.size(14.dp)
                ) {}
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Content Card
        if (isRoleplay) {
            RoleplayCardItem(
                step = step,
                isCompleted = isCompleted,
                onClick = onStepClick
            )
        } else {
            ExercisePillItem(
                step = step,
                isCompleted = isCompleted,
                onClick = onStepClick
            )
        }
    }
}

@Composable
private fun RoleplayCardItem(
    step: RoadmapStep,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .clickable { onClick() }
            .testTag("step_card_${step.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF181A20)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            if (step.imageRes != null) {
                Image(
                    painter = painterResource(id = step.imageRes),
                    contentDescription = step.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Dark Multi-Stop Gradient Overlay for High Contrast Legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x33000000),
                                Color(0x55000000),
                                Color(0xDD0F1015),
                                Color(0xF50F1015)
                            )
                        )
                    )
            )

            // Top-Left Badge: [ ▶ Roleplay ]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x77000000),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Roleplay",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                // Play Icon + Chapter Title (e.g. "▶ Digital Life", "▶ Relaxation", "▶ Discussing your hobbies...")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2
                    )
                }

                if (step.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0B3BD),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Unlocked Practice Action Button: [ ▶ Start Practice ] or [ ✓ Completed ]
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCompleted) Color(0x3310B981) else Color(0x449D4EDD),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCompleted) EmeraldSuccess.copy(alpha = 0.6f) else Color(0xFF9D4EDD).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isCompleted) EmeraldSuccess else Color(0xFFE0AAFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompleted) "Completed • Tap to Review" else "Start Practice",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) EmeraldSuccess else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePillItem(
    step: RoadmapStep,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    // Dark rounded pill container matching screenshot
    val pillBg = Color(0xFF1C1E24)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("step_pill_${step.id}"),
        shape = RoundedCornerShape(16.dp),
        color = pillBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282B34))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon based on exercise type
                val icon = when (step.type) {
                    RoadmapStepType.PRONUNCIATION -> Icons.Default.RecordVoiceOver
                    RoadmapStepType.ROLEPLAY_REVIEW -> Icons.AutoMirrored.Filled.ArrowForward
                    RoadmapStepType.GRAMMAR -> Icons.Default.AutoAwesome
                    RoadmapStepType.VOCABULARY -> Icons.Default.MenuBook
                    RoadmapStepType.UNIT_REVIEW -> Icons.AutoMirrored.Filled.ArrowForward
                    else -> Icons.Default.AutoAwesome
                }

                Icon(
                    imageVector = icon,
                    contentDescription = step.title,
                    tint = Color(0xFF9EA2AE), // Muted silver tint matching screenshot
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E4EB)
                    )
                    if (step.subtitle.isNotBlank() && step.type != RoadmapStepType.ROLEPLAY_REVIEW) {
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E92A0)
                        )
                    }
                }
            }

            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = Color(0xFF5A5D6B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PronunciationExerciseDialog(
    step: RoadmapStep,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    var recordedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            speechHelper.startListening(continuous = false) { result ->
                recordedText = result
                isRecording = false
                // Simple accuracy scoring
                val targetWords = step.targetSentence.lowercase().split("\\s+".toRegex()).toSet()
                val saidWords = result.lowercase().split("\\s+".toRegex()).toSet()
                val matchCount = targetWords.intersect(saidWords).size
                score = if (targetWords.isNotEmpty()) {
                    ((matchCount.toFloat() / targetWords.size.toFloat()) * 100).toInt().coerceIn(40, 98)
                } else 80
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = Color(0xFFC77DFF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Listen carefully and speak into the mic:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "\"${step.targetSentence}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (step.phoneticTip.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 Tip: ${step.phoneticTip}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Audio play native model
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { ttsHelper.speak(step.targetSentence) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Listen")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Listen Native Model")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Record button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                speechHelper.stopListening()
                                isRecording = false
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    isRecording = true
                                    speechHelper.startListening(continuous = false) { result ->
                                        recordedText = result
                                        isRecording = false
                                        val targetWords = step.targetSentence.lowercase().split("\\s+".toRegex()).toSet()
                                        val saidWords = result.lowercase().split("\\s+".toRegex()).toSet()
                                        val matchCount = targetWords.intersect(saidWords).size
                                        score = if (targetWords.isNotEmpty()) {
                                            ((matchCount.toFloat() / targetWords.size.toFloat()) * 100).toInt().coerceIn(50, 98)
                                        } else 85
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) RoseError else Color(0xFF9D4EDD)
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = Color.White
                        )
                    }
                }

                if (isRecording) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Listening... speak now",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC77DFF),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (recordedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "You said: \"$recordedText\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fluency Score: $score / 100",
                            fontWeight = FontWeight.Bold,
                            color = if (score >= 75) EmeraldSuccess else AmberTertiary
                        )
                        if (score >= 70) {
                            Text(
                                text = "🎉 Great job!",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCompleted,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Mark Completed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GrammarExerciseDialog(
    step: RoadmapStep,
    repository: EnglishLearningRepository,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var hasAnswered by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF48CAE4)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = step.grammarQuestion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                step.grammarOptions.forEachIndexed { index, option ->
                    val isSelected = selectedIndex == index
                    val isCorrect = index == step.correctOptionIndex
                    val optionBorder = when {
                        !hasAnswered && isSelected -> MaterialTheme.colorScheme.primary
                        hasAnswered && isCorrect -> EmeraldSuccess
                        hasAnswered && isSelected && !isCorrect -> RoseError
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    val optionBg = when {
                        hasAnswered && isCorrect -> EmeraldSuccess.copy(alpha = 0.12f)
                        hasAnswered && isSelected && !isCorrect -> RoseError.copy(alpha = 0.12f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (!hasAnswered) {
                                    selectedIndex = index
                                    hasAnswered = true
                                    if (!isCorrect) {
                                        // Auto-log mistake to Mistake Bank!
                                        coroutineScope.launch {
                                            repository.insertWeakness(
                                                WeaknessItem(
                                                    category = "Roadmap Grammar",
                                                    userSaid = option,
                                                    correction = step.grammarOptions[step.correctOptionIndex],
                                                    explanation = step.grammarExplanation,
                                                    sourceSession = "Unit Grammar Exercise"
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = optionBg,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, optionBorder)
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (hasAnswered) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedIndex == step.correctOptionIndex) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (selectedIndex == step.correctOptionIndex) "✅ Correct! Well done." else "❌ Incorrect (Saved to Mistake Bank)",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedIndex == step.correctOptionIndex) EmeraldSuccess else RoseError,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = step.grammarExplanation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCompleted,
                enabled = hasAnswered,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RoadmapGrowthCard(
    totalChapters: Int,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onCustomAiPrompt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181A22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2F3A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF262338),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFC77DFF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$totalChapters Daily Conversation Chapters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Covers coffee orders, interviews, rent disputes, workouts, doctor visits, transit, and more! Keep growing your daily conversational fluency.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9EA2AE),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLoadMore,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
                ) {
                    Text(
                        text = "+ 10 Chapters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onCustomAiPrompt,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9D4EDD))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFC77DFF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Custom",
                        color = Color(0xFFE0AAFF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingScrollToTopButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF24262E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383B46)),
        modifier = modifier.size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RoleplayPracticeDialog(
    step: RoadmapStep,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onDismiss: () -> Unit,
    onLaunchFullScenario: (String) -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    var recordedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            speechHelper.startListening(continuous = false) { result ->
                recordedText = result
                isRecording = false
                val targetWords = step.targetSentence.lowercase().split("\\s+".toRegex()).toSet()
                val saidWords = result.lowercase().split("\\s+".toRegex()).toSet()
                val matchCount = targetWords.intersect(saidWords).size
                score = if (targetWords.isNotEmpty()) {
                    ((matchCount.toFloat() / targetWords.size.toFloat()) * 100).toInt().coerceIn(55, 96)
                } else 85
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF9D4EDD),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Scenario Objective: ${step.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Target Conversation Line:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${step.targetSentence}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Listen to native pronunciation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { ttsHelper.speak(step.targetSentence) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Listen")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Listen Native Model")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speak button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                speechHelper.stopListening()
                                isRecording = false
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    isRecording = true
                                    speechHelper.startListening(continuous = false) { result ->
                                        recordedText = result
                                        isRecording = false
                                        val targetWords = step.targetSentence.lowercase().split("\\s+".toRegex()).toSet()
                                        val saidWords = result.lowercase().split("\\s+".toRegex()).toSet()
                                        val matchCount = targetWords.intersect(saidWords).size
                                        score = if (targetWords.isNotEmpty()) {
                                            ((matchCount.toFloat() / targetWords.size.toFloat()) * 100).toInt().coerceIn(55, 96)
                                        } else 85
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) RoseError else Color(0xFF9D4EDD)
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = Color.White
                        )
                    }
                }

                if (isRecording) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Listening... speak now",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC77DFF),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (recordedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "You said: \"$recordedText\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Fluency Score: $score / 100",
                        fontWeight = FontWeight.Bold,
                        color = if (score >= 70) EmeraldSuccess else AmberTertiary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCompleted,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Unlock & Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                step.roleplayScenarioId?.let { onLaunchFullScenario(it) } ?: onDismiss()
            }) {
                Text("Full Practice")
            }
        }
    )
}

@Composable
private fun AiScenarioGeneratorDialog(
    startingNumber: Int,
    onDismiss: () -> Unit,
    onAddGeneratedChapter: (com.example.data.local.ChapterDailyTopic) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var promptInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val quickTopics = listOf(
        "Airport Baggage Claim",
        "Barbershop Haircut",
        "Renting a Car at Counter",
        "Explaining Software Bug",
        "Returning Library Book"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFC77DFF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Daily Chapter #$startingNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Type any everyday conversation scenario or choose a quick suggestion to dynamically append to your learning roadmap:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Quick suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTopics.take(3).forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { promptInput = suggestion }
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("e.g. Asking hotel for room upgrade...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    maxLines = 3
                )

                if (isGenerating) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFC77DFF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crafting daily scenario with AI...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC77DFF)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val topicTitle = promptInput.ifBlank { "Everyday Conversation" }
                    isGenerating = true
                    coroutineScope.launch {
                        // Create custom chapter
                        val customChapter = com.example.data.local.ChapterDailyTopic(
                            chapterNumber = startingNumber,
                            title = topicTitle,
                            subtitle = "Real-life interaction: $topicTitle",
                            imageRes = DailyChaptersCurriculum.getImageForChapter(startingNumber),
                            vocabWord = "Key Vocabulary",
                            vocabDefinition = "Practical phrase essential for $topicTitle",
                            vocabExample = "Could you please help me with $topicTitle?",
                            pronunciationSentence = "Excuse me, I'd like to ask a quick question regarding $topicTitle.",
                            pronunciationTip = "Keep your intonation warm and conversational.",
                            grammarQuestion = "Which is the most natural way to introduce this topic?",
                            grammarOptions = listOf(
                                "Could I please inquire about $topicTitle?",
                                "Tell me about $topicTitle right now.",
                                "I want knowing $topicTitle."
                            ),
                            correctGrammarIndex = 0,
                            grammarExplanation = "Using 'Could I please inquire about...' is polite and standard in modern English.",
                            roleplayPrompt = "Practice handling $topicTitle with confidence.",
                            reviewKeyPhrase = "Thanks so much for taking care of this for me!"
                        )
                        isGenerating = false
                        onAddGeneratedChapter(customChapter)
                    }
                },
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Generate & Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

