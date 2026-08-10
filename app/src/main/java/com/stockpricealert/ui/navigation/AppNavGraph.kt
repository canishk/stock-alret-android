package com.stockpricealert.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockpricealert.StockAlertApp
import com.stockpricealert.data.repository.StockRepository
import com.stockpricealert.ui.form.WatcherFormScreen
import com.stockpricealert.ui.form.WatcherFormViewModel
import com.stockpricealert.ui.list.WatcherListScreen
import com.stockpricealert.ui.list.WatcherListViewModel

object Routes {
    const val LIST = "list"
    const val FORM = "form?watcherId={watcherId}"

    fun formRoute(watcherId: Long? = null): String {
        return if (watcherId == null) {
            "form?watcherId=-1"
        } else {
            "form?watcherId=$watcherId"
        }
    }
}

@Composable
fun AppNavGraph(
    repository: StockRepository,
    onDataChanged: () -> Unit
) {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val notificationManager = (application as StockAlertApp).notificationManager

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            val viewModel: WatcherListViewModel = viewModel(
                factory = WatcherListViewModel.Factory(
                    application = application,
                    repository = repository,
                    notificationManager = notificationManager,
                    onDataChanged = onDataChanged
                )
            )
            WatcherListScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate(Routes.formRoute()) },
                onEditClick = { id -> navController.navigate(Routes.formRoute(id)) }
            )
        }

        composable(
            route = Routes.FORM,
            arguments = listOf(
                navArgument("watcherId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val watcherId = backStackEntry.arguments?.getLong("watcherId")?.takeIf { it >= 0 }
            val viewModel: WatcherFormViewModel = viewModel(
                factory = WatcherFormViewModel.Factory(
                    repository = repository,
                    watcherId = watcherId,
                    onDataChanged = onDataChanged,
                    onSaved = { navController.popBackStack() }
                )
            )
            WatcherFormScreen(
                viewModel = viewModel,
                isEditing = watcherId != null,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
