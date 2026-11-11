package com.example.myapplication.ui.settings

import android.app.LocaleManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.ui.common.SectionTitle
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.updater.ReleaseInfo
import com.example.myapplication.updater.UpdateCheckResult
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            container.transactionRepository,
            container.subscriptionRepository,
            container.settingsRepository,
            container.backupManager,
            container.aiAdvisor,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showRestoreConfirm by remember { mutableStateOf<Uri?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var updateProgress by remember { mutableStateOf<Float?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }

    // System file pickers
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> uri?.let { viewModel.exportBackup(it) } },
    )
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { showRestoreConfirm = it } },
    )
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri -> uri?.let { viewModel.exportCsv(it) } },
    )

    val msgBackupSaved = stringResource(R.string.msg_backup_saved)
    val msgRestoreDone = stringResource(R.string.msg_restore_done)
    val msgCsvSaved = stringResource(R.string.msg_csv_saved)
    val msgFailed = stringResource(R.string.msg_failed)
    val msgDemoLoaded = stringResource(R.string.msg_demo_loaded)
    val msgDataCleared = stringResource(R.string.msg_data_cleared)
    val msgBudgetSaved = stringResource(R.string.msg_budget_saved)
    val msgPinSaved = stringResource(R.string.msg_pin_saved)
    val msgPinRemoved = stringResource(R.string.msg_pin_removed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SettingsEvent.BackupSaved -> String.format(msgBackupSaved, event.count)
                is SettingsEvent.Restored -> String.format(msgRestoreDone, event.count)
                is SettingsEvent.CsvSaved -> String.format(msgCsvSaved, event.count)
                SettingsEvent.OperationFailed -> msgFailed
                SettingsEvent.DemoLoaded -> msgDemoLoaded
                SettingsEvent.DataCleared -> msgDataCleared
                SettingsEvent.BudgetSaved -> msgBudgetSaved
                SettingsEvent.PinSaved -> msgPinSaved
                SettingsEvent.PinRemoved -> msgPinRemoved
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Security / PIN Code section
            item {
                SectionTitle(stringResource(R.string.security_section))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.pin_lock_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (state.pinCode.isNotEmpty()) {
                                            stringResource(R.string.pin_status_enabled)
                                        } else {
                                            stringResource(R.string.pin_status_disabled)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { showPinDialog = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    if (state.pinCode.isNotEmpty()) {
                                        stringResource(R.string.pin_change_action)
                                    } else {
                                        stringResource(R.string.pin_set_action)
                                    },
                                )
                            }
                            if (state.pinCode.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.setPinCode("") },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.pin_disable_action))
                                }
                            }
                        }

                        if (state.pinCode.isNotEmpty()) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.biometric_unlock_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(R.string.biometric_unlock_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = state.biometricEnabled,
                                    onCheckedChange = viewModel::setBiometricEnabled,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Appearance (Theme & Language)
            item {
                SectionTitle(stringResource(R.string.appearance_section))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.theme_title),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = state.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                label = {
                                    Text(
                                        stringResource(R.string.theme_system),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = state.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                label = {
                                    Text(
                                        stringResource(R.string.theme_light),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = state.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                label = {
                                    Text(
                                        stringResource(R.string.theme_dark),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        context.getSystemService(LocaleManager::class.java)
                                            ?.applicationLocales = LocaleList.forLanguageTags("uz")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.language_uzbek))
                            }
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        context.getSystemService(LocaleManager::class.java)
                                            ?.applicationLocales = LocaleList.forLanguageTags("en")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.language_english))
                            }
                        }
                    }
                }
            }

            // 3. Budget limit
            item {
                SectionTitle(stringResource(R.string.budget_title))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.monthlyBudgetText,
                            onValueChange = viewModel::setBudgetText,
                            label = { Text(stringResource(R.string.budget_title)) },
                            placeholder = { Text(stringResource(R.string.budget_hint)) },
                            suffix = { Text(stringResource(R.string.currency_suffix)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { viewModel.saveBudget() }),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Button(
                            onClick = viewModel::saveBudget,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }

            // 4. Data & backup
            item {
                SectionTitle(stringResource(R.string.data_backup))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.backup_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                val filename = "moliya-backup-${LocalDate.now()}.json"
                                backupExportLauncher.launch(filename)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.backup_export))
                        }
                        OutlinedButton(
                            onClick = { backupImportLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.backup_restore))
                        }
                        OutlinedButton(
                            onClick = {
                                val filename = "moliya-${LocalDate.now()}.csv"
                                csvExportLauncher.launch(filename)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.export_csv))
                        }
                    }
                }
            }

            // 5. AI status card
            item {
                SectionTitle(stringResource(R.string.ai_status))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.modelName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.ai_status_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            // 6. Developer / Demo tools & App Version
            item {
                SectionTitle(stringResource(R.string.developer_section))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = viewModel::loadDemoData,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.demo_data))
                        }
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.clear_data), color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                isCheckingUpdate = true
                                scope.launch {
                                    when (val result = container.updateManager.checkForUpdate()) {
                                        is UpdateCheckResult.UpdateAvailable -> {
                                            updateRelease = result.release
                                        }
                                        is UpdateCheckResult.UpToDate -> {
                                            val currentVer = try {
                                                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                                            } catch (_: Exception) { "v1.4" }
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.up_to_date, currentVer),
                                            )
                                        }
                                        is UpdateCheckResult.Error -> {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.update_check_failed, result.message),
                                            )
                                        }
                                    }
                                    isCheckingUpdate = false
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isCheckingUpdate) stringResource(R.string.checking_updates)
                                else stringResource(R.string.check_for_updates),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val currentVer = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        } catch (_: Exception) { "1.4" }
                        Text(
                            text = "Moliya AI v$currentVer · GitHub OTA Auto-Update Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }

    updateRelease?.let { release ->
        com.example.myapplication.ui.common.UpdateAvailableDialog(
            release = release,
            downloadProgress = updateProgress,
            isDownloading = isDownloadingUpdate,
            onDismiss = { updateRelease = null },
            onConfirmUpdate = {
                isDownloadingUpdate = true
                scope.launch {
                    val file = container.updateManager.downloadApk(release.downloadUrl) { progress ->
                        updateProgress = progress
                    }
                    isDownloadingUpdate = false
                    if (file != null) {
                        updateRelease = null
                        container.updateManager.installApk(file)
                    }
                }
            },
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(stringResource(R.string.pin_set_action), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.pin_dialog_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it.filter { ch -> ch.isDigit() }.take(4)
                            pinError = false
                        },
                        label = { Text("PIN (4 raqam)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setPinCode(pinInput)
                            showPinDialog = false
                        } else {
                            pinError = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = showRestoreConfirm
                        showRestoreConfirm = null
                        target?.let { viewModel.importBackup(it) }
                    },
                ) {
                    Text(stringResource(R.string.restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.clear_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAllData()
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
