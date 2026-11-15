package com.example.myapplication.ui.settings

import android.Manifest
import android.app.LocaleManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.ui.common.AddCustomCategoryDialog
import com.example.myapplication.ui.common.SectionTitle
import com.example.myapplication.ui.common.ThousandsSeparatorVisualTransformation
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.common.customCategoryIconRes
import com.example.myapplication.ui.common.formatAmount
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
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

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

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.setSmsAutoDetectEnabled(isGranted)
            if (!isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.sms_permission_dialog_desc))
                }
            }
        },
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
            // 1. Moliya va Avtomatlaştiriş (Budget, Custom Categories, SMS Auto-Detect)
            item {
                SectionTitle("Moliya va Avtomatlaştiriş")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column {
                        val budgetVal = state.monthlyBudgetText.toLongOrNull() ?: 0L
                        SettingsRowItem(
                            iconRes = R.drawable.ic_bar_chart,
                            iconTint = Color(0xFF1E88E5),
                            title = stringResource(R.string.budget_title),
                            subtitle = if (budgetVal > 0L) {
                                "${formatAmount(budgetVal)} söm / oy"
                            } else {
                                "Belgilanmagan — örnatiş uçun bosing"
                            },
                            onClick = { showBudgetDialog = true },
                            trailing = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        SettingsRowItem(
                            iconRes = R.drawable.ic_cat_shopping,
                            iconTint = Color(0xFF8E24AA),
                            title = "Maxsus toifalar (Kategoriyalar)",
                            subtitle = if (state.customCategories.isEmpty()) {
                                "Şaxsiy kirim va çiqim toifalarini qöşiş"
                            } else {
                                "${state.customCategories.size} ta şaxsiy toifa mavjud"
                            },
                            onClick = { showManageCategoriesDialog = true },
                            trailing = {
                                FilledTonalButton(
                                    onClick = { showAddCategoryDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Qöşiş", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        SettingsRowItem(
                            iconRes = R.drawable.ic_auto_awesome,
                            iconTint = Color(0xFF00897B),
                            title = stringResource(R.string.sms_auto_detect_title),
                            subtitle = "Xarid va sotuv SMS xabarlarini AI qoralamaga saqlaş",
                            onClick = null,
                            trailing = {
                                Switch(
                                    checked = state.smsAutoDetectEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECEIVE_SMS,
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPermission) {
                                                viewModel.setSmsAutoDetectEnabled(true)
                                            } else {
                                                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                                            }
                                        } else {
                                            viewModel.setSmsAutoDetectEnabled(false)
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
            }

            // 2. Koʻrinish va Til (Appearance & Language)
            item {
                SectionTitle(stringResource(R.string.appearance_section))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.theme_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
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

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            context.getSystemService(LocaleManager::class.java)
                                                ?.applicationLocales = LocaleList.forLanguageTags("uz")
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Text(stringResource(R.string.language_uzbek), style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            context.getSystemService(LocaleManager::class.java)
                                                ?.applicationLocales = LocaleList.forLanguageTags("en")
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Text(stringResource(R.string.language_english), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Xavfsizlik va Maxfiylik Qalqoni (PIN, Biometrics, FLAG_SECURE, Auto-Lock, Permissions)
            item {
                SectionTitle(stringResource(R.string.security_section))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column {
                        SettingsRowItem(
                            iconRes = R.drawable.ic_lock,
                            iconTint = Color(0xFF3949AB),
                            title = stringResource(R.string.pin_lock_title),
                            subtitle = if (state.pinCode.isNotEmpty()) {
                                "${stringResource(R.string.pin_status_enabled)} • PBKDF2-SHA256 şifrlangan"
                            } else {
                                stringResource(R.string.pin_status_disabled)
                            },
                            onClick = { showPinDialog = true },
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { showPinDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) {
                                        Text(
                                            text = if (state.pinCode.isNotEmpty()) "Özgartiriş" else "Örnatiş",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                    if (state.pinCode.isNotEmpty()) {
                                        TextButton(
                                            onClick = { viewModel.setPinCode("") },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp),
                                        ) {
                                            Text(
                                                text = "Öçiriş",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                            },
                        )

                        if (state.pinCode.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                            SettingsRowItem(
                                iconRes = R.drawable.ic_shield,
                                iconTint = Color(0xFF00897B),
                                title = stringResource(R.string.biometric_unlock_title),
                                subtitle = stringResource(R.string.biometric_unlock_desc),
                                onClick = null,
                                trailing = {
                                    Switch(
                                        checked = state.biometricEnabled,
                                        onCheckedChange = viewModel::setBiometricEnabled,
                                    )
                                },
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                            SettingsRowItem(
                                iconRes = R.drawable.ic_shield,
                                iconTint = Color(0xFF1E88E5),
                                title = "Skrinşot va yozib olişdan himoya",
                                subtitle = "Recent Apps va ekran yozuvçilarida balansni yaşiriş (FLAG_SECURE)",
                                onClick = null,
                                trailing = {
                                    Switch(
                                        checked = state.screenSecurityEnabled,
                                        onCheckedChange = viewModel::setScreenSecurityEnabled,
                                    )
                                },
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                            SettingsRowItem(
                                iconRes = R.drawable.ic_lock,
                                iconTint = Color(0xFF8E24AA),
                                title = "Fonda avtomatik qulflaş",
                                subtitle = "Ilova 60 soniya fonda qolganda PIN-kodni qayta сöraş",
                                onClick = null,
                                trailing = {
                                    Switch(
                                        checked = state.autoLockEnabled,
                                        onCheckedChange = viewModel::setAutoLockEnabled,
                                    )
                                },
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        val hasSmsPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECEIVE_SMS,
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasContactsPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CONTACTS,
                        ) == PackageManager.PERMISSION_GRANTED
                        val activePermsCount = listOf(hasSmsPerm, hasContactsPerm, true).count { it }

                        SettingsRowItem(
                            iconRes = R.drawable.ic_shield,
                            iconTint = Color(0xFF2E7D32),
                            title = "Ruxsatlar va Maxfiylik Qalqoni ($activePermsCount/3 faol)",
                            subtitle = "SMS karta/balans maskalaş yoqilgan • Tizim ruxsatlarini boşqariş",
                            onClick = {
                                runCatching {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    ).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                        )
                    }
                }
            }

            // 4. Maʼlumotlar va Zaxira (Backup, Export, Demo, Clear)
            item {
                SectionTitle(stringResource(R.string.data_backup))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column {
                        SettingsRowItem(
                            iconRes = R.drawable.ic_savings,
                            iconTint = Color(0xFF43A047),
                            title = stringResource(R.string.backup_export),
                            subtitle = "Barça amallar va obunalarni JSON faylga saqlaş",
                            onClick = {
                                val filename = "moliya-backup-${LocalDate.now()}.json"
                                backupExportLauncher.launch(filename)
                            },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        SettingsRowItem(
                            iconRes = R.drawable.ic_build,
                            iconTint = Color(0xFF1E88E5),
                            title = stringResource(R.string.backup_restore),
                            subtitle = "Oldin saqlangan JSON zaxira faylidan tiklaş",
                            onClick = { backupImportLauncher.launch(arrayOf("application/json", "*/*")) },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        SettingsRowItem(
                            iconRes = R.drawable.ic_bar_chart,
                            iconTint = Color(0xFFFB8C00),
                            title = stringResource(R.string.export_csv),
                            subtitle = "Excel va Google Sheets uçun jadval körinişida saqlaş",
                            onClick = {
                                val filename = "moliya-${LocalDate.now()}.csv"
                                csvExportLauncher.launch(filename)
                            },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        SettingsRowItem(
                            iconRes = R.drawable.ic_trophy,
                            iconTint = Color(0xFF8E24AA),
                            title = stringResource(R.string.demo_data),
                            subtitle = "Ilova imkoniyatlarini köriş uçun namuna amallar qöşiş",
                            onClick = viewModel::loadDemoData,
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        SettingsRowItem(
                            iconRes = R.drawable.ic_cat_loans,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.clear_data),
                            subtitle = "Barça kiritilgan amallar va tarixni öçiriş",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showClearConfirm = true },
                        )
                    }
                }
            }

            // 5. AI Tizimi va Ilova yangilanishi
            item {
                SectionTitle("AI Tizimi va Ilova haqida")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = state.modelName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    text = stringResource(R.string.ai_status_ready),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
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
                                            } catch (_: Exception) { "2.2" }
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

                        val currentVer = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        } catch (_: Exception) { "2.2" }
                        Text(
                            text = "Moliya AI v$currentVer · GitHub OTA Auto-Update Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }

    // Monthly Budget Dialog
    if (showBudgetDialog) {
        var budgetInput by remember(state.monthlyBudgetText) { mutableStateOf(state.monthlyBudgetText) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(stringResource(R.string.budget_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Bir oylik umumiy xarajatlar çegarasini kiriting (0 — çeklovsiz):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.budget_title)) },
                        placeholder = { Text(stringResource(R.string.budget_hint)) },
                        suffix = { Text(stringResource(R.string.currency_suffix)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.setBudgetText(budgetInput)
                                viewModel.saveBudget()
                                showBudgetDialog = false
                            },
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setBudgetText(budgetInput)
                        viewModel.saveBudget()
                        showBudgetDialog = false
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Manage Custom Categories Dialog
    if (showManageCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showManageCategoriesDialog = false },
            title = { Text("Maxsus toifalar", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.customCategories.isEmpty()) {
                        Text(
                            text = "Hozirça şaxsiy toifalar qöşilmagan. Pastdagi tugma orqali yangi toifa va ikonka tanlang.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.customCategories, key = { it.id }) { cat ->
                                val tint = Color(cat.colorHex)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(tint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(customCategoryIconRes(cat.iconKey)),
                                            contentDescription = cat.name,
                                            tint = tint,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = if (cat.type == TransactionType.EXPENSE) "Çiqim" else "Kirim",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomCategory(cat.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Öçiriş",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Yangi toifa qöşiş")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageCategoriesDialog = false }) {
                    Text("Yopiş")
                }
            },
        )
    }

    if (showAddCategoryDialog) {
        AddCustomCategoryDialog(
            initialType = TransactionType.EXPENSE,
            showTypeSelector = true,
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { newCat ->
                viewModel.addCustomCategory(newCat)
                showAddCategoryDialog = false
            },
        )
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
                TextButton(
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
                        viewModel.importBackup(uri)
                        showRestoreConfirm = null
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
                        viewModel.clearAllData()
                        showClearConfirm = false
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

@Composable
private fun SettingsRowItem(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
