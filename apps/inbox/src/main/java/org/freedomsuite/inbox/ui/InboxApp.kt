package org.freedomsuite.inbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.freedomsuite.core.ui.AccountSetupScreen

object InboxRoutes {
    const val SETUP = "setup"
    const val LIST = "list"
    const val MESSAGE = "message/{uid}"
    const val COMPOSE = "compose"

    fun message(uid: Long) = "message/$uid"
}

@Composable
fun InboxApp(viewModel: InboxViewModel = viewModel()) {
    val navController = rememberNavController()
    val hasAccount by viewModel.hasAccount.collectAsState()

    LaunchedEffect(hasAccount) {
        if (hasAccount) {
            navController.navigate(InboxRoutes.LIST) {
                popUpTo(InboxRoutes.SETUP) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (hasAccount) InboxRoutes.LIST else InboxRoutes.SETUP,
    ) {
        composable(InboxRoutes.SETUP) {
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()
            val suggestManual by viewModel.suggestManualSetup.collectAsState()
            AccountSetupScreen(
                title = "Inbox",
                subtitle = "We find your mail server from your email domain. No tracking.",
                isLoading = isLoading,
                errorMessage = error,
                connectButtonText = "Connect",
                suggestManualSetup = suggestManual,
                onConfigure = { email, password, manual ->
                    viewModel.configureAccount(email, password, manual)
                },
            )
        }
        composable(InboxRoutes.LIST) {
            MessageListScreen(
                viewModel = viewModel,
                onMessageClick = { uid -> navController.navigate(InboxRoutes.message(uid)) },
                onCompose = { navController.navigate(InboxRoutes.COMPOSE) },
            )
        }
        composable(
            route = InboxRoutes.MESSAGE,
            arguments = listOf(navArgument("uid") { type = NavType.LongType }),
        ) { entry ->
            val uid = entry.arguments?.getLong("uid") ?: return@composable
            MessageDetailScreen(
                viewModel = viewModel,
                uid = uid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(InboxRoutes.COMPOSE) {
            ComposeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSent = { navController.popBackStack() },
            )
        }
    }
}
