package com.kole.logstel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kole.logstel.domain.model.Attachment
import com.kole.logstel.domain.model.AttachmentType
import com.kole.logstel.domain.model.WorkEntry
import com.kole.logstel.ui.model.EntryFormState
import com.kole.logstel.ui.theme.WorklogTheme
import com.kole.logstel.ui.util.formatHours
import com.kole.logstel.ui.util.MonthLabelFormatter
import com.kole.logstel.ui.util.toDisplayDate
import com.kole.logstel.ui.viewmodel.DetailsViewModel
import com.kole.logstel.ui.viewmodel.EntryViewModel
import com.kole.logstel.ui.viewmodel.HomeViewModel
import com.kole.logstel.ui.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorklogTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(700)
                    showSplash = false
                }
                if (showSplash) {
                    WorklogSplash()
                } else {
                    LogStelApp()
                }
            }
        }
    }
}

@Composable
private fun WorklogSplash() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                WorklogLogo(
                    modifier = Modifier.size(62.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Worklog",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private object Routes {
    const val Home = "home"
    const val Records = "records"
    const val Search = "search"
    const val Entry = "entry/{date}"
    const val Details = "details/{date}"
    fun entry(date: String) = "entry/$date"
    fun details(date: String) = "details/$date"
}

private const val PreferencesName = "worklog_preferences"
private const val OnboardingShownKey = "onboarding_shown"
private const val GitHubUrl = "https://github.com/kole284"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogStelApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<List<WorkEntry>?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showOnboarding by rememberSaveable { mutableStateOf(false) }
    val preferences = remember(context) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }

    val backupCreateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(homeViewModel::exportBackup)
    }
    val pdfCreateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let(homeViewModel::exportPdf)
    }
    val backupOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            homeViewModel.readBackup(uri)
                .onSuccess { pendingImport = it }
                .onFailure { snackbarHostState.showSnackbar(it.message ?: "Backup import failed.") }
        }
    }

    LaunchedEffect(homeState.message) {
        homeState.message?.let {
            snackbarHostState.showSnackbar(it)
            homeViewModel.clearMessage()
        }
    }

    LaunchedEffect(preferences) {
        showOnboarding = runCatching {
            !preferences.getBoolean(OnboardingShownKey, false)
        }.getOrDefault(false)
    }

    if (showOnboarding) {
        OnboardingDialog(
            onDismiss = {
                runCatching {
                    preferences.edit().putBoolean(OnboardingShownKey, true).apply()
                }
                showOnboarding = false
            }
        )
    }

    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false },
            onOpenGitHubFailed = {
                scope.launch { snackbarHostState.showSnackbar("Unable to open GitHub link.") }
            }
        )
    }

    pendingImport?.let { entries ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Replace current data?") },
            text = { Text("The backup contains ${entries.size} work entries. Importing it will replace all current local records.") },
            confirmButton = {
                Button(onClick = {
                    homeViewModel.importBackup(entries)
                    pendingImport = null
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        WorklogLogo(
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Worklog")
                    }
                },
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = "About Worklog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNavigation(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    state = homeState,
                    onPrevious = homeViewModel::previousMonth,
                    onNext = homeViewModel::nextMonth,
                    onOpenEntry = { navController.navigate(Routes.entry(it)) },
                    onExportBackup = { backupCreateLauncher.launch("bausite-work-log-backup.json") },
                    onImportBackup = { backupOpenLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    onExportPdf = { pdfCreateLauncher.launch(homeViewModel.pdfFileName()) }
                )
            }
            composable(Routes.Records) {
                RecordsScreen(homeState.entries) { navController.navigate(Routes.details(it)) }
            }
            composable(Routes.Search) {
                SearchScreen { navController.navigate(Routes.details(it)) }
            }
            composable(Routes.Entry) {
                EntryScreen(
                    onCancel = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } }
                )
            }
            composable(Routes.Details) {
                DetailsScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.entry(it)) },
                    onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } }
                )
            }
        }
    }
}

