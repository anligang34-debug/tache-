package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frequency_tasks")
data class FrequencyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val frequencyDays: Int,
    val lastCompletedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Helper to check if task is overdue
    fun isOverdue(): Boolean {
        val lastTime = lastCompletedTimestamp ?: return true
        val elapsedDays = (System.currentTimeMillis() - lastTime) / (1000 * 60 * 60 * 24)
        return elapsedDays >= frequencyDays
    }

    // Helper to calculate when it should be done next
    fun getNextDueDateMillis(): Long {
        val lastTime = lastCompletedTimestamp ?: return createdAt
        return lastTime + (frequencyDays * 24L * 60L * 60L * 1000L)
    }

    // Days elapsed since last completion (or since creation if never done)
    fun daysElapsed(): Int {
        val referenceTime = lastCompletedTimestamp ?: createdAt
        return ((System.currentTimeMillis() - referenceTime) / (1000L * 60 * 60 * 24)).toInt()
    }
}

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey val dateString: String, // Format: YYYY-MM-DD
    val moodRating: Int, // 1 to 5 (1: Très triste, 2: Triste, 3: Neutre, 4: Joyeux, 5: Radieux)
    val gratitudeNote: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val billingCycle: String, // "Mensuel", "Annuel", "Hebdomadaire"
    val paymentDay: Int, // Jour du prélèvement (ex: 15)
    val category: String, // "Loisirs", "Santé", "Travail", "Autre"
    val currency: String = "€",
    val active: Boolean = true
)
