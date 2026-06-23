package com.localphotos.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.PermissionChecker
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.navigation.Screen
import com.localphotos.app.navigation.bottomNavItems
import com.localphotos.app.ui.detail.DetailScreen
import com.localphotos.app.ui.favorites.FavoritesScreen
import com.localphotos.app.ui.main.MainScreen
import com.localphotos.app.ui.theme.LocalPhotosTheme
import java.net.URLDecoder
import org.koin.java.KoinJavaComponent.get

class MainActivity : ComponentActivity() {
    private var permissionGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionGranted = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
        if (!permissionGranted) {
            permissionLauncher.launch(permission)
        }

        setContent {
            LocalPhotosTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!permissionGranted) {
                        PermissionDeniedScreen(onRetry = {
                            permissionLauncher.launch(permission)
                        })
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Storage permission needed to scan photos",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onRetry, modifier = Modifier.padding(16.dp)) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val repository = get<PhotoRepository>(PhotoRepository::class.java)
    val favoriteCount by repository.favoriteCount.collectAsState(initial = 0)
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route } && favoriteCount > 0

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Main.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Main.route) {
                MainScreen(onPhotoClick = { uri ->
                    navController.navigate(Screen.Detail.createRoute(uri))
                })
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(onPhotoClick = { uri ->
                    navController.navigate(Screen.Detail.createRoute(uri))
                })
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("uri") ?: return@composable
                val uri = URLDecoder.decode(encodedUri, "UTF-8")
                DetailScreen(
                    uri = uri,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
