package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_words")
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val level: String, // "Beginner", "Intermediate", "Advanced"
    val definition: String,
    val exampleSentence: String,
    val synonyms: String,
    val antonyms: String,
    val masteryLevel: Int = 0, // 0 to 5
    val nextReviewTimestamp: Long = System.currentTimeMillis(),
    val intervalDays: Int = 1,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "weakness_items")
data class WeaknessItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Grammar", "Pronunciation", "Filler Words", "Pacing"
    val userSaid: String,
    val correction: String,
    val explanation: String,
    val phoneticTip: String = "",
    val sourceSession: String = "Speaking Practice",
    val timestamp: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false,
    val practiceCount: Int = 0
)

@Entity(tableName = "speaking_sessions")
data class SpeakingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val userTranscript: String,
    val overallScore: Int,
    val fluencyScore: Int,
    val grammarScore: Int,
    val vocabScore: Int,
    val pronunciationScore: Int,
    val wordsPerMinute: Int,
    val fillerWordCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val feedbackSummary: String
)

@Entity(tableName = "roleplay_messages")
data class RoleplayMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scenarioId: String,
    val sender: String, // "user", "ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val feedbackSnippet: String? = null
)
