package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.TextToSpeechHelper
import com.example.data.local.CallSender
import com.example.data.local.PracticeTopic
import com.example.data.local.PracticeTopicCatalog
import com.example.data.local.TopicChatMessage
import com.example.data.local.entity.WeaknessItem
import com.example.data.remote.AiEngineManager
import com.example.data.remote.PollinationsApiService
import com.example.data.repository.EnglishLearningRepository
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@Composable
fun TopicChatPracticeScreen(
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper
) {
    var activeTopic by remember { mutableStateOf<PracticeTopic?>(null) }

    if (activeTopic != null) {
        ActiveTopicChatSession(
            topic = activeTopic!!,
            repository = repository,
            ttsHelper = ttsHelper,
            speechHelper = speechHelper,
            onBack = { activeTopic = null }
        )
    } else {
        TopicCatalogLobby(
            onSelectTopic = { topic ->
                activeTopic = topic
            }
        )
    }
}

@Composable
private fun TopicCatalogLobby(
    onSelectTopic: (PracticeTopic) -> Unit
) {
    val topicsByCategory = remember {
        PracticeTopicCatalog.topics.groupBy { it.category }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E17)),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header matching Screenshot 3
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Our Picks for You!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = { /* History */ },
                    modifier = Modifier.testTag("topic_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Hero Banner: "Pick a topic and start chatting!"
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF232136)
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(115.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pick a topic and\nstart chatting!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type text or send real-time voice notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC77DFF)
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.img_topic_chat_banner_1788579477775),
                            contentDescription = "Topic Banner",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Section: Introduction
        topicsByCategory["Introduction"]?.let { introTopics ->
            item {
                SectionHeader("Introduction")
            }
            items(introTopics) { topic ->
                IntroductionTopicRow(topic = topic, onClick = { onSelectTopic(topic) })
            }
        }

        // Section: Free Topics
        topicsByCategory["Free Topics"]?.let { freeTopics ->
            item {
                SectionHeader("Free Topics", starCount = 2)
            }
            items(freeTopics) { topic ->
                CircleAvatarTopicRow(topic = topic, onClick = { onSelectTopic(topic) })
            }
        }

        // Section: Job Interview
        topicsByCategory["Job Interview"]?.let { jobTopics ->
            item {
                SectionHeader("Job Interview", starCount = 3)
            }
            items(jobTopics) { topic ->
                CircleAvatarTopicRow(topic = topic, onClick = { onSelectTopic(topic) })
            }
        }

        // Section: Meetings
        topicsByCategory["Meetings"]?.let { meetingTopics ->
            item {
                SectionHeader("Meetings", starCount = 2)
            }
            items(meetingTopics) { topic ->
                CircleAvatarTopicRow(topic = topic, onClick = { onSelectTopic(topic) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, starCount: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (starCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AmberTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .background(AmberTertiary, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun IntroductionTopicRow(
    topic: PracticeTopic,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("topic_item_${topic.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Special green-ring avatar matching screenshot 3
        Box(
            modifier = Modifier
                .size(70.dp)
                .border(2.5.dp, EmeraldSuccess, CircleShape)
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = topic.iconRes),
                contentDescription = topic.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            // Star badge
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberTertiary,
                        modifier = Modifier.size(10.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberTertiary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldSuccess
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = topic.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CircleAvatarTopicRow(
    topic: PracticeTopic,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("topic_item_${topic.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(3.dp)
        ) {
            Image(
                painter = painterResource(id = topic.iconRes),
                contentDescription = topic.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (topic.starRating > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(topic.starRating.coerceAtMost(2)) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AmberTertiary,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = topic.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveTopicChatSession(
    topic: PracticeTopic,
    repository: EnglishLearningRepository,
    ttsHelper: TextToSpeechHelper,
    speechHelper: SpeechRecognitionHelper,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiEngineManager = remember { AiEngineManager(context) }
    val currentEngine by aiEngineManager.currentEngine.collectAsState()

    val messages = remember {
        mutableStateListOf(
            TopicChatMessage(
                sender = CallSender.AI,
                text = topic.initialAiGreeting
            )
        )
    }

    var textInput by remember { mutableStateOf("") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isAiGenerating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Speak initial AI greeting
    LaunchedEffect(topic.id) {
        ttsHelper.speak(topic.initialAiGreeting)
    }

    fun handleSend(userText: String, isAudio: Boolean) {
        if (userText.isBlank()) return
        textInput = ""

        // Add user message
        val userMsg = TopicChatMessage(
            sender = CallSender.USER,
            text = userText,
            isAudioInput = isAudio
        )
        messages.add(userMsg)
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }

        isAiGenerating = true
        coroutineScope.launch {
            val historyString = messages.takeLast(6).joinToString("\n") {
                "${if (it.sender == CallSender.USER) "Learner" else "Coach Emma"}: ${it.text}"
            }

            // Analyze user grammar & generate reply using active AI Engine
            val turn = aiEngineManager.generateLiveCallTurn(
                tutorName = "Coach Emma",
                tutorPersona = "Friendly, encouraging English tutor on topic: ${topic.title}",
                userSpokenText = userText,
                callTopic = topic.title,
                conversationHistory = historyString,
                targetEngine = currentEngine
            )

            isAiGenerating = false

            // Update user message with feedback if grammar was imperfect
            if (!turn.liveCorrection.isNullOrBlank()) {
                val index = messages.indexOf(userMsg)
                if (index >= 0) {
                    messages[index] = userMsg.copy(
                        grammarCorrection = turn.liveCorrection,
                        grammarReason = turn.livePraise,
                        betterAlternative = null,
                        fluencyScore = turn.fluencyScore
                    )
                }
            }

            // Add AI reply
            val aiMsg = TopicChatMessage(
                sender = CallSender.AI,
                text = turn.spokenReply
            )
            messages.add(aiMsg)
            ttsHelper.speak(turn.spokenReply)

            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecordingAudio = true
            speechHelper.startListening { recognized ->
                isRecordingAudio = false
                handleSend(recognized, isAudio = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isAiGenerating) "AI is analyzing & typing..." else "Online • Voice & Text Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAiGenerating) AmberTertiary else EmeraldSuccess
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { ttsHelper.stop() }) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Mute/Audio")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Input bar with text input and microphone button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isRecordingAudio) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎙️ Listening to your voice... Speak now",
                                color = RoseError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Type message or use mic...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("topic_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mic Button
                        Button(
                            onClick = {
                                if (isRecordingAudio) {
                                    speechHelper.stopListening()
                                    isRecordingAudio = false
                                } else {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                        == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        isRecordingAudio = true
                                        speechHelper.startListening { recognized ->
                                            isRecordingAudio = false
                                            handleSend(recognized, isAudio = true)
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingAudio) RoseError else Color(0xFF9D4EDD)
                            ),
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Record Voice",
                                tint = Color.White
                            )
                        }

                        if (textInput.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { handleSend(textInput, isAudio = false) },
                                modifier = Modifier.testTag("topic_chat_send")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                TopicChatMessageBubble(
                    message = msg,
                    onPlayTts = { ttsHelper.speak(msg.text) },
                    onBookmarkMistake = { corr, reason ->
                        coroutineScope.launch {
                            repository.insertWeakness(
                                WeaknessItem(
                                    category = "Topic Chat Grammar",
                                    userSaid = msg.text,
                                    correction = corr,
                                    explanation = reason,
                                    sourceSession = "Chat on ${topic.title}"
                                )
                            )
                            Toast.makeText(context, "Saved to Mistake Bank!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isAiGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing grammar & replying...",
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
}

@Composable
private fun TopicChatMessageBubble(
    message: TopicChatMessage,
    onPlayTts: () -> Unit,
    onBookmarkMistake: (correction: String, reason: String) -> Unit
) {
    val isUser = message.sender == CallSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) Color(0xFF7B2CBF) else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isUser && message.isAudioInput) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Audio input",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Spoken Audio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onPlayTts,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Play voice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Grammar Feedback Card under user message
        if (isUser && message.grammarCorrection != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberTertiary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberTertiary.copy(alpha = 0.4f)),
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Better Way:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AmberTertiary
                        )
                        IconButton(
                            onClick = {
                                onBookmarkMistake(
                                    message.grammarCorrection,
                                    message.grammarReason ?: "Grammar refinement"
                                )
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save to Mistake Bank",
                                tint = AmberTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "\"${message.grammarCorrection}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!message.grammarReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message.grammarReason,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
