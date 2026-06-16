package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RituelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = RituelRepository(db)

    // Reactive StateFlow sources
    val tasks: StateFlow<List<FrequencyTask>> = repository.frequencyTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val moodEntries: StateFlow<List<MoodEntry>> = repository.moodEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subscriptions: StateFlow<List<Subscription>> = repository.subscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Navigation and Calendar State
    private val _currentYear = MutableStateFlow(LocalDate.now().year)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(LocalDate.now().monthValue)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    init {
        // Pre-populate data if the tables are completely empty
        viewModelScope.launch {
            prepopulateIfEmpty()
        }
    }

    private suspend fun prepopulateIfEmpty() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        // 1. Prepopulate Frequency Tasks
        val initialTasks = repository.frequencyTasks.first()
        if (initialTasks.isEmpty()) {
            val nowMs = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L

            repository.insertTask(
                FrequencyTask(
                    name = "Arroser les plantes d'intérieur",
                    frequencyDays = 7,
                    lastCompletedTimestamp = nowMs - (4 * oneDayMs) // Done 4 days ago (green)
                )
            )
            repository.insertTask(
                FrequencyTask(
                    name = "Nettoyer les filtres de climatisation",
                    frequencyDays = 90,
                    lastCompletedTimestamp = nowMs - (12 * oneDayMs) // Done 12 days ago (green)
                )
            )
            repository.insertTask(
                FrequencyTask(
                    name = "Laver la voiture de fond en comble",
                    frequencyDays = 30,
                    lastCompletedTimestamp = nowMs - (32 * oneDayMs) // Done 32 days ago (Overdue / Yellow)
                )
            )
            repository.insertTask(
                FrequencyTask(
                    name = "Tirer les sauvegardes de sécurité",
                    frequencyDays = 5,
                    lastCompletedTimestamp = nowMs - (6 * oneDayMs) // Done 6 days ago (Overdue / Red)
                )
            )
            repository.insertTask(
                FrequencyTask(
                    name = "Tondre la pelouse et tailler",
                    frequencyDays = 14,
                    lastCompletedTimestamp = null // Never done (Overdue / Red)
                )
            )
        }

        // 2. Prepopulate Subscriptions
        val initialSubs = repository.subscriptions.first()
        if (initialSubs.isEmpty()) {
            repository.insertSubscription(Subscription(name = "Netflix Premium", price = 19.99, billingCycle = "Mensuel", paymentDay = 15, category = "Loisirs"))
            repository.insertSubscription(Subscription(name = "Spotify Premium", price = 10.99, billingCycle = "Mensuel", paymentDay = 5, category = "Loisirs"))
            repository.insertSubscription(Subscription(name = "Salles de Sport (Gym)", price = 29.90, billingCycle = "Mensuel", paymentDay = 1, category = "Santé"))
            repository.insertSubscription(Subscription(name = "SaaS Adobe Creative Cloud", price = 34.50, billingCycle = "Mensuel", paymentDay = 24, category = "Travail"))
            repository.insertSubscription(Subscription(name = "Assurance Mobile SFAM", price = 4.90, billingCycle = "Mensuel", paymentDay = 8, category = "Autre"))
        }

        // 3. Prepopulate Mood/Gratitude logs
        val initialMoods = repository.moodEntries.first()
        if (initialMoods.isEmpty()) {
            repository.insertMoodEntry(MoodEntry(today.minusDays(1).format(formatter), 5, "Excellente séance de sport et d'écriture."))
            repository.insertMoodEntry(MoodEntry(today.minusDays(2).format(formatter), 4, "Bon souper préparé avec des légumes frais du potager."))
            repository.insertMoodEntry(MoodEntry(today.minusDays(3).format(formatter), 3, "Journée de travail très intense mais productive."))
            repository.insertMoodEntry(MoodEntry(today.minusDays(4).format(formatter), 4, "Agréable balade en forêt sous les premiers arbres fleuris."))
            repository.insertMoodEntry(MoodEntry(today.minusDays(5).format(formatter), 2, "Un peu fatigué et stressé par les échéances."))
            repository.insertMoodEntry(MoodEntry(today.minusDays(6).format(formatter), 5, "Reçu une très belle nouvelle d'un ami d'enfance !"))
            repository.insertMoodEntry(MoodEntry(today.minusDays(7).format(formatter), 4, "Soirée tranquille sans écrans, lecture palpitante."))
            repository.insertMoodEntry(MoodEntry(today.minusDays(8).format(formatter), 3, "Rien de spécial, repos réparateur."))
        }
    }

    // Task Actions
    fun addTask(name: String, frequencyDays: Int) {
        viewModelScope.launch {
            repository.insertTask(FrequencyTask(name = name, frequencyDays = frequencyDays))
        }
    }

    fun completeTask(task: FrequencyTask) {
        viewModelScope.launch {
            repository.markTaskCompleted(task.id, System.currentTimeMillis())
        }
    }

    fun deleteTask(task: FrequencyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Mood Actions
    fun saveMood(date: LocalDate, rating: Int, note: String) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.insertMoodEntry(MoodEntry(dateString = dateStr, moodRating = rating, gratitudeNote = note))
        }
    }

    fun deleteMoodEntry(entry: MoodEntry) {
        viewModelScope.launch {
            repository.deleteMoodEntry(entry)
        }
    }

    // Subscription Actions
    fun addSubscription(name: String, price: Double, cycle: String, day: Int, category: String) {
        viewModelScope.launch {
            repository.insertSubscription(
                Subscription(
                    name = name,
                    price = price,
                    billingCycle = cycle,
                    paymentDay = day,
                    category = category
                )
            )
        }
    }

    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(sub)
        }
    }

    // Calendar month control
    fun nextMonth() {
        if (_currentMonth.value == 12) {
            _currentMonth.value = 1
            _currentYear.value += 1
        } else {
            _currentMonth.value += 1
        }
    }

    fun prevMonth() {
        if (_currentMonth.value == 1) {
            _currentMonth.value = 12
            _currentYear.value -= 1
        } else {
            _currentMonth.value -= 1
        }
    }
}
