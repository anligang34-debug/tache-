package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FrequencyTask
import com.example.data.MoodEntry
import com.example.data.Subscription
import com.example.ui.RituelViewModel
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: RituelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RituelApp(viewModel = viewModel)
            }
        }
    }
}

// Navigation Tabs enum
enum class RituelTab(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    TASKS("Routines", Icons.Filled.Autorenew, Icons.Outlined.Autorenew),
    JOURNAL("Journal", Icons.Filled.Mood, Icons.Outlined.Mood),
    SUBSCRIPTIONS("Abonnements", Icons.Filled.CardMembership, Icons.Outlined.CardMembership)
}

@Composable
fun BentoHeader() {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    val formattedDate = LocalDate.now().format(formatter)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Command Center",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Circular dynamic initials badge from design template
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { /* action if wanted */ },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AL", // anligang34 initials
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RituelApp(viewModel: RituelViewModel) {
    var selectedTab by remember { mutableStateOf(RituelTab.TASKS) }

    Scaffold(
        topBar = {
            BentoHeader()
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav")
            ) {
                RituelTab.values().forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = if (selected) "Aujourd'hui" else tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = spring(),
                label = "MainCrossfade"
            ) { tab ->
                when (tab) {
                    RituelTab.TASKS -> TasksScreen(viewModel)
                    RituelTab.JOURNAL -> JournalScreen(viewModel)
                    RituelTab.SUBSCRIPTIONS -> SubscriptionsScreen(viewModel)
                }
            }
        }
    }
}

