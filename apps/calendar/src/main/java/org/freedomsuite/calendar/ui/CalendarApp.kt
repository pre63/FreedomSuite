package org.freedomsuite.calendar.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import org.freedomsuite.calendar.reminder.EventReminderScheduler
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchDeepLinkHandler
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object CalendarRoutes {
    const val LIST = "list"
    const val EVENT = "event/{uid}"
    const val CREATE = "create"
    const val EDIT = "event/{uid}/edit"

    fun event(uid: String) = "event/$uid"
    fun edit(uid: String) = "event/$uid/edit"
}

@Composable
fun CalendarApp(
    viewModel: CalendarViewModel = viewModel(),
    launchIntent: Intent? = null,
) {
    val navController = rememberNavController()

    LaunchedEffect(launchIntent) {
        val reminderUid = launchIntent?.getStringExtra(EventReminderScheduler.EXTRA_EVENT_UID)
        if (reminderUid != null) {
            navController.navigate(CalendarRoutes.event(reminderUid))
            launchIntent.removeExtra(EventReminderScheduler.EXTRA_EVENT_UID)
            return@LaunchedEffect
        }
        val link = SearchDeepLinkHandler.parse(launchIntent) ?: return@LaunchedEffect
        if (link.source == SearchBridge.Source.CALENDAR) {
            navController.navigate(CalendarRoutes.event(link.hitId))
        }
        SearchDeepLinkHandler.consume(launchIntent)
    }

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
                onEdit = { navController.navigate(CalendarRoutes.edit(uid)) },
            )
        }
        composable(
            route = CalendarRoutes.EDIT,
            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
        ) { entry ->
            val uid = entry.arguments?.getString("uid") ?: return@composable
            EditEventScreen(
                viewModel = viewModel,
                uid = uid,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
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
