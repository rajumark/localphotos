package com.localphotos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Category : Screen("category")
    data object Albums : Screen("albums")
    data object Faces : Screen("faces")
    data object Documents : Screen("documents")
    data object Labels : Screen("labels")
    data object Detail : Screen("detail/{uri}") {
        fun createRoute(uri: String) = "detail/${java.net.URLEncoder.encode(uri, "UTF-8")}"
    }
    data object AlbumDetail : Screen("album_detail/{bucketId}/{albumName}") {
        fun createRoute(bucketId: String, albumName: String) =
            "album_detail/${java.net.URLEncoder.encode(bucketId, "UTF-8")}/${java.net.URLEncoder.encode(albumName, "UTF-8")}"
    }
    data object LabelDetail : Screen("label_detail/{label}") {
        fun createRoute(label: String) =
            "label_detail/${java.net.URLEncoder.encode(label, "UTF-8")}"
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
        label = "Category",
        route = Screen.Category.route,
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView
    )
)

data class CategoryItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

val categoryItems = listOf(
    CategoryItem("Albums", Screen.Albums.route, Icons.Filled.PhotoAlbum),
    CategoryItem("Faces", Screen.Faces.route, Icons.Filled.Face),
    CategoryItem("Documents", Screen.LabelDetail.createRoute("Paper"), Icons.Filled.Description),
    CategoryItem("Labels", Screen.Labels.route, Icons.AutoMirrored.Filled.Label)
)
