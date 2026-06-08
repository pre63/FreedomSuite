package org.freedomsuite.calendar.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object CalendarRoutes {
    const val LIST = "list"
    const val EVENT = "event/{uid}"
    const val CREATE = "create"

    fun event(uid: String) = "event/$uid"
}

@Composable
fun CalendarApp(viewModel: CalendarViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = CalendarRoutes.LIST,
    ) {
        composable(CalendarRoutes.LIST) {
            EventListScreen(
                viewModel = viewModel,
                onEventClick = { uid -> navController.navigate(CalendarRoutes.event(uid)) },
                onCreate = { navController.navigate(CalendarRoutes.CREATE) },
            )
        }
        composable(
            route = CalendarRoutes.EVENT,
            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
        ) { entry ->
            val uid = entry.arguments?.getString("uid") ?: return@composable
            EventDetailScreen(
                viewModel = viewModel,
                uid = uid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(CalendarRoutes.CREATE) {
            CreateEventScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCreated = { uid ->
                    navController.popBackStack()
                    navController.navigate(CalendarRoutes.event(uid))
                },
            )
        }
    }
}
