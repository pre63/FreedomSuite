package org.freedomsuite.messages.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchDeepLinkHandler

object MessagesRoutes {
    const val LIST = "channels"
    const val CHANNEL = "channel/{channelId}"
    const val CREATE = "create"
    const val SETTINGS = "settings"

    fun channel(id: String) = "channel/$id"
}

@Composable
fun MessagesApp(
    viewModel: MessagesViewModel = viewModel(),
    launchIntent: Intent? = null,
) {
    val navController = rememberNavController()

    LaunchedEffect(launchIntent) {
        val link = SearchDeepLinkHandler.parse(launchIntent) ?: return@LaunchedEffect
        if (link.source == SearchBridge.Source.MESSAGE) {
            navController.navigate(MessagesRoutes.channel(link.hitId))
        }
        SearchDeepLinkHandler.consume(launchIntent)
    }

    NavHost(navController = navController, startDestination = MessagesRoutes.LIST) {
        composable(MessagesRoutes.LIST) {
            ChannelListScreen(
                viewModel = viewModel,
                onChannelClick = { id -> navController.navigate(MessagesRoutes.channel(id)) },
                onCreateChannel = { navController.navigate(MessagesRoutes.CREATE) },
                onSettings = { navController.navigate(MessagesRoutes.SETTINGS) },
            )
        }
        composable(MessagesRoutes.CREATE) {
            CreateChannelScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.popBackStack()
                    navController.navigate(MessagesRoutes.channel(id))
                },
            )
        }
        composable(MessagesRoutes.SETTINGS) {
            MessagesSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = MessagesRoutes.CHANNEL,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
        ) { entry ->
            val channelId = entry.arguments?.getString("channelId") ?: return@composable
            ChannelScreen(
                viewModel = viewModel,
                channelId = channelId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
