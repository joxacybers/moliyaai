package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.ui.addedit.AddEditScreen
import com.example.myapplication.ui.advisor.AdvisorScreen
import com.example.myapplication.ui.auth.PinLockScreen
import com.example.myapplication.ui.auth.SplashScreen
import com.example.myapplication.ui.budget.CategoryBudgetScreen
import com.example.myapplication.ui.common.UpdateAvailableDialog
import com.example.myapplication.ui.common.appContainer
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.debts.DebtsScreen
import com.example.myapplication.ui.history.HistoryScreen
import com.example.myapplication.ui.navigation.AddEditRoute
import com.example.myapplication.ui.navigation.AdvisorTab
import com.example.myapplication.ui.navigation.CategoryBudgetsRoute
import com.example.myapplication.ui.navigation.DebtsRoute
import com.example.myapplication.ui.navigation.HistoryTab
import com.example.myapplication.ui.navigation.HomeTab
import com.example.myapplication.ui.navigation.SavingsRoute
import com.example.myapplication.ui.navigation.SettingsRoute
import com.example.myapplication.ui.navigation.StatsTab
import com.example.myapplication.ui.navigation.SubscriptionsTab
import com.example.myapplication.ui.navigation.rememberAppNavigator
import com.example.myapplication.ui.navigation.toDecoratedEntries
import com.example.myapplication.ui.savings.SavingsGoalsScreen
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.stats.StatsScreen
import com.example.myapplication.ui.subscriptions.SubscriptionsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.updater.ReleaseInfo
import com.example.myapplication.updater.UpdateCheckResult
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = appContainer()
            val themeMode by container.settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val pinCode by container.settingsRepository.pinCode.collectAsStateWithLifecycle(initialValue = null)
            val biometricEnabled by container.settingsRepository.biometricEnabled.collectAsStateWithLifecycle(initialValue = true)

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = {},
            )

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
                }
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                var pinUnlocked by rememberSaveable { mutableStateOf(false) }

                when {
                    showSplash -> {
                        SplashScreen(onFinished = { showSplash = false })
                    }
                    pinCode == null -> {
                        // Still reading DataStore
                    }
                    pinCode!!.isNotEmpty() && !pinUnlocked -> {
                        val isBiometricAvailable = remember {
                            com.example.myapplication.ui.auth.BiometricHelper.isBiometricAvailable(this@MainActivity)
                        }
                        PinLockScreen(
                            expectedPin = pinCode!!,
                            onUnlocked = { pinUnlocked = true },
                            biometricAvailable = isBiometricAvailable && biometricEnabled,
                            onBiometricClick = {
                                com.example.myapplication.ui.auth.BiometricHelper.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = { pinUnlocked = true },
                                )
                            },
                        )
                    }
                    else -> {
                        MainApp()
                    }
                }
            }
        }
    }
}

private sealed interface TabIcon {
    data class Vector(val imageVector: ImageVector) : TabIcon
    data class Resource(val resId: Int) : TabIcon
}

private data class NavItem(
    val key: NavKey,
    val icon: TabIcon,
    val labelRes: Int,
)

private val NAV_ITEMS = listOf(
    NavItem(HomeTab, TabIcon.Vector(Icons.Default.Home), R.string.nav_home),
    NavItem(HistoryTab, TabIcon.Vector(Icons.AutoMirrored.Filled.List), R.string.nav_history),
    NavItem(SubscriptionsTab, TabIcon.Resource(R.drawable.ic_subscriptions), R.string.nav_subscriptions),
    NavItem(StatsTab, TabIcon.Resource(R.drawable.ic_bar_chart), R.string.nav_stats),
    NavItem(AdvisorTab, TabIcon.Resource(R.drawable.ic_auto_awesome), R.string.nav_advisor),
)

