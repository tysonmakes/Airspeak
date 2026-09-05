package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.RoleplayScenario
import com.example.data.local.entity.RoleplayMessage
import com.example.data.local.entity.SpeakingSession
import com.example.data.local.entity.VocabularyWord
import com.example.data.local.entity.WeaknessItem
import com.example.data.remote.AiEvaluationService
import com.example.data.remote.EvaluationResult
import com.example.data.remote.RoleplayTurnResult
import com.example.data.remote.TargetedDrill
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class EnglishLearningRepository(
    private val database: AppDatabase,
    private val aiService: AiEvaluationService = AiEvaluationService()
) {
    private val vocabDao = database.vocabularyDao()
    private val weaknessDao = database.weaknessDao()
    private val speakingDao = database.speakingSessionDao()
    private val roleplayDao = database.roleplayDao()

    // Vocabulary & SRS
    val allWords: Flow<List<VocabularyWord>> = vocabDao.getAllWords()

    fun getWordsByLevel(level: String): Flow<List<VocabularyWord>> = vocabDao.getWordsByLevel(level)

    fun getDueWords(): Flow<List<VocabularyWord>> = vocabDao.getDueWords(System.currentTimeMillis())

    suspend fun updateWordSrs(word: VocabularyWord, rating: SrsRating) {
        val now = System.currentTimeMillis()
        val newIntervalDays: Int
        val newMastery: Int

        when (rating) {
            SrsRating.AGAIN -> {
                newIntervalDays = 1
                newMastery = maxOf(0, word.masteryLevel - 1)
            }
            SrsRating.GOOD -> {
                newIntervalDays = if (word.intervalDays <= 1) 3 else (word.intervalDays * 1.8).toInt()
                newMastery = minOf(5, word.masteryLevel + 1)
            }
            SrsRating.EASY -> {
                newIntervalDays = if (word.intervalDays <= 1) 6 else (word.intervalDays * 2.5).toInt()
                newMastery = minOf(5, word.masteryLevel + 2)
            }
        }

        val nextReview = now + TimeUnit.DAYS.toMillis(newIntervalDays.toLong())
        val updated = word.copy(
            intervalDays = newIntervalDays,
            masteryLevel = newMastery,
            nextReviewTimestamp = nextReview
        )
        vocabDao.updateWord(updated)
    }

    suspend fun toggleBookmark(word: VocabularyWord) {
        vocabDao.updateWord(word.copy(isBookmarked = !word.isBookmarked))
    }

    // Weakness Log & Correction Bank
    val allWeaknesses: Flow<List<WeaknessItem>> = weaknessDao.getAllWeaknesses()
    val unmasteredWeaknesses: Flow<List<WeaknessItem>> = weaknessDao.getUnmasteredWeaknesses()

    suspend fun insertWeakness(item: WeaknessItem) = weaknessDao.insertWeakness(item)

    suspend fun insertWeaknesses(items: List<WeaknessItem>) = weaknessDao.insertWeaknesses(items)

    suspend fun toggleWeaknessMastered(item: WeaknessItem) {
        weaknessDao.updateWeakness(
            item.copy(
                isMastered = !item.isMastered,
                practiceCount = item.practiceCount + 1
            )
        )
    }

    suspend fun deleteWeakness(id: Int) = weaknessDao.deleteWeakness(id)

    suspend fun clearAllWeaknesses() = weaknessDao.clearAllWeaknesses()

    suspend fun logWeakness(
        originalMistake: String,
        correctedForm: String,
        category: String,
        explanation: String
    ) {
        val item = WeaknessItem(
            userSaid = originalMistake,
            correction = correctedForm,
            category = category,
            explanation = explanation,
            practiceCount = 0,
            isMastered = false,
            timestamp = System.currentTimeMillis()
        )
        weaknessDao.insertWeakness(item)
    }

    // Speaking Sessions
    val allSessions: Flow<List<SpeakingSession>> = speakingDao.getAllSessions()

    suspend fun saveSpeakingSession(session: SpeakingSession): Long = speakingDao.insertSession(session)

    suspend fun evaluateSpeech(topic: String, transcript: String, durationSeconds: Int): EvaluationResult {
        return aiService.evaluateSpeech(topic, transcript, durationSeconds)
    }

    // Roleplay Chat
    fun getRoleplayMessages(scenarioId: String): Flow<List<RoleplayMessage>> =
        roleplayDao.getMessagesForScenario(scenarioId)

    suspend fun saveRoleplayMessage(message: RoleplayMessage) = roleplayDao.insertMessage(message)

    suspend fun clearRoleplayScenario(scenarioId: String) = roleplayDao.clearScenario(scenarioId)

    suspend fun generateRoleplayReply(
        scenario: RoleplayScenario,
        history: List<RoleplayMessage>,
        userMessage: String
    ): RoleplayTurnResult {
        return aiService.generateRoleplayReply(scenario, history, userMessage)
    }

    fun generateDrills(weaknesses: List<WeaknessItem>): List<TargetedDrill> {
        return aiService.generateTargetedDrills(weaknesses)
    }

    suspend fun fetchAndSaveDynamicVocabulary(level: String): Int {
        val words = aiService.generateDynamicVocabulary(level)
        if (words.isNotEmpty()) {
            vocabDao.insertAll(words)
            return words.size
        }
        return 0
    }
}

enum class SrsRating {
    AGAIN, // 1 day
    GOOD,  // 3-5 days
    EASY   // 7+ days
}
