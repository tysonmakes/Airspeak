package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.audio.TextToSpeechHelper
import com.example.data.remote.AiEngine
import com.example.data.remote.AiEngineManager
import com.example.data.remote.AiRoutingMode
import com.example.data.remote.GeminiClient
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    aiEngineManager: AiEngineManager,
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentMode by aiEngineManager.currentRoutingMode.collectAsState()
    val activeEngine by aiEngineManager.currentActiveEngine.collectAsState()
    val allWeaknesses by repository.allWeaknesses.collectAsStateWithLifecycle(initialValue = emptyList())

    val settingsPrefs = remember {
        context.getSharedPreferences("airspeak_settings", Context.MODE_PRIVATE)
    }

    // Key input states
    var geminiKeyInput by remember {
        mutableStateOf(aiEngineManager.getCustomGeminiKey().ifBlank {
            if (GeminiClient.hasValidApiKey()) BuildConfig.GEMINI_API_KEY else ""
        })
    }
    var githubTokenInput by remember { mutableStateOf(aiEngineManager.getCustomGitHubToken()) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showGithubToken by remember { mutableStateOf(false) }

    // Latency testing states
    val latencyResults = remember { mutableStateMapOf<AiEngine, Long>() }
    val isTestingMap = remember { mutableStateMapOf<AiEngine, Boolean>() }

    // Audio & TTS pacing state (Default ~0.88x pacing per blueprint)
    var speechRate by remember {
        mutableFloatStateOf(settingsPrefs.getFloat("tts_pacing", 0.88f))
    }

    var showClearMistakesDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & AI Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Multi-Engine Pipeline • Voice Pacing • Mistake Bank",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9D4EDD),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14151B)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: AI MODEL & ENGINE SELECTION (BLUEPRINT MANDATE)
            item {
                Text(
                    text = "AI MODEL & ENGINE SELECTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC77DFF)
                )
            }

            // Overview card of current active setup
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF181A22)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Routing Strategy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentMode.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = currentMode.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentMode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Radio List of all 6 Routing Modes
            AiRoutingMode.values().forEach { mode ->
                item(key = "mode_${mode.id}") {
                    val isSelected = currentMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                aiEngineManager.setRoutingMode(mode)
                                Toast.makeText(context, "Active Mode: ${mode.title}", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("routing_mode_${mode.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF221F35) else Color(0xFF14151B)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF9D4EDD) else Color(0xFF262832)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { aiEngineManager.setRoutingMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFC77DFF),
                                    unselectedColor = Color(0xFF55596B)
                                )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Latency Tester Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LIVE LATENCY & ENGINE BENCHMARK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC77DFF)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14151B)),
                    border = BorderStroke(1.dp, Color(0xFF262832))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Test real-time round-trip latency to each provider endpoint:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        AiEngine.values().forEach { engine ->
                            val latency = latencyResults[engine]
                            val isTesting = isTestingMap[engine] == true

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = engine.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${engine.provider} • Expected: ${engine.speedTier}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFC77DFF)
                                    )
                                } else if (latency != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (latency >= 0) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (latency >= 0) "⚡ ${latency}ms" else "Offline / Err",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (latency >= 0) EmeraldSuccess else RoseError,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            isTestingMap[engine] = true
                                            coroutineScope.launch {
                                                val ms = aiEngineManager.testEngineLatency(engine)
                                                latencyResults[engine] = ms
                                                isTestingMap[engine] = false
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Ping", fontSize = 11.sp, color = Color(0xFFC77DFF))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: API KEYS & CREDENTIALS
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "API KEYS & CREDENTIALS CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC77DFF)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14151B)),
                    border = BorderStroke(1.dp, Color(0xFF262832))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Google Gemini 1.5 Flash API Key",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Used for sub-second primary calls & chapter grading. Key is stored locally in private SharedPreferences.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_key_input"),
                            placeholder = { Text("YOUR_GEMINI_API_KEY", color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF9D4EDD),
                                unfocusedBorderColor = Color(0xFF383B46)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    aiEngineManager.setCustomGeminiKey(geminiKeyInput)
                                    Toast.makeText(context, "Gemini API key saved!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_gemini_key_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    geminiKeyInput = ""
                                    aiEngineManager.setCustomGeminiKey("")
                                    Toast.makeText(context, "Custom key cleared", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // GitHub Models Token (Optional)
                        Text(
                            text = "GitHub Models Free Tier Token (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Used for GitHub Models REST endpoint (Phi-3 / Llama-3 / Mistral). Optional personal access token.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = githubTokenInput,
                            onValueChange = { githubTokenInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_token_input"),
                            placeholder = { Text("ghp_xxxxxxxxxxxx", color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = if (showGithubToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGithubToken = !showGithubToken }) {
                                    Icon(
                                        imageVector = if (showGithubToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF9D4EDD),
                                unfocusedBorderColor = Color(0xFF383B46)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                aiEngineManager.setCustomGitHubToken(githubTokenInput)
                                Toast.makeText(context, "GitHub Token saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_github_token_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D3A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save GitHub Token", color = Color.White)
                        }
                    }
                }
            }

            // SECTION 3: AUDIO & NEURAL VOICE SETTINGS
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AUDIO & VOICE LAYER (NEURAL TTS)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC77DFF)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14151B)),
                    border = BorderStroke(1.dp, Color(0xFF262832))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Neural Voice Profile",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Microsoft Edge Neural Voice (en-US-AnaNeural)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC77DFF)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Voice Pacing Slider (~0.88x pacing default)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice Pacing / Speech Rate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "${(speechRate * 100).roundToInt()}% (${if (speechRate in 0.85f..0.92f) "Recommended ~0.88x" else if (speechRate < 0.85f) "Slower" else "Faster"})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }

                        Slider(
                            value = speechRate,
                            onValueChange = {
                                speechRate = it
                                ttsHelper.setSpeechRate(it)
                                settingsPrefs.edit().putFloat("tts_pacing", it).apply()
                            },
                            valueRange = 0.70f..1.20f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFC77DFF),
                                activeTrackColor = Color(0xFF9D4EDD),
                                inactiveTrackColor = Color(0xFF2E313D)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                ttsHelper.speak("Hello! This is how I speak with Microsoft Edge Neural Voice at current pacing. Natural and clear!")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_pacing_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFC77DFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice Audio", color = Color(0xFFC77DFF))
                        }
                    }
                }
            }

            // SECTION 4: AUTOMATED MISTAKE BANK & STORAGE
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AUTOMATED MISTAKE BANK & DATA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC77DFF)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14151B)),
                    border = BorderStroke(1.dp, Color(0xFF262832))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Weakness Log Storage",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "100% Private on device (Android Room Local SQLite)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = AmberTertiary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${allWeaknesses.size}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = AmberTertiary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showClearMistakesDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_mistakes_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RoseError.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RoseError)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Weaknesses & Mistakes", color = RoseError)
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing all weaknesses
    if (showClearMistakesDialog) {
        AlertDialog(
            onDismissRequest = { showClearMistakesDialog = false },
            title = { Text("Clear Weakness Bank?") },
            text = { Text("This will remove all logged pronunciation and grammar mistakes from your local on-device database. You will start with a fresh slate.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.clearAllWeaknesses()
                            showClearMistakesDialog = false
                            Toast.makeText(context, "All mistakes cleared!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMistakesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
