package dev.a10101100.snipcraft.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.a10101100.snipcraft.feature.editor.EditorScreen
import dev.a10101100.snipcraft.feature.library.LibraryScreen
import dev.a10101100.snipcraft.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
object SettingsRoute

@Serializable
data class EditorRoute(val snippetId: String? = null)

@Composable
fun SnipNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backstackEntry by navController.currentBackStackEntryAsState()
    val isTopLevel = backstackEntry?.destination?.let {
        it.hasRoute(LibraryRoute::class) || it.hasRoute(SettingsRoute::class)
    } ?: true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = backstackEntry?.destination?.hasRoute(LibraryRoute::class) == true,
                        onClick = {
                            navController.navigate(LibraryRoute) {
                                popUpTo(LibraryRoute) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                if (backstackEntry?.destination?.hasRoute(LibraryRoute::class) == true)
                                    Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks,
                                contentDescription = "Library",
                            )
                        },
                        label = { Text("Library") },
                    )
                    NavigationBarItem(
                        selected = backstackEntry?.destination?.hasRoute(SettingsRoute::class) == true,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(LibraryRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (backstackEntry?.destination?.hasRoute(SettingsRoute::class) == true)
                                    Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings",
                            )
                        },
                        label = { Text("Settings") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LibraryRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<LibraryRoute> {
                LibraryScreen(
                    onSnippetClick = { id -> navController.navigate(EditorRoute(id)) },
                    onCreateSnippet = { navController.navigate(EditorRoute()) },
                )
            }
            composable<EditorRoute> { backStack ->
                EditorScreen(
                    snippetId = backStack.toRoute<EditorRoute>().snippetId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
