package com.example.data

import kotlinx.coroutines.flow.Flow

class RituelRepository(private val db: AppDatabase) {
    val frequencyTasks: Flow<List<FrequencyTask>> = db.frequencyTaskDao().getAllTasks()
    val moodEntries: Flow<List<MoodEntry>> = db.moodEntryDao().getAllMoodEntries()
    val subscriptions: Flow<List<Subscription>> = db.subscriptionDao().getAllSubscriptions()

    // Frequency Tasks operations
    suspend fun insertTask(task: FrequencyTask) = db.frequencyTaskDao().insertTask(task)
    suspend fun updateTask(task: FrequencyTask) = db.frequencyTaskDao().updateTask(task)
    suspend fun deleteTask(task: FrequencyTask) = db.frequencyTaskDao().deleteTask(task)
    suspend fun markTaskCompleted(id: Int, timestamp: Long) = db.frequencyTaskDao().updateCompletion(id, timestamp)

    // Mood & Gratitude operations
    suspend fun insertMoodEntry(entry: MoodEntry) = db.moodEntryDao().insertMoodEntry(entry)
    suspend fun getMoodEntryByDate(dateString: String): MoodEntry? = db.moodEntryDao().getMoodEntryByDate(dateString)
    suspend fun deleteMoodEntry(entry: MoodEntry) = db.moodEntryDao().deleteMoodEntry(entry)

    // Subscriptions operations
    suspend fun insertSubscription(sub: Subscription) = db.subscriptionDao().insertSubscription(sub)
    suspend fun deleteSubscription(sub: Subscription) = db.subscriptionDao().deleteSubscription(sub)
}
