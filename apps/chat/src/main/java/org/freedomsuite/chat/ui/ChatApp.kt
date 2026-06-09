package org.freedomsuite.chat.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.freedomsuite.core.ui.FreedomScaffold

object ChatRoutes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun ChatApp() {
    val navController = rememberNavController()
    val vm: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = ChatRoutes.CHAT) {
        composable(ChatRoutes.CHAT) {
            ChatScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(ChatRoutes.SETTINGS) },
            )
        }
        composable(ChatRoutes.SETTINGS) {
            ChatSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
    }
}

