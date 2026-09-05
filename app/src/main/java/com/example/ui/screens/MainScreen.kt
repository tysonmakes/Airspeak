package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
            speechHelper.shutdown()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavigationTab.Learn,
        NavigationTab.Practice,
        NavigationTab.Call,
        NavigationTab.Speaking,
        NavigationTab.Mistakes
    )

    var showInfoDialog by remember { mutableStateOf(false) }
    val hasGeminiKey = remember { GeminiClient.hasValidApiKey() }

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
                                    text = "CEFR B2 • Active",
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
                        modifier = Modifier.padding(start = 12.dp)
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
                                text = "5d",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = AmberTertiary
                            )
                        }
                    }
                },
                actions = {
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
    ) { innerPadding ->
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
                    onLaunchRoleplay = { _ -> selectedTabIndex = 1 }
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
}
