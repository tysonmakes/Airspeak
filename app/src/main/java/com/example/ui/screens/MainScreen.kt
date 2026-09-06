package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.TextToSpeechHelper
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiClient
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess

import androidx.compose.material.icons.filled.Settings
import com.example.data.remote.AiEngineManager

sealed interface AppOverlay {
    data class Roleplay(val scenarioId: String? = null) : AppOverlay
    data object Vocabulary : AppOverlay
    data object StreakStats : AppOverlay
    data object Settings : AppOverlay
}

sealed class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Learn : NavigationTab("Learn", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_learn")
    data object Practice : NavigationTab("Practice", Icons.Filled.Forum, Icons.Outlined.Forum, "nav_practice")
    data object Call : NavigationTab("Call", Icons.Filled.Call, Icons.Outlined.Call, "nav_call")
    data object Speaking : NavigationTab("Speaking", Icons.Filled.Mic, Icons.Outlined.Mic, "nav_speaking")
    data object Mistakes : NavigationTab("Mistakes", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver, "nav_mistakes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current

    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { EnglishLearningRepository(database) }
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val aiEngineManager = remember { AiEngineManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
            speechHelper.shutdown()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var overlayScreen by remember { mutableStateOf<AppOverlay?>(null) }
    val tabs = listOf(
        NavigationTab.Learn,
        NavigationTab.Practice,
        NavigationTab.Call,
        NavigationTab.Speaking,
        NavigationTab.Mistakes
    )

    BackHandler(enabled = overlayScreen != null) {
        overlayScreen = null
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    val hasGeminiKey = remember { GeminiClient.hasValidApiKey() }

    val roadmapPrefs = remember { context.getSharedPreferences("airspeak_roadmap", Context.MODE_PRIVATE) }
    val completedSteps = remember { roadmapPrefs.getStringSet("completed_steps", emptySet()) ?: emptySet() }
    val dynamicStreak = remember(completedSteps) {
        if (completedSteps.isEmpty()) 0 else maxOf(1, completedSteps.size / 3)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "AirSpeak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (dynamicStreak == 0) "Beginner • Day 1" else "Streak • ${dynamicStreak}d",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberTertiary.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { overlayScreen = AppOverlay.StreakStats }
                            .testTag("streak_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Daily streak",
                                tint = AmberTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${dynamicStreak}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = AmberTertiary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            ttsHelper.stop()
                            speechHelper.stopListening()
                            overlayScreen = AppOverlay.Vocabulary
                        },
                        modifier = Modifier.testTag("top_vocab_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Vocabulary Vault & SRS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            ttsHelper.stop()
                            speechHelper.stopListening()
                            overlayScreen = AppOverlay.Settings
                        },
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & AI Selector",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("app_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About AirSpeak",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (overlayScreen == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTabIndex = index
                                ttsHelper.stop()
                                speechHelper.stopListening()
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val overlay = overlayScreen) {
            is AppOverlay.Roleplay -> {
                RoleplayScreen(
                    repository = repository,
                    ttsHelper = ttsHelper,
                    speechHelper = speechHelper,
                    initialScenarioId = overlay.scenarioId,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onBack = { overlayScreen = null }
                )
            }
            is AppOverlay.Vocabulary -> {
                VocabularyScreen(
                    repository = repository,
                    ttsHelper = ttsHelper,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onBack = { overlayScreen = null }
                )
            }
            is AppOverlay.Settings -> {
                SettingsScreen(
                    aiEngineManager = aiEngineManager,
                    repository = repository,
                    ttsHelper = ttsHelper,
                    onBack = { overlayScreen = null }
                )
            }
            else -> {
                Crossfade(
                    targetState = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    label = "tab_crossfade"
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> LearnRoadmapScreen(
                            repository = repository,
                            ttsHelper = ttsHelper,
                            speechHelper = speechHelper,
                            onLaunchRoleplay = { scenarioId ->
                                overlayScreen = AppOverlay.Roleplay(scenarioId)
                            }
                        )
                        1 -> TopicChatPracticeScreen(
                            repository = repository,
                            ttsHelper = ttsHelper,
                            speechHelper = speechHelper
                        )
                        2 -> LiveCallScreen(
                            repository = repository,
                            ttsHelper = ttsHelper,
                            speechHelper = speechHelper,
                            onNavigateToWeaknessLog = { selectedTabIndex = 4 }
                        )
                        3 -> SpeakingScreen(
                            repository = repository,
                            ttsHelper = ttsHelper,
                            speechHelper = speechHelper,
                            onNavigateToWeaknessLog = { selectedTabIndex = 4 }
                        )
                        4 -> WeaknessLogScreen(
                            repository = repository,
                            ttsHelper = ttsHelper
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "About AirSpeak AI",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "AirSpeak synthesizes the core strengths of Stimuler and Airlearn into one cohesive experience:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Stimuler-Engine: Live voice speech evaluation analyzing pace (WPM), filler-word detection ('um/uh'), grammar correction side-by-side, and pronunciation clarity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Interactive Voice-Chat: 5 roleplay scenarios (Interview, Cafe, Airport, Debate, Casual) with real-time spoken AI replies and instant coaching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Airlearn-Style Vocabulary: Beginner, Intermediate & Advanced daily word sets, 3D flip flashcards, and Spaced Repetition (SRS) mastery tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Mistake Bank & Mini-Drills: Auto-logs grammar errors into your personal Weakness Log and creates targeted quizzes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (hasGeminiKey)
                            "✅ Gemini 3.5 Flash is active."
                        else
                            "⚡ Using built-in offline linguistic analysis. To enable Gemini Flash cloud evaluation, add GEMINI_API_KEY in the AI Studio Secrets panel.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasGeminiKey) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (overlayScreen == AppOverlay.StreakStats) {
        StreakStatsDialog(
            currentStreak = dynamicStreak,
            completedStepsCount = completedSteps.size,
            onDismiss = { overlayScreen = null },
            onOpenVocab = { overlayScreen = AppOverlay.Vocabulary },
            onOpenRoleplay = { overlayScreen = AppOverlay.Roleplay() }
        )
    }
}

@Composable
fun StreakStatsDialog(
    currentStreak: Int,
    completedStepsCount: Int,
    onDismiss: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenRoleplay: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = AmberTertiary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentStreak == 0) "Start Your Daily Streak" else "$currentStreak-Day Learning Streak",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (currentStreak == 0)
                        "Complete your first chapter in the Learn tab to ignite your daily fluency streak!"
                    else
                        "Consistent daily speaking builds fluency 3x faster. Keep up the momentum!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Week days indicators
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        val activeDaysCount = minOf(7, currentStreak)
                        days.forEachIndexed { idx, day ->
                            val isCompleted = idx < activeDaysCount
                            val isToday = idx == (activeDaysCount.coerceAtLeast(1) - 1)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) AmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.surface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily targets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current Progress",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Curriculum Chapters Completed", style = MaterialTheme.typography.bodySmall)
                            Text("$completedStepsCount completed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressFraction = (completedStepsCount % 10) / 10f
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = EmeraldSuccess
                        )
                    }
                }

                // Quick Launch Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenVocab,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Learn Vocab", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onOpenRoleplay,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Roleplay", fontSize = 12.sp)
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
