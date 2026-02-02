package com.mehmetmertmazici.libraryauapp.ui.navigation

/**
 * Screen
 * Uygulama içi navigasyon rotaları
 *
 * iOS Karşılığı:
 *   iOS'ta ContentView.swift içindeki @ViewBuilder switch-case
 *   + NavigationStack/NavigationLink ile yönetiliyor.
 *
 *   Android'de sealed class + NavHost ile type-safe navigasyon kullanıyoruz.
 */
sealed class Screen(val route: String) {

    // ── Auth Akışı ──
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object PendingApproval : Screen("pending_approval")

    // ── Ana Ekranlar (Tab İçerikleri) ──
    data object Main : Screen("main")

    // ── Kitaplar ──
    data object BookList : Screen("book_list")
    data object BookDetail : Screen("book_detail/{bookTemplateId}") {
        fun createRoute(bookTemplateId: String) = "book_detail/$bookTemplateId"
    }
    data object AddBook : Screen("add_book")
    data object BookEdit : Screen("book_edit/{bookTemplateId}") {
        fun createRoute(bookTemplateId: String) = "book_edit/$bookTemplateId"
    }
    data object AddCopy : Screen("add_copy/{bookTemplateId}") {
        fun createRoute(bookTemplateId: String) = "add_copy/$bookTemplateId"
    }

    // ── Öğrenciler ──
    data object StudentList : Screen("student_list")
    data object StudentDetail : Screen("student_detail/{studentId}") {
        fun createRoute(studentId: String) = "student_detail/$studentId"
    }
    data object AddStudent : Screen("add_student")

    // ── Ödünç İşlemleri ──
    data object BorrowBook : Screen("borrow_book")
    data object BorrowedBooks : Screen("borrowed_books")

    // ── Barkod Tarayıcı ──
    data object BarcodeScanner : Screen("barcode_scanner")

    // ── Yönetim ──
    data object AdminManagement : Screen("admin_management")
    data object Profile : Screen("profile")
    data object Trash : Screen("trash")
}