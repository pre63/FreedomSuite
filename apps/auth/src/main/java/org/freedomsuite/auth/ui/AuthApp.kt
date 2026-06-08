package org.freedomsuite.auth.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object AuthRoutes {
    const val LIST = "list"
    const val ADD = "add"
    const val SETTINGS = "settings"
}

@Composable
fun AuthApp(viewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthRoutes.LIST) {
        composable(AuthRoutes.LIST) {
            AccountListScreen(
                viewModel = viewModel,
                onAdd = { navController.navigate(AuthRoutes.ADD) },
                onSettings = { navController.navigate(AuthRoutes.SETTINGS) },
            )
        }
        composable(AuthRoutes.ADD) {
            AddAccountScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAdded = { navController.popBackStack() },
            )
        }
        composable(AuthRoutes.SETTINGS) {
            AuthSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
