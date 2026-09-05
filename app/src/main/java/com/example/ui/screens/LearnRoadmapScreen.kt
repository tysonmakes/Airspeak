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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
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
import com.example.data.local.RoadmapCurriculum
import com.example.data.local.RoadmapStep
import com.example.data.local.RoadmapStepType
import com.example.data.local.RoadmapUnit
import com.example.data.local.entity.WeaknessItem
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

    // Track completed steps
    var completedStepIds by remember {
        val saved = prefs.getStringSet("completed_steps", null)
        val defaultSet = RoadmapCurriculum.units.flatMap { it.steps }
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
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar matching Screenshot 1 & 2
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Daily Fluency Roadmap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AmberTertiary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberTertiary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Daily Streak",
                        tint = AmberTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = AmberTertiary
                    )
                }
            }
        }

        // Timeline list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            RoadmapCurriculum.units.forEach { unit ->
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
                                    step.roleplayScenarioId?.let { onLaunchRoleplay(it) }
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
        }
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
    val completedColor = Color(0xFF9D4EDD) // Vibrant purple matching screenshot
    val uncompletedColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical rail + Node circle
        Box(
            modifier = Modifier
                .width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vertical connecting line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (step.type == RoadmapStepType.ROLEPLAY) 180.dp else 68.dp)
                    .background(if (isCompleted) completedColor.copy(alpha = 0.6f) else uncompletedColor)
            )

            // Circle Node
            Surface(
                shape = CircleShape,
                color = if (isCompleted) completedColor else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = if (isCompleted) completedColor else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${step.stepNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Content Card
        if (step.type == RoadmapStepType.ROLEPLAY) {
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
            .clickable { onClick() }
            .testTag("step_card_${step.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B2E) // Dark indigo/purple card like screenshot
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            // Background Image
            if (step.imageRes != null) {
                Image(
                    painter = painterResource(id = step.imageRes),
                    contentDescription = step.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color(0xFF13111C).copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Top Badge
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
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
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
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
    val pillBg = Color(0xFF242038) // Sleek rounded container matching screenshot
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("step_pill_${step.id}"),
        shape = RoundedCornerShape(16.dp),
        color = pillBg,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    RoadmapStepType.VOCABULARY -> Icons.Default.School
                    RoadmapStepType.UNIT_REVIEW -> Icons.AutoMirrored.Filled.ArrowForward
                    else -> Icons.Default.AutoAwesome
                }
                val iconTint = when (step.type) {
                    RoadmapStepType.PRONUNCIATION -> Color(0xFFC77DFF)
                    RoadmapStepType.GRAMMAR -> Color(0xFF48CAE4)
                    RoadmapStepType.VOCABULARY -> AmberTertiary
                    else -> Color(0xFFE0AAFF)
                }

                Icon(
                    imageVector = icon,
                    contentDescription = step.title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (step.subtitle.isNotBlank()) {
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
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
            speechHelper.startListening { result ->
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
                                    speechHelper.startListening { result ->
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
