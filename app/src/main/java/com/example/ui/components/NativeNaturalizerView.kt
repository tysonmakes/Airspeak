package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.TextToSpeechHelper
import com.example.data.remote.NativeNaturalizerService
import com.example.data.remote.NaturalizedOutput
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@Composable
fun NativeNaturalizerView(
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val naturalizerService = remember { NativeNaturalizerService() }

    var inputText by remember { mutableStateOf("I am agree with your opinion.") }
    var isNaturalizing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<NaturalizedOutput?>(null) }
    var isListeningInput by remember { mutableStateOf(false) }

    // Practice drill state
    var practiceTarget by remember { mutableStateOf<String?>(null) }
    var isPracticeListening by remember { mutableStateOf(false) }
    var practiceResultText by remember { mutableStateOf("") }
    var practiceScore by remember { mutableStateOf<Int?>(null) }

    val starterSentences = listOf(
        "I am agree with your opinion",
        "Sorry I late because heavy traffic",
        "Can I ask you a question please?",
        "I don't understand what you mean",
        "Please help me in my work"
    )

    val inputPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListeningInput = true
            speechHelper.startListening(continuous = false) { res ->
                isListeningInput = false
                if (res.isNotBlank()) inputText = res.trim()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice typing", Toast.LENGTH_SHORT).show()
        }
    }

    val practicePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val target = practiceTarget ?: return@rememberLauncherForActivityResult
            isPracticeListening = true
            practiceResultText = ""
            practiceScore = null
            speechHelper.startListening(continuous = false) { res ->
                isPracticeListening = false
                practiceResultText = res.trim()
                practiceScore = computeSentenceMatch(target, res)
            }
        } else {
            Toast.makeText(context, "Microphone permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = IndigoPrimary.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = IndigoPrimary,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Native Naturalizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Speak or type any sentence. Gemini upgrades it into 3 authentic native phrasing levels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Starter Chips
        Text(
            text = "Try Common Non-Native Sentences:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(starterSentences) { starter ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        inputText = starter
                    }
                ) {
                    Text(
                        text = starter,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Input Field
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Enter a sentence to naturalize...") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            if (isListeningInput) {
                                speechHelper.stopListening()
                                isListeningInput = false
                            } else {
                                val hasAudioPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudioPerm) {
                                    isListeningInput = true
                                    speechHelper.startListening(continuous = false) { res ->
                                        isListeningInput = false
                                        if (res.isNotBlank()) inputText = res.trim()
                                    }
                                } else {
                                    inputPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isListeningInput) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = if (isListeningInput) RoseError else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Transform Button
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    isNaturalizing = true
                    coroutineScope.launch {
                        result = naturalizerService.naturalize(inputText)
                        isNaturalizing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            enabled = inputText.isNotBlank() && !isNaturalizing,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
            if (isNaturalizing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Analyzing Native Phrasing...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transform to Native English", fontWeight = FontWeight.Bold)
            }
        }

        // Output Results
        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val res = result ?: return@AnimatedVisibility
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Coach Rationale Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberTertiary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberTertiary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Coach Breakdown",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = res.coachTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Level 1: Casual Daily
                NaturalLevelCard(
                    title = "Casual Daily Native",
                    badge = "Conversational",
                    badgeColor = EmeraldSuccess,
                    sentence = res.casual,
                    ttsHelper = ttsHelper,
                    onPractice = {
                        practiceTarget = res.casual
                        practiceResultText = ""
                        practiceScore = null
                    }
                )

                // Level 2: Workplace Pro
                NaturalLevelCard(
                    title = "Workplace & Professional",
                    badge = "Corporate & Meetings",
                    badgeColor = IndigoPrimary,
                    sentence = res.workplace,
                    ttsHelper = ttsHelper,
                    onPractice = {
                        practiceTarget = res.workplace
                        practiceResultText = ""
                        practiceScore = null
                    }
                )

                // Level 3: Idiomatic Expression
                NaturalLevelCard(
                    title = "Idiomatic Power Phrasing",
                    badge = "Advanced Native",
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    sentence = res.idiomatic,
                    ttsHelper = ttsHelper,
                    onPractice = {
                        practiceTarget = res.idiomatic
                        practiceResultText = ""
                        practiceScore = null
                    }
                )

                // Active Practice Drill Card
                practiceTarget?.let { target ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🎙️ Speaking Practice Drill",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "\"$target\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { ttsHelper.speak(target) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Hear Audio")
                                }

                                Button(
                                    onClick = {
                                        if (isPracticeListening) {
                                            speechHelper.stopListening()
                                            isPracticeListening = false
                                        } else {
                                            val hasAudioPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasAudioPerm) {
                                                isPracticeListening = true
                                                practiceResultText = ""
                                                practiceScore = null
                                                speechHelper.startListening(continuous = false) { resSpoken ->
                                                    isPracticeListening = false
                                                    practiceResultText = resSpoken.trim()
                                                    practiceScore = computeSentenceMatch(target, resSpoken)
                                                }
                                            } else {
                                                practicePermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPracticeListening) RoseError else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPracticeListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPracticeListening) "Listening..." else "Speak This")
                                }
                            }

                            practiceScore?.let { score ->
                                val scoreColor = if (score >= 75) EmeraldSuccess else AmberTertiary
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = scoreColor.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = scoreColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$score% Match • ${if (score >= 80) "Splendid natural rhythm!" else "Good effort, listen again to polish!"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = scoreColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NaturalLevelCard(
    title: String,
    badge: String,
    badgeColor: Color,
    sentence: String,
    ttsHelper: TextToSpeechHelper,
    onPractice: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Text(
                text = "\"$sentence\"",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 22.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { ttsHelper.speak(sentence) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalButton(
                    onClick = onPractice,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Practice", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun computeSentenceMatch(target: String, spoken: String): Int {
    val cleanTargetWords = target.lowercase().replace(Regex("[^a-z0-9 ]"), "").split("\\s+".toRegex()).filter { it.isNotEmpty() }
    val cleanSpokenWords = spoken.lowercase().replace(Regex("[^a-z0-9 ]"), "").split("\\s+".toRegex()).filter { it.isNotEmpty() }

    if (cleanTargetWords.isEmpty()) return 100
    if (cleanSpokenWords.isEmpty()) return 0

    var matched = 0
    for (tw in cleanTargetWords) {
        if (cleanSpokenWords.contains(tw)) {
            matched++
        }
    }
    val score = (matched.toDouble() / cleanTargetWords.size.toDouble()) * 100.0
    return score.coerceIn(0.0, 100.0).toInt()
}
