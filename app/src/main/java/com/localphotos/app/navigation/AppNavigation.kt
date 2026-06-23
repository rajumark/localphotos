package com.localphotos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Favorites : Screen("favorites")
    data object Detail : Screen("detail/{uri}") {
        fun createRoute(uri: String) = "detail/${java.net.URLEncoder.encode(uri, "UTF-8")}"
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "Photos",
        route = Screen.Main.route,
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary
    ),
    BottomNavItem(
        label = "Favorites",
        route = Screen.Favorites.route,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )
)
