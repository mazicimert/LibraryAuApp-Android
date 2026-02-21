package com.mehmetmertmazici.libraryauapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mehmetmertmazici.libraryauapp.ui.books.AddBookScreen
import com.mehmetmertmazici.libraryauapp.ui.books.AddCopyScreen
import com.mehmetmertmazici.libraryauapp.ui.books.BookDetailScreen
import com.mehmetmertmazici.libraryauapp.ui.books.BookEditScreen
import com.mehmetmertmazici.libraryauapp.ui.books.BookListScreen
import com.mehmetmertmazici.libraryauapp.ui.components.NetworkStatusBanner
import com.mehmetmertmazici.libraryauapp.ui.students.AddStudentScreen
import com.mehmetmertmazici.libraryauapp.ui.students.StudentDetailScreen
import com.mehmetmertmazici.libraryauapp.ui.students.StudentListScreen
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBackground
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
                containerColor = Color.Transparent,
                bottomBar = {
                    ModernBottomNavigationBar(
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
            // Main content with AnkaraBackground
            AnkaraBackground {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(bottom = paddingValues.calculateBottomPadding())
                                        .statusBarsPadding()
                ) {
                    NavHost(navController = navController, startDestination = TabItem.Books.route) {
                        composable(TabItem.Books.route) {
                            BookListScreen(
                                    onBookClick = { book ->
                                        navController.navigate(
                                                Screen.BookDetail.createRoute(book.id ?: "")
                                        )
                                    },
                                    onBarcodeScanner = {
                                        navController.navigate(Screen.Scanner.route)
                                    },
                                    onAddBook = { navController.navigate(Screen.AddBook.route) }
                            )
                        }
                        composable(TabItem.Students.route) {
                            StudentListScreen(
                                    onStudentClick = { student ->
                                        navController.navigate(
                                                Screen.StudentDetail.createRoute(student.id ?: "")
                                        )
                                    },
                                    onAddStudent = {
                                        navController.navigate(Screen.AddStudent.route)
                                    }
                            )
                        }
                        composable(TabItem.Borrowing.route) {
                            PlaceholderScreen(title = "Ödünçler")
                        }
                        composable(TabItem.Admin.route) {
                            PlaceholderScreen(title = "Admin Yönetimi")
                        }
                        composable(TabItem.Profile.route) { PlaceholderScreen(title = "Profil") }

                        // BookDetail Screen
                        composable(
                                route = Screen.BookDetail.route,
                                arguments =
                                        listOf(navArgument("bookId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                            BookDetailScreen(
                                    bookId = bookId,
                                    onBack = { navController.popBackStack() },
                                    onEditBook = {
                                        navController.navigate(Screen.BookEdit.createRoute(bookId))
                                    },
                                    onAddCopy = {
                                        navController.navigate(Screen.AddCopy.createRoute(bookId))
                                    }
                            )
                        }

                        // AddBook Screen
                        composable(route = Screen.AddBook.route) {
                            AddBookScreen(
                                    onBack = { navController.popBackStack() },
                                    onBookAdded = { navController.popBackStack() }
                            )
                        }

                        // AddCopy Screen
                        composable(
                                route = Screen.AddCopy.route,
                                arguments =
                                        listOf(navArgument("bookId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                            AddCopyScreen(
                                    bookId = bookId,
                                    onBack = { navController.popBackStack() },
                                    onCopyAdded = { navController.popBackStack() }
                            )
                        }

                        // BookEdit Screen
                        composable(
                                route = Screen.BookEdit.route,
                                arguments =
                                        listOf(navArgument("bookId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                            BookEditScreen(
                                    bookId = bookId,
                                    onBack = { navController.popBackStack() },
                                    onBookUpdated = { navController.popBackStack() }
                            )
                        }

                        // StudentDetail Screen
                        composable(
                                route = Screen.StudentDetail.route,
                                arguments =
                                        listOf(
                                                navArgument("studentId") {
                                                    type = NavType.StringType
                                                }
                                        )
                        ) { backStackEntry ->
                            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                            StudentDetailScreen(
                                    studentId = studentId,
                                    onBack = { navController.popBackStack() }
                            )
                        }

                        // AddStudent Screen
                        composable(Screen.AddStudent.route) {
                            AddStudentScreen(onDismiss = { navController.popBackStack() })
                        }
                    }
                }
            }
        }

        // Network status banner overlay
        NetworkStatusBanner(isOnline = isOnline)
    }
}

/** Modern Bottom Navigation Bar Arka plan gradyanına uyumlu, glassmorphism efektli modern bar */
@Composable
private fun ModernBottomNavigationBar(
        items: List<TabItem>,
        currentRoute: String?,
        pendingAdminCount: Int,
        onItemClick: (TabItem) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Glassmorphism arka plan renkleri
    val barBackground =
            if (isDark) {
                Brush.verticalGradient(
                        colors =
                                listOf(
                                        AnkaraBlue.copy(alpha = 0.85f),
                                        Color(0xFF001A3D).copy(alpha = 0.95f)
                                )
                )
            } else {
                Brush.verticalGradient(
                        colors =
                                listOf(
                                        Color.White.copy(alpha = 0.75f),
                                        Color.White.copy(alpha = 0.90f)
                                )
                )
            }

    // Üst kenarlık / ayırıcı çizgi rengi
    val borderColor =
            if (isDark) {
                AnkaraLightBlue.copy(alpha = 0.25f)
            } else {
                AnkaraBlue.copy(alpha = 0.08f)
            }

    // Seçili ikon ve metin renkleri
    val selectedColor = if (isDark) AnkaraLightBlue else AnkaraBlue
    val unselectedColor =
            if (isDark) Color.White.copy(alpha = 0.50f) else AnkaraBlue.copy(alpha = 0.40f)
    val indicatorColor =
            if (isDark) AnkaraLightBlue.copy(alpha = 0.15f) else AnkaraLightBlue.copy(alpha = 0.15f)

    Column {
        // İnce üst ayırıcı çizgi
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(borderColor))

        // Navigation Bar
        NavigationBar(
                modifier = Modifier.background(barBackground).navigationBarsPadding(),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val showBadge = item == TabItem.Admin && pendingAdminCount > 0

                NavigationBarItem(
                        selected = isSelected,
                        onClick = { onItemClick(item) },
                        icon = {
                            val icon = if (isSelected) item.selectedIcon else item.unselectedIcon

                            if (showBadge) {
                                BadgedBox(
                                        badge = {
                                            Badge(
                                                    containerColor =
                                                            if (isDark) Color(0xFFFF6B6B)
                                                            else MaterialTheme.colorScheme.error,
                                                    contentColor = Color.White
                                            ) {
                                                Text(
                                                        text = pendingAdminCount.toString(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                ) {
                                    Icon(
                                            imageVector = icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight =
                                            if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                            )
                        },
                        colors =
                                NavigationBarItemDefaults.colors(
                                        selectedIconColor = selectedColor,
                                        selectedTextColor = selectedColor,
                                        unselectedIconColor = unselectedColor,
                                        unselectedTextColor = unselectedColor,
                                        indicatorColor = indicatorColor
                                )
                )
            }
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
    data object Books :
            TabItem(
                    route = Screen.Books.route,
                    title = "Kitaplar",
                    selectedIcon = Icons.Filled.MenuBook,
                    unselectedIcon = Icons.Outlined.MenuBook
            )

    data object Students :
            TabItem(
                    route = Screen.Students.route,
                    title = "Öğrenciler",
                    selectedIcon = Icons.Filled.Groups,
                    unselectedIcon = Icons.Outlined.Groups
            )

    data object Borrowing :
            TabItem(
                    route = Screen.Borrowing.route,
                    title = "Ödünçler",
                    selectedIcon = Icons.Filled.Book,
                    unselectedIcon = Icons.Outlined.Book
            )

    data object Admin :
            TabItem(
                    route = Screen.Admin.route,
                    title = "Adminler",
                    selectedIcon = Icons.Filled.AdminPanelSettings,
                    unselectedIcon = Icons.Outlined.AdminPanelSettings
            )

    data object Profile :
            TabItem(
                    route = Screen.Profile.route,
                    title = "Profil",
                    selectedIcon = Icons.Filled.AccountCircle,
                    unselectedIcon = Icons.Outlined.AccountCircle
            )
}

/** Placeholder screen for tabs */
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
