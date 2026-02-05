package com.mehmetmertmazici.libraryauapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mehmetmertmazici.libraryauapp.ui.books.BookListScreen
import com.mehmetmertmazici.libraryauapp.ui.components.NetworkStatusBanner
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraLightBlue

/**
 * MainScreen Ana ekran - Bottom navigation ile tab yapısı
 *
 * iOS Karşılığı: MainTabView.swift iOS: TabView → Android: NavigationBar + NavHost
 */
@Composable
fun MainScreen(
    isSuperAdmin: Boolean,
    isOnline: Boolean,
    pendingAdminCount: Int = 0,
    onNavigateToScreen: (Screen) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tab items based on user role
    val tabItems =
        remember(isSuperAdmin) {
            if (isSuperAdmin) {
                listOf(
                    TabItem.Books,
                    TabItem.Students,
                    TabItem.Borrowing,
                    TabItem.Admin,
                    TabItem.Profile
                )
            } else {
                listOf(TabItem.Books, TabItem.Students, TabItem.Borrowing, TabItem.Profile)
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    items = tabItems,
                    currentRoute = currentRoute,
                    pendingAdminCount = if (isSuperAdmin) pendingAdminCount else 0,
                    onItemClick = { item ->
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Main content with gradient background
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            Color.White,
                                            AnkaraLightBlue.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                )
                        )
                        .padding(paddingValues)
            ) {
                NavHost(navController = navController, startDestination = TabItem.Books.route) {
                    composable(TabItem.Books.route) {
                        BookListScreen(
                            onBookClick = { book ->
                                navController.navigate(
                                    Screen.BookDetail.createRoute(book.id ?: "")
                                )
                            },
                            onBarcodeScanner = { navController.navigate(Screen.Scanner.route) },
                            onAddBook = { navController.navigate(Screen.AddBook.route) }
                        )
                    }
                    composable(TabItem.Students.route) {
                        // TODO: StudentListScreen()
                        PlaceholderScreen(title = "Öğrenciler")
                    }
                    composable(TabItem.Borrowing.route) {
                        // TODO: BorrowedBooksScreen()
                        PlaceholderScreen(title = "Ödünçler")
                    }
                    composable(TabItem.Admin.route) {
                        // TODO: AdminManagementScreen()
                        PlaceholderScreen(title = "Admin Yönetimi")
                    }
                    composable(TabItem.Profile.route) {
                        // TODO: ProfileScreen()
                        PlaceholderScreen(title = "Profil")
                    }
                }
            }
        }

        // Network status banner overlay
        NetworkStatusBanner(isOnline = isOnline)
    }
}

@Composable
private fun BottomNavigationBar(
    items: List<TabItem>,
    currentRoute: String?,
    pendingAdminCount: Int,
    onItemClick: (TabItem) -> Unit
) {
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.92f),
        contentColor = AnkaraBlue,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val showBadge = item == TabItem.Admin && pendingAdminCount > 0

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    if (showBadge) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(
                                        text = pendingAdminCount.toString(),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    if (isSelected) item.selectedIcon
                                    else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector =
                                if (isSelected) item.selectedIcon
                                else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight =
                            if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = AnkaraBlue,
                        selectedTextColor = AnkaraBlue,
                        unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                        unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
                        indicatorColor = AnkaraLightBlue.copy(alpha = 0.15f)
                    )
            )
        }
    }
}

/** Tab Items iOS tabItem karşılıkları */
sealed class TabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    // Kitaplar - books.vertical
    data object Books :
        TabItem(
            route = Screen.Books.route,
            title = "Kitaplar",
            selectedIcon = Icons.Filled.MenuBook,
            unselectedIcon = Icons.Outlined.MenuBook
        )

    // Öğrenciler - person.3
    data object Students :
        TabItem(
            route = Screen.Students.route,
            title = "Öğrenciler",
            selectedIcon = Icons.Filled.Groups,
            unselectedIcon = Icons.Outlined.Groups
        )

    // Ödünçler - book
    data object Borrowing :
        TabItem(
            route = Screen.Borrowing.route,
            title = "Ödünçler",
            selectedIcon = Icons.Filled.Book,
            unselectedIcon = Icons.Outlined.Book
        )

    // Adminler - person.badge.key
    data object Admin :
        TabItem(
            route = Screen.Admin.route,
            title = "Adminler",
            selectedIcon = Icons.Filled.AdminPanelSettings,
            unselectedIcon = Icons.Outlined.AdminPanelSettings
        )

    // Profil - person.circle
    data object Profile :
        TabItem(
            route = Screen.Profile.route,
            title = "Profil",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle
        )
}

/** Placeholder screen for tabs Gerçek ekranlar implement edilene kadar kullanılacak */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = AnkaraBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bu ekran yakında eklenecek",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