@Composable
fun MainApp() {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val topLevelRoutes = remember { NAV_ITEMS.map { it.key }.toSet() }
    val navigator = rememberAppNavigator(startRoute = HomeTab, topLevelRoutes = topLevelRoutes)

    var availableRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    // Check for updates automatically in background on launch
    LaunchedEffect(Unit) {
        val result = container.updateManager.checkForUpdate()
        if (result is UpdateCheckResult.UpdateAvailable) {
            availableRelease = result.release
        }
    }

    BackHandler(enabled = !navigator.isAtRoot()) {
        navigator.pop()
    }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<HomeTab> {
            DashboardScreen(
                onAddTransaction = { navigator.push(AddEditRoute()) },
                onOpenAdvisor = { navigator.navigateToTab(AdvisorTab) },
                onOpenSettings = { navigator.push(SettingsRoute) },
                onSeeAllHistory = { navigator.navigateToTab(HistoryTab) },
                onOpenCategoryBudgets = { navigator.push(CategoryBudgetsRoute) },
                onOpenDebts = { navigator.push(DebtsRoute) },
                onOpenSavings = { navigator.push(SavingsRoute) },
            )
        }
        entry<CategoryBudgetsRoute> {
            CategoryBudgetScreen(
                onNavigateBack = { navigator.pop() },
            )
        }
        entry<DebtsRoute> {
            DebtsScreen(
                onNavigateBack = { navigator.pop() },
            )
        }
        entry<SavingsRoute> {
            SavingsGoalsScreen(
                onNavigateBack = { navigator.pop() },
            )
        }
        entry<HistoryTab> {
            HistoryScreen(
                onAddTransaction = { navigator.push(AddEditRoute()) },
            )
        }
        entry<SubscriptionsTab> {
            SubscriptionsScreen()
        }
        entry<StatsTab> {
            StatsScreen()
        }
        entry<AdvisorTab> {
            AdvisorScreen()
        }
        entry<AddEditRoute> { key ->
            AddEditScreen(
                transactionId = key.transactionId,
                onNavigateBack = { navigator.pop() },
            )
        }
        entry<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navigator.pop() },
            )
        }
    }

    val isFullScreenRoute = navigator.currentRoute is AddEditRoute ||
        navigator.currentRoute is CategoryBudgetsRoute ||
        navigator.currentRoute is DebtsRoute ||
        navigator.currentRoute is SavingsRoute

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isFullScreenRoute) {
                NavigationBar {
                    NAV_ITEMS.forEach { item ->
                        val isSelected = item.key == navigator.topLevelRoute && item.key == navigator.currentRoute
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { navigator.navigateToTab(item.key) },
                            icon = {
                                when (val ic = item.icon) {
                                    is TabIcon.Vector -> Icon(ic.imageVector, contentDescription = stringResource(item.labelRes))
                                    is TabIcon.Resource -> Icon(painterResource(ic.resId), contentDescription = stringResource(item.labelRes))
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            entries = navigator.toDecoratedEntries(entryProvider),
            onBack = { navigator.pop() },
            transitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { (it * 0.22f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                )) togetherWith (slideOutHorizontally(
                    targetOffsetX = { (-it * 0.15f).toInt() },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                ))
            },
            popTransitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { (-it * 0.15f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                )) togetherWith (slideOutHorizontally(
                    targetOffsetX = { (it * 0.22f).toInt() },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                ))
            },
            predictivePopTransitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { (-it * 0.15f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                )) togetherWith (slideOutHorizontally(
                    targetOffsetX = { (it * 0.22f).toInt() },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                ))
            },
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }

    availableRelease?.let { release ->
        UpdateAvailableDialog(
            release = release,
            downloadProgress = downloadProgress,
            isDownloading = isDownloading,
            onDismiss = { availableRelease = null },
            onConfirmUpdate = {
                isDownloading = true
                scope.launch {
                    val file = container.updateManager.downloadApk(release.downloadUrl) { progress ->
                        downloadProgress = progress
                    }
                    isDownloading = false
                    if (file != null) {
                        availableRelease = null
                        container.updateManager.installApk(file)
                    }
                }
            },
        )
    }
}
