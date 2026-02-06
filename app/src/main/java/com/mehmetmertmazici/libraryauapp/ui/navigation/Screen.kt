package com.mehmetmertmazici.libraryauapp.ui.navigation

/**
 * Navigation Routes
 * Uygulama içi navigasyon rotaları
 *
 * iOS Karşılığı: MainTabView.swift tab yapısı
 */
sealed class Screen(val route: String) {

    // ── Auth Screens ──
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object PendingApproval : Screen("pending_approval")

    // ── Main Tabs ──
    data object Books : Screen("books")
    data object Students : Screen("students")
    data object Borrowing : Screen("borrowing")
    data object Admin : Screen("admin")
    data object Profile : Screen("profile")

    // ── Detail Screens ──
    data object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: String) = "book_detail/$bookId"
    }

    data object StudentDetail : Screen("student_detail/{studentId}") {
        fun createRoute(studentId: String) = "student_detail/$studentId"
    }

    data object AddBook : Screen("add_book")
    data object AddCopy : Screen("add_copy/{bookId}") {
        fun createRoute(bookId: String) = "add_copy/$bookId"
    }
    data object BookEdit : Screen("book_edit/{bookId}") {
        fun createRoute(bookId: String) = "book_edit/$bookId"
    }
    data object AddStudent : Screen("add_student")
    data object Scanner : Screen("scanner")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")

    // ── Borrowing Screens ──
    data object BorrowBook : Screen("borrow_book/{studentId}") {
        fun createRoute(studentId: String) = "borrow_book/$studentId"
    }

    data object ReturnBook : Screen("return_book/{borrowedBookId}") {
        fun createRoute(borrowedBookId: String) = "return_book/$borrowedBookId"
    }
}