package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.TextToSpeechHelper
import com.example.data.local.entity.VocabularyWord
import com.example.data.repository.EnglishLearningRepository
import com.example.data.repository.SrsRating
import com.example.ui.components.WordFlashcard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@Composable
fun VocabularyScreen(
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allWords by repository.allWords.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Flashcards & SRS, 1: Quick Quiz, 2: Word Library
    var selectedLevel by remember { mutableStateOf("All") } // "All", "Beginner", "Intermediate", "Advanced"
    var searchQuery by remember { mutableStateOf("") }

    val filteredWords = allWords.filter { word ->
        (selectedLevel == "All" || word.level.equals(selectedLevel, ignoreCase = true)) &&
        (searchQuery.isBlank() || word.word.contains(searchQuery, ignoreCase = true) || word.definition.contains(searchQuery, ignoreCase = true))
    }

    var flashcardIndex by remember { mutableIntStateOf(0) }
    var isGeneratingWords by remember { mutableStateOf(false) }
    var generationMessage by remember { mutableStateOf<String?>(null) }
    val currentFlashcardWord = if (filteredWords.isNotEmpty()) {
        filteredWords[flashcardIndex % filteredWords.size]
    } else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        painter = painterResource(id = R.drawable.img_vocab_hero_1788577063475),
                        contentDescription = "Vocabulary builder",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "ZERO-AUTH VOCAB ENGINE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Keyless AI & Local SQLite SRS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Daily Smart Vocabulary & Retention",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Level-based high-impact words with native audio pronunciation, flip flashcards, and spaced memory intervals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Mode Navigation Tabs: Flashcards, Quiz, Library
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
                            Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SRS Cards")
                        }
                    },
                    modifier = Modifier.testTag("tab_srs_cards")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quick Quiz")
                        }
                    },
                    modifier = Modifier.testTag("tab_quick_quiz")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Word Bank")
                        }
                    },
                    modifier = Modifier.testTag("tab_word_bank")
                )
            }
        }

        // Level Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val levels = listOf("All", "Beginner", "Intermediate", "Advanced")
                items(levels) { lvl ->
                    val isSelected = selectedLevel == lvl
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable {
                                selectedLevel = lvl
                                flashcardIndex = 0
                            }
                            .testTag("level_filter_$lvl")
                    ) {
                        Text(
                            text = lvl,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // TAB 0: FLASHCARD & SRS MODE
        if (selectedTab == 0) {
            item {
                if (currentFlashcardWord != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Card ${(flashcardIndex % filteredWords.size) + 1} of ${filteredWords.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                Text(
                                    text = "Tap card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        WordFlashcard(
                            word = currentFlashcardWord,
                            onSpeak = { text -> ttsHelper.speak(text) },
                            onRateSrs = { rating ->
                                coroutineScope.launch {
                                    repository.updateWordSrs(currentFlashcardWord, rating)
                                    flashcardIndex++
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (flashcardIndex > 0) flashcardIndex--
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Previous")
                            }

                            Button(
                                onClick = {
                                    flashcardIndex++
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Next Card")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic 5-Word Generator (Keyless AI)
                        FilledTonalButton(
                            onClick = {
                                coroutineScope.launch {
                                    isGeneratingWords = true
                                    generationMessage = null
                                    val targetLevel = if (selectedLevel == "All") "Intermediate" else selectedLevel
                                    val added = repository.fetchAndSaveDynamicVocabulary(targetLevel)
                                    isGeneratingWords = false
                                    generationMessage = if (added > 0) {
                                        "Saved $added dynamic $targetLevel words locally in Room SQLite!"
                                    } else {
                                        "Loaded today's 5 core vocabulary words into local database."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_dynamic_words_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isGeneratingWords
                        ) {
                            if (isGeneratingWords) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating with Keyless AI...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate 5 New Words (Keyless AI)")
                            }
                        }

                        generationMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No vocabulary words match this level.")
                    }
                }
            }
        }

        // TAB 1: QUICK QUIZ (SRS TESTING)
        if (selectedTab == 1) {
            item {
                QuizSection(
                    words = filteredWords.ifEmpty { allWords },
                    ttsHelper = ttsHelper
                )
            }
        }

        // TAB 2: WORD BANK & SEARCH
        if (selectedTab == 2) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocab_search_field"),
                    placeholder = { Text("Search vocabulary, definitions...") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            items(filteredWords) { word ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_item_${word.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = word.phonetic,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (word.level) {
                                        "Beginner" -> EmeraldSuccess.copy(alpha = 0.15f)
                                        "Intermediate" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = word.level,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (word.level) {
                                            "Beginner" -> EmeraldSuccess
                                            "Intermediate" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.tertiary
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { ttsHelper.speak(word.word) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Pronounce",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = word.definition,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${word.exampleSentence}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizSection(
    words: List<VocabularyWord>,
    ttsHelper: TextToSpeechHelper
) {
    if (words.size < 4) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Please ensure at least 4 words are available to take the quiz.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }

    val currentTarget = words[questionIndex % words.size]
    val options = remember(questionIndex) {
        val wrongOptions = words.filter { it.id != currentTarget.id }.shuffled().take(3).map { it.definition }
        (wrongOptions + currentTarget.definition).shuffled()
    }
    val correctIndex = options.indexOf(currentTarget.definition)

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
                Text(
                    text = "Question ${(questionIndex % words.size) + 1} of ${words.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Which of the following best defines the word:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentTarget.word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { ttsHelper.speak(currentTarget.word) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Hear word", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = "${currentTarget.phonetic} • ${currentTarget.partOfSpeech}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4 Options
            options.forEachIndexed { idx, optionText ->
                val isSelected = selectedOptionIndex == idx
                val optionColor = when {
                    !isAnswerSubmitted -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    idx == correctIndex -> EmeraldSuccess.copy(alpha = 0.2f)
                    isSelected && idx != correctIndex -> RoseError.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = optionColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(enabled = !isAnswerSubmitted) {
                            selectedOptionIndex = idx
                        }
                        .testTag("quiz_option_$idx")
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
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAnswerSubmitted) {
                            if (idx == correctIndex) {
                                Icon(Icons.Default.Check, contentDescription = "Correct", tint = EmeraldSuccess)
                            } else if (isSelected && idx != correctIndex) {
                                Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = RoseError)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isAnswerSubmitted) {
                Button(
                    onClick = {
                        if (selectedOptionIndex != null) {
                            isAnswerSubmitted = true
                            if (selectedOptionIndex == correctIndex) {
                                score++
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_quiz_answer"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedOptionIndex != null
                ) {
                    Text("Check Answer")
                }
            } else {
                Button(
                    onClick = {
                        questionIndex++
                        selectedOptionIndex = null
                        isAnswerSubmitted = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("next_quiz_question"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next Question")
                }
            }
        }
    }
}
