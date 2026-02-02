package com.mehmetmertmazici.libraryauapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * BorrowedBook Model
 * Ödünç alınan kitap işlemlerini temsil eder
 *
 * iOS Karşılığı: BorrowedBook.swift
 */
data class BorrowedBook(
    @DocumentId
    val id: String? = null,
    val bookCopyId: String = "",
    val studentId: String = "",
    val borrowDate: Timestamp = Timestamp.now(),
    val dueDate: Timestamp = Timestamp.now(),
    val returnDate: Timestamp? = null,
    val isReturned: Boolean = false
) {
    /**
     * Yeni ödünç kaydı oluşturma için secondary constructor
     */
    constructor(
        bookCopyId: String,
        studentId: String,
        borrowDays: Int = 14
    ) : this(
        id = null,
        bookCopyId = bookCopyId,
        studentId = studentId,
        borrowDate = Timestamp.now(),
        dueDate = calculateDueDate(borrowDays),
        returnDate = null,
        isReturned = false
    )

    companion object {
        private fun calculateDueDate(borrowDays: Int): Timestamp {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, borrowDays)
            return Timestamp(calendar.time)
        }

        private val dateFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
        }
    }

    // ── Computed Properties ──

    /** Gecikme durumu kontrolü */
    val isOverdue: Boolean
        get() {
            if (isReturned) return false
            return Date().after(dueDate.toDate())
        }

    /** Gecikme gün sayısı */
    val overdueDays: Int
        get() {
            if (!isOverdue) return 0
            val diff = Date().time - dueDate.toDate().time
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        }

    /** Durum metni */
    val statusText: String
        get() = when {
            isReturned -> "İade Edildi"
            isOverdue -> "Süresi Geçti ($overdueDays gün)"
            else -> "Ödünçte"
        }

    /** Durum rengi */
    val statusColor: String
        get() = when {
            isReturned -> "green"
            isOverdue -> "red"
            else -> "blue"
        }

    /** Kalan gün sayısı (pozitif değer) */
    val remainingDays: Int
        get() {
            if (isReturned) return 0
            val diff = dueDate.toDate().time - Date().time
            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
            return maxOf(0, days)
        }

    /** Ödünç alma tarihi string */
    val borrowDateText: String
        get() = dateFormatter.format(borrowDate.toDate())

    /** Teslim tarihi string */
    val dueDateText: String
        get() = dateFormatter.format(dueDate.toDate())

    /** İade tarihi string (varsa) */
    val returnDateText: String?
        get() = returnDate?.let { dateFormatter.format(it.toDate()) }

    /** Gecikme durumu için emoji */
    val statusEmoji: String
        get() = when {
            isReturned -> "✅"
            isOverdue -> "⏰"
            else -> "📖"
        }
}

// ══════════════════════════════════════════════════════════════
// MARK: - BorrowingRule - İş Kuralları
// ══════════════════════════════════════════════════════════════

object BorrowingRule {
    const val MAX_BOOKS_PER_STUDENT = 3
    const val DEFAULT_BORROW_DAYS = 14
    const val WARNING_DAYS_BEFORE_DUE = 2

    /** Öğrencinin yeni kitap alabilir mi kontrolü */
    fun canBorrowBook(currentBorrowCount: Int): Boolean {
        return currentBorrowCount < MAX_BOOKS_PER_STUDENT
    }

    /**
     * Öğrencinin aynı kitaptan alabileceği kontrol (BookTemplate bazlı)
     */
    fun canBorrowSameBook(
        studentBorrowedBooks: List<BorrowedBook>,
        targetBookTemplateId: String,
        allBookCopies: List<BookCopy>
    ): Boolean {
        // Öğrencinin şu an ödünçte olan kitaplarını al
        val activeBorrowedCopyIds = studentBorrowedBooks
            .filter { !it.isReturned }
            .map { it.bookCopyId }

        // Bu kopyaların hangi kitap şablonlarına ait olduğunu bul
        val borrowedTemplateIds = allBookCopies
            .filter { it.id in activeBorrowedCopyIds }
            .map { it.bookTemplateId }

        // Hedef kitap şablonu zaten ödünçte mi kontrol et
        return targetBookTemplateId !in borrowedTemplateIds
    }
}