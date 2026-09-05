package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RoleplayMessage
import com.example.data.local.entity.SpeakingSession
import com.example.data.local.entity.VocabularyWord
import com.example.data.local.entity.WeaknessItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_words ORDER BY level, id")
    fun getAllWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE level = :level ORDER BY id")
    fun getWordsByLevel(level: String): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE nextReviewTimestamp <= :now ORDER BY nextReviewTimestamp ASC")
    fun getDueWords(now: Long): Flow<List<VocabularyWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<VocabularyWord>)

    @Update
    suspend fun updateWord(word: VocabularyWord)

    @Query("SELECT COUNT(*) FROM vocabulary_words")
    suspend fun countWords(): Int
}

@Dao
interface WeaknessDao {
    @Query("SELECT * FROM weakness_items ORDER BY timestamp DESC")
    fun getAllWeaknesses(): Flow<List<WeaknessItem>>

    @Query("SELECT * FROM weakness_items WHERE isMastered = 0 ORDER BY timestamp DESC")
    fun getUnmasteredWeaknesses(): Flow<List<WeaknessItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakness(item: WeaknessItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeaknesses(items: List<WeaknessItem>)

    @Update
    suspend fun updateWeakness(item: WeaknessItem)

    @Query("DELETE FROM weakness_items WHERE id = :id")
    suspend fun deleteWeakness(id: Int)

    @Query("DELETE FROM weakness_items")
    suspend fun clearAllWeaknesses()

    @Query("SELECT COUNT(*) FROM weakness_items")
    suspend fun countWeaknesses(): Int
}

@Dao
interface SpeakingSessionDao {
    @Query("SELECT * FROM speaking_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SpeakingSession>>

    @Query("SELECT * FROM speaking_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<SpeakingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SpeakingSession): Long
}

@Dao
interface RoleplayDao {
    @Query("SELECT * FROM roleplay_messages WHERE scenarioId = :scenarioId ORDER BY timestamp ASC")
    fun getMessagesForScenario(scenarioId: String): Flow<List<RoleplayMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: RoleplayMessage)

    @Query("DELETE FROM roleplay_messages WHERE scenarioId = :scenarioId")
    suspend fun clearScenario(scenarioId: String)
}