@Composable
private fun BottomNavigation(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        listOf(Routes.Home to "Calendar", Routes.Records to "Records", Routes.Search to "Search").forEach { (route, label) ->
            NavigationBarItem(
                selected = current == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(Routes.Home) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                icon = { Text(label.first().toString(), fontWeight = FontWeight.Bold) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: com.kole.logstel.ui.viewmodel.HomeUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportPdf: () -> Unit
) {
    var pendingDate by remember { mutableStateOf<String?>(null) }
    val entryDates = state.entries.map { it.date }.toSet()
    val pending = pendingDate
    if (pending != null) {
        val exists = pending in entryDates
        AlertDialog(
            onDismissRequest = { pendingDate = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(if (exists) "Update existing entry?" else "Create work entry?") },
            confirmButton = {
                Button(onClick = {
                    pendingDate = null
                    onOpenEntry(pending)
                }) { Text(if (exists) "Edit" else "Create") }
            },
            dismissButton = { TextButton(onClick = { pendingDate = null }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalendarCard(
                month = state.month,
                entryDates = entryDates,
                onPrevious = onPrevious,
                onNext = onNext,
                onDayClick = { pendingDate = it }
            )
        }
        item {
            BackupExportHeader(
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onExportPdf = onExportPdf
            )
        }
        item {
            MonthlyTotalCard(
                totalHours = state.totalHours
            )
        }
    }
}

@Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Welcome to Worklog") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The calendar shows the current month.")
                Text("Days with saved work entries are highlighted.")
                Text("Tap an empty day to create a work entry.")
                Text("Tap a highlighted day to edit an existing entry.")
                Text("Use Records to review the month.")
                Text("Use Search to find entries by worker, construction site, or date.")
                Text("Use PDF export for monthly reports.")
                Text("Use backup, export, and import when changing phones.")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onOpenGitHubFailed: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("About Worklog") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Designed and developed by Nikola Kostić")
                TextButton(
                    onClick = {
                        runCatching { uriHandler.openUri(GitHubUrl) }
                            .onFailure { onOpenGitHubFailed() }
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(GitHubUrl)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun BackupExportHeader(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportPdf: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WorklogLogo(Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Backup and Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Keep all records local and share monthly reports.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("Export backup") }
                OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("Import") }
            }
            Button(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) { Text("Share monthly PDF") }
        }
    }
}

@Composable
private fun MonthlyTotalCard(
    totalHours: java.math.BigDecimal
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Total hours this month so far",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                formatHours(totalHours),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    entryDates: Set<String>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (String) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onPrevious) { Text("Previous") }
                Text(
                    month.format(MonthLabelFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onNext) { Text("Next") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val firstOffset = month.atDay(1).dayOfWeek.value - 1
            val days = month.lengthOfMonth()
            val cells = firstOffset + days
            val rows = (cells + 6) / 7
            val previousMonth = month.minusMonths(1)
            val previousMonthDays = previousMonth.lengthOfMonth()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(rows) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) { column ->
                            val day = row * 7 + column - firstOffset + 1
                            if (day in 1..days) {
                                CalendarDay(month.atDay(day), month.atDay(day).toString() in entryDates, onDayClick, Modifier.weight(1f))
                            } else {
                                val disabledDay = if (day < 1) previousMonthDays + day else day - days
                                DisabledCalendarDay(disabledDay, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(date: LocalDate, hasEntry: Boolean, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val today = date == LocalDate.now()
    val isFutureDate = date.isAfter(LocalDate.now())
    val targetColor by animateColorAsState(
        when {
            isFutureDate -> MaterialTheme.colorScheme.primary.copy(alpha = 0f)
            hasEntry -> MaterialTheme.colorScheme.primary
            today -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0f)
        },
        label = "dayColor"
    )
    val scale by animateFloatAsState(if (hasEntry && !isFutureDate) 1f else 0.92f, label = "dayScale")
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isFutureDate) { onClick(date.toString()) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(targetColor)
                .then(if (today && !isFutureDate) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    isFutureDate -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    hasEntry -> MaterialTheme.colorScheme.onPrimary
                    today -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (!isFutureDate && (today || hasEntry)) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DisabledCalendarDay(day: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryScreen(
    viewModel: EntryViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val attachments = uris.map { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val type = if ((context.contentResolver.getType(uri) ?: "").startsWith("video")) AttachmentType.Video else AttachmentType.Image
            uri.toString() to type
        }
        viewModel.addAttachments(attachments)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            onMessage(it)
            viewModel.consumeMessage()
        }
    }

    EntryForm(
        state = state,
        onMainWorker = viewModel::updateMainWorker,
        onSite = viewModel::updateConstructionSite,
        onHours = viewModel::updateHours,
        onNotes = viewModel::updateNotes,
        onNewWorker = viewModel::updateNewWorker,
        onAddWorker = viewModel::addWorker,
        onRemoveWorker = viewModel::removeWorker,
        onAddAttachments = { attachmentLauncher.launch(arrayOf("image/*", "video/*")) },
        onRemoveAttachment = viewModel::removeAttachment,
        onSave = viewModel::save,
        onCancel = onCancel
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryForm(
    state: EntryFormState,
    onMainWorker: (String) -> Unit,
    onSite: (String) -> Unit,
    onHours: (String) -> Unit,
    onNotes: (String) -> Unit,
    onNewWorker: (String) -> Unit,
    onAddWorker: () -> Unit,
    onRemoveWorker: (String) -> Unit,
    onAddAttachments: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Work Entry", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            OutlinedTextField(
                value = state.mainWorker,
                onValueChange = onMainWorker,
                label = { Text("Main Worker") },
                singleLine = true,
                isError = state.mainWorkerError != null,
                supportingText = { state.mainWorkerError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = state.date.toDisplayDate(),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = state.constructionSite,
                onValueChange = onSite,
                label = { Text("Construction Site") },
                singleLine = true,
                isError = state.constructionSiteError != null,
                supportingText = { state.constructionSiteError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.newWorker,
                    onValueChange = onNewWorker,
                    label = { Text("Other Worker") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onAddWorker) { Text("Add") }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.otherWorkers.forEach { worker ->
                    AssistChip(onClick = { onRemoveWorker(worker) }, label = { Text("$worker  Remove") })
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.hoursWorked,
                onValueChange = onHours,
                label = { Text("Hours Worked") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.hoursError != null,
                supportingText = { state.hoursError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotes,
                label = { Text("Notes") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddAttachments) { Text("Add images or videos") }
                state.attachments.forEach { attachment ->
                    AttachmentRow(attachment = attachment, onClick = {}, onRemove = { onRemoveAttachment(attachment) })
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}

@Composable
private fun RecordsScreen(entries: List<WorkEntry>, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Records", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (entries.isEmpty()) {
            item { EmptyCard("No work entries for the selected month.") }
        } else {
            items(entries, key = { it.date }) { entry ->
                RecordRow(entry, onOpen)
            }
        }
    }
}

@Composable
private fun SearchScreen(viewModel: SearchViewModel = hiltViewModel(), onOpen: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                label = { Text("Search site, date, or worker") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.results.isEmpty()) {
            item { EmptyCard("No matching records.") }
        } else {
            items(state.results, key = { it.date }) { entry -> RecordRow(entry, onOpen) }
        }
    }
}

@Composable
private fun RecordRow(entry: WorkEntry, onOpen: (String) -> Unit) {
    Card(
        onClick = { onOpen(entry.date) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.date.toDisplayDate(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(entry.constructionSite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.hoursWorked} h", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsScreen(
    viewModel: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    val entry by viewModel.entry.collectAsState()
    val context = LocalContext.current
    val current = entry
    if (current == null) {
        EmptyCard("Record not found.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Record Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("Back") }
                Button(onClick = { onEdit(current.date) }) { Text("Edit") }
            }
        }
        item { DetailCard("Main Worker", current.mainWorker) }
        item { DetailCard("Date", current.date.toDisplayDate()) }
        item { DetailCard("Construction Site", current.constructionSite) }
        item { DetailCard("Other Workers", current.otherWorkers.joinToString(", ").ifBlank { "None" }) }
        item { DetailCard("Hours", "${current.hoursWorked} h") }
        item { DetailCard("Notes", current.notes.ifBlank { "No notes" }) }
        item {
            Text("Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (current.attachments.isEmpty()) {
                Text("No images or videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    current.attachments.forEach { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            onClick = {
                                runCatching { openAttachment(context, attachment) }
                                    .onFailure { onMessage("Unable to open attachment. It may have been moved or deleted.") }
                            },
                            onRemove = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AttachmentRow(attachment: Attachment, onClick: () -> Unit, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = onRemove == null) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text(if (attachment.type == AttachmentType.Image) "Image" else "Video") }
        )
        Spacer(Modifier.width(10.dp))
        Text(Uri.parse(attachment.uri).lastPathSegment ?: attachment.uri, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onRemove != null) TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorklogLogo(Modifier.size(40.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WorklogLogo(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "Worklog logo",
        tint = tint,
        modifier = modifier
    )
}

private fun shareUri(context: android.content.Context, uri: Uri, title: String, type: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = type
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun openAttachment(context: android.content.Context, attachment: Attachment) {
    val type = if (attachment.type == AttachmentType.Image) "image/*" else "video/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(attachment.uri), type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}