// ==========================================
// 1. ROUTINES (TASKS BY FREQUENCY) SCREEN
// ==========================================
@Composable
fun TasksScreen(viewModel: RituelViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // Statistics and urgent task math
    val overdueTasksCount = tasks.count { it.isOverdue() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Dashboard Overview (Bento Primary Card)
            item {
                BentoTasksHeaderCard(
                    overdueCount = overdueTasksCount,
                    totalCount = tasks.size
                )
            }

            // Section headings & Lists
            if (tasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Aucune routine configurée",
                        description = "Cliquez sur le bouton '+' en bas à droite pour ajouter votre première tâche récurrente (ex: arroser les plantes, changer les draps)."
                    )
                }
            } else {
                val urgentTasks = tasks.filter { it.isOverdue() }.sortedByDescending { it.daysElapsed() }
                val upToDateTasks = tasks.filter { !it.isOverdue() }.sortedBy { it.getNextDueDateMillis() }

                // 1. Overdue/Urgent tasks: display full-width high-priority Bento cards
                if (urgentTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Routines en retard (${urgentTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorVerySad,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                    items(urgentTasks, key = { "urgent_${it.id}" }) { task ->
                        BentoTaskWide(task = task, onComplete = { viewModel.completeTask(task) }, onDelete = { viewModel.deleteTask(task) })
                    }
                }

                // 2. Up to date tasks: display in beautiful side-by-side Bento squares
                if (upToDateTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "À ne pas oublier (${upToDateTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }

                    // Loop through up to date tasks in pairs of 2 to build our adaptive Bento grid rows
                    var i = 0
                    while (i < upToDateTasks.size) {
                        if (i + 1 < upToDateTasks.size) {
                            val left = upToDateTasks[i]
                            val right = upToDateTasks[i + 1]
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        BentoTaskSquare(task = left, onComplete = { viewModel.completeTask(left) }, onDelete = { viewModel.deleteTask(left) })
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        BentoTaskSquare(task = right, onComplete = { viewModel.completeTask(right) }, onDelete = { viewModel.deleteTask(right) })
                                    }
                                }
                            }
                            i += 2
                        } else {
                            val single = upToDateTasks[i]
                            item {
                                BentoTaskSquare(task = single, onComplete = { viewModel.completeTask(single) }, onDelete = { viewModel.deleteTask(single) })
                            }
                            i += 1
                        }
                    }
                }
            }

            // Margin bottom for the floating button spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating action button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 8.dp)
                .testTag("add_routine_fab")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter routine")
        }

        if (showAddDialog) {
            AddRoutineDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, freq ->
                    viewModel.addTask(name, freq)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BentoTasksHeaderCard(overdueCount: Int, totalCount: Int) {
    val compliance = if (totalCount > 0) {
        ((totalCount - overdueCount).toFloat() / totalCount)
    } else {
        1.0f
    }
    val compliancePercentage = (compliance * 100).toInt()

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left dynamic content
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Tableau de Bord",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Routines & Cycles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Bento dynamic badge
                if (overdueCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BentoWarningBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "En retard (+$overdueCount)",
                            color = BentoWarningText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "À jour ! 🎉",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Text(
                    text = if (overdueCount > 0) "Prenez soin de vos habitudes en retard." else "Exceptionnel ! Tout est en ordre de marche.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score d'assiduité",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$compliancePercentage%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customized HTML bento-styled compliance progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(compliance)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoTaskWide(
    task: FrequencyTask,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val daysElapsed = task.daysElapsed()
    val nextDueDate = InstantToLocalDate(task.getNextDueDateMillis())
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon layout matching Netflix 'N' container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information fields
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BentoWarningBg.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Retard",
                            color = BentoWarningText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Fréquence : tous les ${task.frequencyDays} j • Échéance passée",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            // Action completes with checkmark ripple feedback
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSecondaryContainer)
                        .testTag("complete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Fait",
                        tint = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoTaskSquare(
    task: FrequencyTask,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val daysElapsed = task.daysElapsed()
    val nextDueDate = InstantToLocalDate(task.getNextDueDateMillis())
    val daysLeft = task.frequencyDays - daysElapsed
    val formattedStatus = if (daysLeft > 0) "Dans $daysLeft j" else "Aujourd'hui"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category mini badge or indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clean checkbox replacement from mockup
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f))
                        .testTag("complete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Marquer fait",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$formattedStatus • Tous les ${task.frequencyDays} j",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var frequencyText by remember { mutableStateOf("7") }
    var errors by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Créer une Routine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la tâche") },
                    placeholder = { Text("ex: Arroser le ficus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_routine_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = frequencyText,
                    onValueChange = { frequencyText = it },
                    label = { Text("Fréquence (en jours)") },
                    placeholder = { Text("ex: 7") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_routine_freq"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errors != null) {
                    Text(
                        text = errors!!,
                        color = ColorVerySad,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_routine_button"),
                        onClick = {
                            val freq = frequencyText.toIntOrNull()
                            when {
                                name.isBlank() -> {
                                    errors = "Veuillez entrer un nom à votre tâche."
                                }
                                freq == null || freq <= 0 -> {
                                    errors = "Veuillez entrer une fréquence valide supérieure à 0."
                                }
                                else -> {
                                    onAdd(name, freq)
                                }
                            }
                        }
                    ) {
                        Text("Créer")
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MOOD & GRATITUDE JOURNAL SCREEN
// ==========================================
@Composable
fun JournalScreen(viewModel: RituelViewModel) {
    val moodEntries by viewModel.moodEntries.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    // Screen local calendars (month state triggers redraw flow)
    val currentYear by viewModel.currentYear.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()

    var showLogDialogByDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedViewEntry by remember { mutableStateOf<MoodEntry?>(null) }

    // Aggregate monthly statistics
    val yearMonth = YearMonth.of(currentYear, currentMonth)
    val entriesThisMonth = moodEntries.filter {
        val entryDate = LocalDate.parse(it.dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        entryDate.year == currentYear && entryDate.monthValue == currentMonth
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Heading Card Tracker - styled as an elegant compact bento header block
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Journal & Gratitude",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enregistrez votre humeur et notez un moment positif en terrasse ou au soleil.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Calendar Grid Display - styled as a beautiful solid Bento Box
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with Prev/Next Month selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.prevMonth() }) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft, 
                                contentDescription = "Mois précédent",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            text = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() } + " " + currentYear,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight, 
                                contentDescription = "Mois suivant",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week Headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val weekdays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                        weekdays.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month Calendar Grid Logic
                    val daysInMonth = yearMonth.lengthOfMonth()
                    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value // 1 = Monday, 7 = Sunday
                    val totalSlots = daysInMonth + (firstDayOfWeek - 1)
                    val rowsCount = (totalSlots + 6) / 7

                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val slotIndex = row * 7 + col
                                val dayNumber = slotIndex - (firstDayOfWeek - 2)

                                if (dayNumber in 1..daysInMonth) {
                                    val currentSlotDate = LocalDate.of(currentYear, currentMonth, dayNumber)
                                    val currentSlotDateStr = currentSlotDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    val matchingEntry = moodEntries.find { it.dateString == currentSlotDateStr }

                                    CalendarDayCell(
                                        dayNumber = dayNumber,
                                        isToday = currentSlotDate == LocalDate.now(),
                                        moodEntry = matchingEntry,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.5f) // slightly wider box for a cleaner grid
                                            .padding(3.dp),
                                        onClick = {
                                            if (matchingEntry != null) {
                                                selectedViewEntry = matchingEntry
                                            } else {
                                                showLogDialogByDate = currentSlotDate
                                            }
                                        }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1.5f).padding(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Histograms and entries in current month
        item {
            Text(
                text = "Notes de gratitude récentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }

        if (entriesThisMonth.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Aucune entrée enregistrée pour ce mois-ci. Tapez sur une case du calendrier pour ajouter votre humeur.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(entriesThisMonth.take(10), key = { it.dateString }) { entry ->
                RecentGratitudeRow(entry = entry, onClick = { selectedViewEntry = entry })
            }
        }
    }

    // Modal forms and details dialogs
    if (showLogDialogByDate != null) {
        AddMoodDialog(
            date = showLogDialogByDate!!,
            onDismiss = { showLogDialogByDate = null },
            onSave = { rating, note ->
                viewModel.saveMood(showLogDialogByDate!!, rating, note)
                showLogDialogByDate = null
            }
        )
    }

    if (selectedViewEntry != null) {
        ViewMoodDetailsDialog(
            entry = selectedViewEntry!!,
            onDelete = {
                viewModel.deleteMoodEntry(selectedViewEntry!!)
                selectedViewEntry = null
            },
            onDismiss = { selectedViewEntry = null }
        )
    }
}

// Color and Icon selector helpers for Mood Level
fun getMoodVisuals(rating: Int): Pair<Color, String> {
    return when (rating) {
        5 -> Pair(ColorRadiant, "🌟 Radieux")
        4 -> Pair(ColorHappy, "😊 Joyeux")
        3 -> Pair(ColorNeutral, "😐 Neutre")
        2 -> Pair(ColorSad, "😢 Triste")
        else -> Pair(ColorVerySad, "😭 Très triste")
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    moodEntry: MoodEntry?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val hasEntry = moodEntry != null
    val targetBgColor = if (hasEntry) {
        getMoodVisuals(moodEntry!!.moodRating).first.copy(alpha = 0.15f)
    } else if (isToday) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(targetBgColor)
            .border(
                width = 1.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("calendar_day_$dayNumber")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = "$dayNumber",
                fontWeight = if (isToday || hasEntry) FontWeight.ExtraBold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasEntry) {
                    getMoodVisuals(moodEntry!!.moodRating).first
                } else if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                }
            )
            if (hasEntry) {
                Spacer(modifier = Modifier.height(2.dp))
                // Miniature elegant dot indicator
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(getMoodVisuals(moodEntry!!.moodRating).first)
                )
            }
        }
    }
}

@Composable
fun RecentGratitudeRow(entry: MoodEntry, onClick: () -> Unit) {
    val date = LocalDate.parse(entry.dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val (moodColor, moodText) = getMoodVisuals(entry.moodRating)
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = date.format(formatter)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                )
                Text(
                    text = moodText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = moodColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Italic quote block with styling directly from Bento HTML:
            // "Un café chaud en terrasse au soleil."
            val noteText = if (entry.gratitudeNote.isBlank()) "Aucun moment noté." else entry.gratitudeNote
            Text(
                text = "“ $noteText ”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5 dot rating tracker matching HTML layout precisely:
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (score in 1..5) {
                    val active = score <= entry.moodRating
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) moodColor
                                else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun AddMoodDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(3) } // Neutral is default (3)
    var note by remember { mutableStateOf("") }
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enregistrer votre Émotion",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = date.format(formatter).capitalize(Locale.FRENCH),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Five Buttons Selector for Mood
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (score in 1..5) {
                        val isSelected = rating == score
                        val (moodColor, description) = getMoodVisuals(score)
                        val emoji = when (score) {
                            5 -> "🌟"
                            4 -> "😊"
                            3 -> "😐"
                            2 -> "😢"
                            else -> "😭"
                        }
                        IconButton(
                            onClick = { rating = score },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) moodColor.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .testTag("select_mood_$score")
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }

                // Subtitle helper
                val (_, activeText) = getMoodVisuals(rating)
                Text(
                    text = "Humeur sélectionnée : $activeText",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = getMoodVisuals(rating).first
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Gratitude du jour (un truc positif)") },
                    placeholder = { Text("Quelque chose qui vous a rendu heureux...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("mood_note_field"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_mood_button"),
                        onClick = { onSave(rating, note) }
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun ViewMoodDetailsDialog(
    entry: MoodEntry,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val date = LocalDate.parse(entry.dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val (moodColor, moodLabel) = getMoodVisuals(entry.moodRating)
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = date.format(formatter).capitalize(Locale.FRENCH),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = moodLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = moodColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Supprimer", tint = ColorVerySad)
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Moment Positif :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (entry.gratitudeNote.isBlank()) "Aucun mot de gratitude renseigné." else entry.gratitudeNote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = onDismiss
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. SUBSCRIPTIONS (SUIVI D'ABONNEMENTS) SCREEN
// ==========================================
@Composable
fun SubscriptionsScreen(viewModel: RituelViewModel) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // Statistical counts
    val totalExpense = subscriptions.sumOf { it.price }
    val today = LocalDate.now()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Dashboard header block for finances - solid Bento Box style
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Budget & Forfaits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Vos prélèvements récurrents en un clin d'œil.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = String.format("%.2f €", totalExpense),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Mensuel cumulé",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${subscriptions.size}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Forfaits actifs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Notification / Warning Card for upcoming dues this week!
            val upcomingSchedules = subscriptions.filter { sub ->
                val dueDiff = sub.paymentDay - today.dayOfMonth
                dueDiff in 0..7
            }
            if (upcomingSchedules.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoWarningBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = BentoWarningText.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Échéances proches",
                                tint = BentoWarningText,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Règlements à surveiller",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoWarningText
                                )
                                Text(
                                    text = "Prochainement : " + upcomingSchedules.joinToString { "${it.name} (${it.paymentDay})" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BentoWarningText.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Subscriptions list styled in side-by-side Bento pairs
            if (subscriptions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Aucun abonnement enregistré",
                        description = "Enregistrez vos forfaits de streaming, de sport ou de logiciels pour suivre vos dépenses globales."
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vos cartes d'abonnements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                // Grid layout grouping subscriptions side-by-side in couples of 2
                var i = 0
                while (i < subscriptions.size) {
                    if (i + 1 < subscriptions.size) {
                        val first = subscriptions[i]
                        val second = subscriptions[i + 1]
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    BentoSubscriptionSquare(sub = first, onDelete = { viewModel.deleteSubscription(first) })
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    BentoSubscriptionSquare(sub = second, onDelete = { viewModel.deleteSubscription(second) })
                                }
                            }
                        }
                        i += 2
                    } else {
                        val single = subscriptions[i]
                        item {
                            BentoSubscriptionSquare(sub = single, onDelete = { viewModel.deleteSubscription(single) })
                        }
                        i += 1
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Add Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 8.dp)
                .testTag("add_subscription_fab")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter abonnement")
        }

        if (showAddDialog) {
            AddSubscriptionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, price, cycle, day, cat ->
                    viewModel.addSubscription(name, price, cycle, day, cat)
                    showAddDialog = false
                }
            )
        }
    }
}

// Sub category icons resolver
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Loisirs" -> Icons.Filled.PlayCircle
        "Santé" -> Icons.Filled.Favorite
        "Travail" -> Icons.Filled.Computer
        else -> Icons.Filled.Category
    }
}

@Composable
fun BentoSubscriptionSquare(sub: Subscription, onDelete: () -> Unit) {
    val today = LocalDate.now()
    val dueDiff = sub.paymentDay - today.dayOfMonth
    // Create first-letter abbreviation for the logo badge
    val stampLetter = if (sub.name.isNotBlank()) sub.name[0].uppercaseChar().toString() else "?"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("subscription_card_${sub.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Retro solid block logo matching Netflix 'N' in provided bento HTML
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stampLetter,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                // Bold price tags
                Text(
                    text = String.format("%.2f €", sub.price),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Text(
                    text = sub.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Le ${sub.paymentDay} du mois",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small dynamic alert dot
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (dueDiff in 0..7) BentoWarningBg 
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (dueDiff == 0) "Payé" else if (dueDiff in 1..7) "$dueDiff j" else "Ok",
                        color = if (dueDiff in 0..7) BentoWarningText else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_subscription_${sub.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var paymentDayText by remember { mutableStateOf("1") }
    var cycle by remember { mutableStateOf("Mensuel") }
    var category by remember { mutableStateOf("Loisirs") }

    val categories = listOf("Loisirs", "Santé", "Travail", "Autre")
    var errors by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Ajouter un Abonnement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom (ex: Netflix, Spotify)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_sub_name"),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix (€, ex: 10.99)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_sub_price")
                            .padding(end = 4.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = paymentDayText,
                        onValueChange = { paymentDayText = it },
                        label = { Text("Jour de paye (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_sub_day")
                            .padding(start = 4.dp),
                        singleLine = true
                    )
                }

                // Simple Visual Radio Row for Categories
                Text(
                    text = "Catégorie :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_$cat"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (errors != null) {
                    Text(
                        text = errors!!,
                        color = ColorVerySad,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_subscription_button"),
                        onClick = {
                            val price = priceText.toDoubleOrNull()
                            val day = paymentDayText.toIntOrNull()

                            when {
                                name.isBlank() -> {
                                    errors = "Veuillez donner un nom à votre abonnement."
                                }
                                price == null || price <= 0.0 -> {
                                    errors = "Veuillez entrer un prix exact supérieur à 0."
                                }
                                day == null || day !in 1..31 -> {
                                    errors = "Veuillez spécifier un jour du mois entre 1 et 31."
                                }
                                else -> {
                                    onAdd(name, price, cycle, day, category)
                                }
                            }
                        }
                    ) {
                        Text("Ajouter")
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. COMMON GENERAL COMPOSE WIDGETS
// ==========================================
@Composable
fun EmptyStateCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Converter Helpers for dates (milliseconds-to-localdate)
fun InstantToLocalDate(timestamp: Long): LocalDate {
    return java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
}
