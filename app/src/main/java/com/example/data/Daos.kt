package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FrequencyTaskDao {
    @Query("SELECT * FROM frequency_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<FrequencyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: FrequencyTask)

    @Update
    suspend fun updateTask(task: FrequencyTask)

    @Delete
    suspend fun deleteTask(task: FrequencyTask)

    @Query("UPDATE frequency_tasks SET lastCompletedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateCompletion(id: Int, timestamp: Long)
}

@Dao
interface MoodEntryDao {
    @Query("SELECT * FROM mood_entries ORDER BY dateString DESC")
    fun getAllMoodEntries(): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries WHERE dateString = :dateString LIMIT 1")
    suspend fun getMoodEntryByDate(dateString: String): MoodEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntry)

    @Delete
    suspend fun deleteMoodEntry(entry: MoodEntry)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY name ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
}
