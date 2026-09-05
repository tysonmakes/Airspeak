package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.TextToSpeechHelper
import com.example.data.local.entity.WeaknessItem
import com.example.data.remote.TargetedDrill
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.components.WeaknessCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@Composable
fun WeaknessLogScreen(
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allWeaknesses by repository.allWeaknesses.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Personal Weakness Log, 1: Targeted Mini-Drills
    var selectedCategory by remember { mutableStateOf("All") } // "All", "Grammar", "Pronunciation", "Filler Words"
    var showOnlyNeedsPractice by remember { mutableStateOf(false) }

    val filteredList = allWeaknesses.filter { item ->
        (selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)) &&
        (!showOnlyNeedsPractice || !item.isMastered)
    }

    val masteredCount = allWeaknesses.count { it.isMastered }
    val totalCount = allWeaknesses.size
    val masteryProgress = if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary & Mastery Progress Header
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "CORRECTION BANK",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Personal Weakness Log",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$masteredCount/$totalCount",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Mastery Progress: ${(masteryProgress * 100).toInt()}% of logged errors conquered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { masteryProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // View Tabs: Log vs Mini-Drills
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mistake Bank")
                        }
                    },
                    modifier = Modifier.testTag("tab_mistake_bank")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Targeted Mini-Drills")
                        }
                    },
                    modifier = Modifier.testTag("tab_targeted_drills")
                )
            }
        }

        // TAB 0: MISTAKE BANK LIST
        if (selectedTab == 0) {
            // Category Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("All", "Grammar", "Pronunciation", "Filler Words")
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                                .testTag("cat_filter_$cat")
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 Clean Slate!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No errors logged in this category. Complete speaking tests or voice chats to auto-log mistakes.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList) { item ->
                    WeaknessCard(
                        item = item,
                        onToggleMastered = {
                            coroutineScope.launch {
                                repository.toggleWeaknessMastered(item)
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                repository.deleteWeakness(item.id)
                            }
                        },
                        onSpeak = { text -> ttsHelper.speak(text) },
                        modifier = Modifier.testTag("weakness_item_${item.id}")
                    )
                }
            }
        }

        // TAB 1: TARGETED MINI-DRILLS
        if (selectedTab == 1) {
            item {
                MiniDrillsSection(
                    drills = repository.generateDrills(allWeaknesses),
                    ttsHelper = ttsHelper
                )
            }
        }
    }
}

@Composable
fun MiniDrillsSection(
    drills: List<TargetedDrill>,
    ttsHelper: TextToSpeechHelper
) {
    var drillIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var drillScore by remember { mutableIntStateOf(0) }

    if (drills.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "No targeted drills generated yet. Practice speaking to auto-generate personalized drills!",
                modifier = Modifier.padding(20.dp)
            )
        }
        return
    }

    val currentDrill = drills[drillIndex % drills.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "TARGETED MINI-DRILL ${(drillIndex % drills.size) + 1}/${drills.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Drill Score: $drillScore",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = currentDrill.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RoseError.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Original weakness: \"${currentDrill.incorrectSentence}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoseError,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            currentDrill.options.forEachIndexed { idx, opt ->
                val isSelected = selectedOption == idx
                val bgColor = when {
                    !isSubmitted -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    idx == currentDrill.correctIndex -> EmeraldSuccess.copy(alpha = 0.2f)
                    isSelected && idx != currentDrill.correctIndex -> RoseError.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(enabled = !isSubmitted) {
                            selectedOption = idx
                        }
                        .testTag("drill_opt_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + idx)}. ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSubmitted) {
                            if (idx == currentDrill.correctIndex) {
                                Icon(Icons.Default.Check, contentDescription = "Correct", tint = EmeraldSuccess)
                            } else if (isSelected && idx != currentDrill.correctIndex) {
                                Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = RoseError)
                            }
                        }
                    }
                }
            }

            if (isSubmitted) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 ${currentDrill.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { ttsHelper.speak(currentDrill.options[currentDrill.correctIndex]) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Hear native correction", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!isSubmitted) {
                Button(
                    onClick = {
                        if (selectedOption != null) {
                            isSubmitted = true
                            if (selectedOption == currentDrill.correctIndex) {
                                drillScore++
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_drill_answer"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedOption != null
                ) {
                    Text("Check Answer")
                }
            } else {
                Button(
                    onClick = {
                        drillIndex++
                        selectedOption = null
                        isSubmitted = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("next_drill_question"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next Targeted Drill")
                }
            }
        }
    }
}
