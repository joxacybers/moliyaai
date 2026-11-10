package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

class AppNavigator(
    topLevelRouteState: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
    val startRoute: NavKey,
) {
    var topLevelRoute: NavKey by topLevelRouteState

    val currentRoute: NavKey
        get() = backStacks[topLevelRoute]?.lastOrNull() ?: topLevelRoute

    fun navigateToTab(tab: NavKey) {
        if (tab in backStacks.keys) {
            val targetStack = backStacks[tab]
            if (topLevelRoute == tab) {
                // Already on this tab: pop to root (e.g. dismiss Settings or AddEdit)
                while ((targetStack?.size ?: 0) > 1) {
                    targetStack?.removeLastOrNull()
                }
            } else {
                topLevelRoute = tab
            }
        }
    }

    fun push(route: NavKey) {
        val currentStack = backStacks[topLevelRoute] ?: return
        currentStack.add(route)
    }

    fun pop(): Boolean {
        val currentStack = backStacks[topLevelRoute] ?: return false
        if (currentStack.size > 1) {
            currentStack.removeLastOrNull()
            return true
        }
        if (topLevelRoute != startRoute) {
            topLevelRoute = startRoute
            return true
        }
        return false
    }

    fun popToRoot(tab: NavKey = topLevelRoute) {
        val stack = backStacks[tab]
        while ((stack?.size ?: 0) > 1) {
            stack?.removeLastOrNull()
        }
    }

    fun isAtRoot(): Boolean {
        val currentStack = backStacks[topLevelRoute] ?: return true
        return currentStack.size <= 1 && topLevelRoute == startRoute
    }
}

@Composable
fun rememberAppNavigator(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): AppNavigator {
    val topLevelRouteState = remember { mutableStateOf(startRoute) }
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }
    return remember(startRoute, topLevelRoutes) {
        AppNavigator(
            topLevelRouteState = topLevelRouteState,
            backStacks = backStacks,
            startRoute = startRoute,
        )
    }
}

@Composable
fun AppNavigator.toDecoratedEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val decorated = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    val activeTabs = if (topLevelRoute == startRoute) {
        listOf(startRoute)
    } else {
        listOf(startRoute, topLevelRoute)
    }
    return activeTabs.flatMap { decorated[it] ?: emptyList() }
}
