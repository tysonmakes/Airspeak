package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.RoleplayDao
import com.example.data.local.dao.SpeakingSessionDao
import com.example.data.local.dao.VocabularyDao
import com.example.data.local.dao.WeaknessDao
import com.example.data.local.entity.RoleplayMessage
import com.example.data.local.entity.SpeakingSession
import com.example.data.local.entity.VocabularyWord
import com.example.data.local.entity.WeaknessItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        VocabularyWord::class,
        WeaknessItem::class,
        SpeakingSession::class,
        RoleplayMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun weaknessDao(): WeaknessDao
    abstract fun speakingSessionDao(): SpeakingSessionDao
    abstract fun roleplayDao(): RoleplayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airspeak_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            // Populate essential vocabulary dictionary, but leave personal mistake bank clean for user
                            database.vocabularyDao().insertAll(DefaultData.initialWords)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
