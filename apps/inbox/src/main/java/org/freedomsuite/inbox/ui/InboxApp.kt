package org.freedomsuite.inbox.ui

import android.content.Intent
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
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchDeepLinkHandler
import org.freedomsuite.core.ui.AccountSetupScreen

object InboxRoutes {
    const val SETUP = "setup"
    const val LIST = "list"
    const val MESSAGE = "message/{uid}"
    const val COMPOSE = "compose"
    const val SETTINGS = "settings"

    fun message(uid: Long) = "message/$uid"
}

@Composable
fun InboxApp(
    viewModel: InboxViewModel = viewModel(),
    launchIntent: Intent? = null,
) {
    val navController = rememberNavController()
    val hasAccount by viewModel.hasAccount.collectAsState()

    LaunchedEffect(hasAccount) {
        if (hasAccount) {
            navController.navigate(InboxRoutes.LIST) {
                popUpTo(InboxRoutes.SETUP) { inclusive = true }
            }
        }
    }

    LaunchedEffect(launchIntent, hasAccount) {
        if (!hasAccount) return@LaunchedEffect
        val link = SearchDeepLinkHandler.parse(launchIntent) ?: return@LaunchedEffect
        if (link.source == SearchBridge.Source.MAIL) {
            link.mailFolder?.let { viewModel.selectFolder(it) }
            link.mailUid?.let { uid ->
                viewModel.openMessage(uid, link.mailFolder)
                navController.navigate(InboxRoutes.message(uid))
            }
        }
        SearchDeepLinkHandler.consume(launchIntent)
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
                onCompose = {
                    viewModel.clearComposeDraft()
                    navController.navigate(InboxRoutes.COMPOSE)
                },
                onSettings = { navController.navigate(InboxRoutes.SETTINGS) },
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
                onReply = { message ->
                    viewModel.startReply(message)
                    navController.navigate(InboxRoutes.COMPOSE)
                },
            )
        }
        composable(InboxRoutes.COMPOSE) {
            ComposeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSent = { navController.popBackStack() },
            )
        }
        composable(InboxRoutes.SETTINGS) {
            InboxSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(InboxRoutes.SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
