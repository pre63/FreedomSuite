package org.freedomsuite.files.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object FilesRoutes {
    const val FOLDERS = "folders"
    const val FOLDER = "folder"
    const val FILE = "file"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
}

@Composable
fun FilesApp(viewModel: FilesViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = FilesRoutes.FOLDERS) {
        composable(FilesRoutes.FOLDERS) {
            FolderListScreen(
                viewModel = viewModel,
                onOpenFolder = { folderId ->
                    navController.navigate("${FilesRoutes.FOLDER}/$folderId")
                },
                onSearch = { navController.navigate(FilesRoutes.SEARCH) },
                onSettings = { navController.navigate(FilesRoutes.SETTINGS) },
            )
        }
        composable(
            route = "${FilesRoutes.FOLDER}/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType }),
        ) { entry ->
            val folderId = entry.arguments?.getString("folderId") ?: return@composable
            FolderScreen(
                viewModel = viewModel,
                folderId = folderId,
                onBack = { navController.popBackStack() },
                onOpenFile = { fileId ->
                    navController.navigate("${FilesRoutes.FILE}/$fileId")
                },
            )
        }
        composable(
            route = "${FilesRoutes.FILE}/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.StringType }),
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId") ?: return@composable
            FileDetailScreen(
                viewModel = viewModel,
                fileId = fileId,
                onBack = { navController.popBackStack() },
                onOpenSimilar = { similarId ->
                    navController.navigate("${FilesRoutes.FILE}/$similarId")
                },
            )
        }
        composable(FilesRoutes.SETTINGS) {
            FilesSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(FilesRoutes.SEARCH) {
            PhotoSearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenFile = { fileId ->
                    navController.navigate("${FilesRoutes.FILE}/$fileId")
                },
            )
        }
    }
}
