package com.mehmetmertmazici.libraryauapp.data.repository

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mehmetmertmazici.libraryauapp.data.model.*
import com.mehmetmertmazici.libraryauapp.domain.util.BarcodeGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * FirebaseRepository Tüm Firebase Firestore işlemlerini yönetir
 *
 * iOS Karşılığı: FirebaseService.swift iOS: Combine Publisher → Android: Flow/suspend function
 */
@Singleton
class FirebaseRepository
@Inject
constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    // ── Collection Names ──
    private object Collections {
        const val BOOK_TEMPLATES = "bookTemplates"
        const val BOOK_COPIES = "bookCopies"
        const val STUDENTS = "students"
        const val ADMIN_USERS = "adminUsers"
        const val BORROWED_BOOKS = "borrowedBooks"
        const val APP_SETTINGS = "appSettings"
    }

    // ── State Flows ──
    private val _networkStatus = MutableStateFlow(NetworkStatus.CONNECTED)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _syncStatus = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val syncStatus: StateFlow<LoadingState> = _syncStatus.asStateFlow()

    init {
        enableOfflinePersistence()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Offline Persistence Setup
    // ══════════════════════════════════════════════════════════════

    private fun enableOfflinePersistence() {
        val settings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    setSizeBytes(100 * 1024 * 1024) // 100MB cache
                }
            )
        }
        firestore.firestoreSettings = settings
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - BookTemplate Operations
    // ══════════════════════════════════════════════════════════════

    /** Tüm kitap şablonlarını getir */
    suspend fun fetchBookTemplates(): Result<List<BookTemplate>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BOOK_TEMPLATES)
                    .whereEqualTo("isDeleted", false)
                    .orderBy("title")
                    .get()
                    .await()

            val templates =
                snapshot.documents.mapNotNull {
                    it.toObject(BookTemplate::class.java)?.copy(id = it.id)
                }
            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ID ile tek kitap şablonu getir */
    suspend fun fetchBookTemplateById(bookId: String): Result<BookTemplate> {
        return try {
            val document =
                firestore.collection(Collections.BOOK_TEMPLATES).document(bookId).get().await()

            val template = document.toObject(BookTemplate::class.java)?.copy(id = document.id)
            if (template != null) {
                Result.success(template)
            } else {
                Result.failure(Exception("Kitap bulunamadı"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Yeni kitap şablonu ekle */
    suspend fun addBookTemplate(template: BookTemplate): Result<String> {
        return try {
            val docRef = firestore.collection(Collections.BOOK_TEMPLATES).add(template).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Kitap şablonu güncelle */
    suspend fun updateBookTemplate(template: BookTemplate): Result<Unit> {
        return try {
            val id = template.id ?: throw FirebaseError.InvalidDocumentID
            firestore.collection(Collections.BOOK_TEMPLATES).document(id).set(template).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - BookCopy Operations
    // ══════════════════════════════════════════════════════════════

    /** Belirli kitap şablonuna ait kopyaları getir */
    suspend fun fetchBookCopies(bookTemplateId: String): Result<List<BookCopy>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BOOK_COPIES)
                    .whereEqualTo("bookTemplateId", bookTemplateId)
                    .whereEqualTo("isDeleted", false)
                    .orderBy("copyNumber")
                    .get()
                    .await()

            val copies =
                snapshot.documents.mapNotNull {
                    it.toObject(BookCopy::class.java)?.copy(id = it.id)
                }
            Result.success(copies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Tüm kitap kopyalarını getir */
    suspend fun fetchAllBookCopies(): Result<List<BookCopy>> {
        return try {
            val snapshot = firestore.collection(Collections.BOOK_COPIES).get().await()

            val copies =
                snapshot.documents.mapNotNull {
                    it.toObject(BookCopy::class.java)?.copy(id = it.id)
                }
            Result.success(copies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sonraki bookId'yi al */
    suspend fun getNextBookId(): Result<Int> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BOOK_COPIES)
                    .orderBy("bookId", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()

            val nextId =
                if (snapshot.documents.isNotEmpty()) {
                    val lastCopy = snapshot.documents.first().toObject(BookCopy::class.java)
                    (lastCopy?.bookId ?: 0) + 1
                } else {
                    1
                }
            println("📊 Sonraki bookId: $nextId")
            Result.success(nextId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Barkod ile kitap kopyası ara */
    suspend fun findBookCopy(barcode: String): Result<BookCopy?> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BOOK_COPIES)
                    .whereEqualTo("barcode", barcode)
                    .limit(1)
                    .get()
                    .await()

            val copy =
                snapshot.documents.firstOrNull()?.let {
                    it.toObject(BookCopy::class.java)?.copy(id = it.id)
                }
            Result.success(copy)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Yeni kitap kopyası ekle */
    suspend fun addBookCopy(copy: BookCopy): Result<String> {
        return try {
            val docRef = firestore.collection(Collections.BOOK_COPIES).add(copy).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Kitap kopyasını güncelle */
    suspend fun updateBookCopy(copy: BookCopy): Result<Unit> {
        return try {
            val id = copy.id ?: throw FirebaseError.InvalidDocumentID
            firestore.collection(Collections.BOOK_COPIES).document(id).set(copy).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Student Operations
    // ══════════════════════════════════════════════════════════════

    /** Tüm öğrencileri getir */
    suspend fun fetchStudents(): Result<List<Student>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.STUDENTS)
                    .whereEqualTo("isDeleted", false)
                    .orderBy("name")
                    .get()
                    .await()

            val students =
                snapshot.documents.mapNotNull {
                    it.toObject(Student::class.java)?.copy(id = it.id)
                }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Öğrenci numarası ile ara */
    suspend fun findStudent(studentNumber: String): Result<Student?> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.STUDENTS)
                    .whereEqualTo("studentNumber", studentNumber)
                    .limit(1)
                    .get()
                    .await()

            val student =
                snapshot.documents.firstOrNull()?.let {
                    it.toObject(Student::class.java)?.copy(id = it.id)
                }
            Result.success(student)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Öğrenci ID ile getir */
    suspend fun fetchStudentById(studentId: String): Result<Student?> {
        return try {
            val document =
                firestore.collection(Collections.STUDENTS).document(studentId).get().await()

            val student = document.toObject(Student::class.java)?.copy(id = document.id)
            if (student != null) {
                Result.success(student)
            } else {
                Result.failure(Exception("Öğrenci bulunamadı"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Yeni öğrenci ekle */
    suspend fun addStudent(student: Student): Result<String> {
        return try {
            val docRef = firestore.collection(Collections.STUDENTS).add(student).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Öğrenci güncelle */
    suspend fun updateStudent(student: Student): Result<Unit> {
        return try {
            val id = student.id ?: throw FirebaseError.InvalidDocumentID
            firestore.collection(Collections.STUDENTS).document(id).set(student).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - BorrowedBook Operations
    // ══════════════════════════════════════════════════════════════

    /** Tüm ödünç kayıtlarını getir */
    suspend fun fetchBorrowedBooks(): Result<List<BorrowedBook>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BORROWED_BOOKS)
                    .orderBy("borrowDate", Query.Direction.DESCENDING)
                    .get()
                    .await()

            val borrowedBooks =
                snapshot.documents.mapNotNull {
                    it.toObject(BorrowedBook::class.java)?.copy(id = it.id)
                }
            Result.success(borrowedBooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Aktif ödünç kayıtlarını getir */
    suspend fun fetchActiveBorrowedBooks(): Result<List<BorrowedBook>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BORROWED_BOOKS)
                    .whereEqualTo("isReturned", false)
                    .orderBy("dueDate")
                    .get()
                    .await()

            val borrowedBooks =
                snapshot.documents.mapNotNull {
                    it.toObject(BorrowedBook::class.java)?.copy(id = it.id)
                }
            Result.success(borrowedBooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Gecikmiş kitapları getir */
    suspend fun fetchOverdueBooks(): Result<List<BorrowedBook>> {
        return try {
            val now = Timestamp.now()
            val snapshot =
                firestore
                    .collection(Collections.BORROWED_BOOKS)
                    .whereEqualTo("isReturned", false)
                    .whereLessThan("dueDate", now)
                    .orderBy("dueDate")
                    .get()
                    .await()

            val overdueBooks =
                snapshot.documents.mapNotNull {
                    it.toObject(BorrowedBook::class.java)?.copy(id = it.id)
                }
            Result.success(overdueBooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Öğrencinin aktif ödünç kayıtlarını getir */
    suspend fun fetchStudentActiveBorrowedBooks(studentId: String): Result<List<BorrowedBook>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BORROWED_BOOKS)
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("isReturned", false)
                    .get()
                    .await()

            val borrowedBooks =
                snapshot.documents.mapNotNull {
                    it.toObject(BorrowedBook::class.java)?.copy(id = it.id)
                }
            Result.success(borrowedBooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Yeni ödünç kaydı oluştur */
    suspend fun createBorrowRecord(borrowedBook: BorrowedBook): Result<String> {
        return try {
            val docRef = firestore.collection(Collections.BORROWED_BOOKS).add(borrowedBook).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Kitap iade işlemi */
    suspend fun returnBook(borrowedBookId: String): Result<Unit> {
        return try {
            firestore
                .collection(Collections.BORROWED_BOOKS)
                .document(borrowedBookId)
                .update(mapOf("isReturned" to true, "returnDate" to Timestamp.now()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - AdminUser Operations
    // ══════════════════════════════════════════════════════════════

    /** Tüm admin kullanıcılarını getir */
    suspend fun fetchAdminUsers(): Result<List<AdminUser>> {
        return try {
            val snapshot =
                firestore.collection(Collections.ADMIN_USERS).orderBy("createdAt").get().await()

            val adminUsers =
                snapshot.documents.mapNotNull {
                    it.toObject(AdminUser::class.java)?.copy(id = it.id)
                }
            println("✅ fetchAdminUsers başarılı: ${adminUsers.size} admin")
            Result.success(adminUsers)
        } catch (e: Exception) {
            println("❌ fetchAdminUsers hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Onay bekleyen adminleri getir */
    suspend fun fetchPendingAdmins(): Result<List<AdminUser>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.ADMIN_USERS)
                    .whereEqualTo("isApproved", false)
                    .whereEqualTo("isSuperAdmin", false)
                    .orderBy("createdAt")
                    .get()
                    .await()

            val pendingAdmins =
                snapshot.documents.mapNotNull {
                    it.toObject(AdminUser::class.java)?.copy(id = it.id)
                }
            println("✅ fetchPendingAdmins başarılı: ${pendingAdmins.size} bekleyen")
            Result.success(pendingAdmins)
        } catch (e: Exception) {
            println("❌ fetchPendingAdmins hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Admin kullanıcı kaydet/güncelle */
    suspend fun saveAdminUser(adminUser: AdminUser): Result<Unit> {
        return try {
            val id = adminUser.id ?: throw FirebaseError.InvalidDocumentID
            firestore.collection(Collections.ADMIN_USERS).document(id).set(adminUser).await()
            println("✅ saveAdminUser başarılı: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ saveAdminUser hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Admin onaylama */
    suspend fun approveAdmin(adminId: String): Result<Unit> {
        return try {
            firestore
                .collection(Collections.ADMIN_USERS)
                .document(adminId)
                .update("isApproved", true)
                .await()
            println("✅ approveAdmin başarılı: $adminId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ approveAdmin hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Admin kullanıcı sil */
    suspend fun deleteAdminUser(adminId: String): Result<Unit> {
        return try {
            firestore.collection(Collections.ADMIN_USERS).document(adminId).delete().await()
            println("✅ deleteAdminUser başarılı: $adminId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ deleteAdminUser hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Kullanıcı bilgilerini getir */
    suspend fun fetchAdminUser(userId: String): Result<AdminUser?> {
        return try {
            val document =
                firestore.collection(Collections.ADMIN_USERS).document(userId).get().await()

            val adminUser = document.toObject(AdminUser::class.java)?.copy(id = document.id)
            println("✅ fetchAdminUser başarılı: ${adminUser?.displayName}")
            Result.success(adminUser)
        } catch (e: Exception) {
            println("❌ fetchAdminUser hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Süper admin sayısını getir */
    suspend fun fetchSuperAdminCount(): Result<Int> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.ADMIN_USERS)
                    .whereEqualTo("isSuperAdmin", true)
                    .get()
                    .await()

            val count = snapshot.documents.size
            println("✅ fetchSuperAdminCount başarılı: $count süper admin")
            Result.success(count)
        } catch (e: Exception) {
            println("❌ fetchSuperAdminCount hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Onay bekleyen adminleri gerçek zamanlı dinle */
    fun listenToPendingAdmins(): Flow<List<AdminUser>> = callbackFlow {
        val listener =
            firestore
                .collection(Collections.ADMIN_USERS)
                .whereEqualTo("isApproved", false)
                .whereEqualTo("isSuperAdmin", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val pendingAdmins =
                        snapshot?.documents?.mapNotNull {
                            it.toObject(AdminUser::class.java)?.copy(id = it.id)
                        }
                            ?: emptyList()

                    trySend(pendingAdmins)
                }

        awaitClose { listener.remove() }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - App Settings Operations
    // ══════════════════════════════════════════════════════════════

    /** Uygulama ayarlarını getir */
    suspend fun fetchAppSettings(): Result<AppSettings?> {
        return try {
            val document =
                firestore.collection(Collections.APP_SETTINGS).document("main").get().await()

            val settings = document.toObject(AppSettings::class.java)
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Uygulama ayarlarını güncelle */
    suspend fun updateAppSettings(settings: AppSettings): Result<Unit> {
        return try {
            firestore.collection(Collections.APP_SETTINGS).document("main").set(settings).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - First Launch Data Loading
    // ══════════════════════════════════════════════════════════════

    /** JSON dosyasından kitapları yükle */
    suspend fun loadBooksFromJSON(): Result<Unit> {
        return try {
            val jsonString =
                context.assets.open("books.json").bufferedReader().use { it.readText() }

            val gson = Gson()
            val type = object : TypeToken<List<BookData>>() {}.type
            val booksData: List<BookData> = gson.fromJson(jsonString, type)

            // Başlığı boş olanları filtrele
            val validBooks = booksData.filter { it.title.trim().isNotEmpty() }
            println("📊 Toplam: ${booksData.size}, Geçerli Kitap: ${validBooks.size}")

            validBooks.forEachIndexed { index, bookData ->
                val bookId = index + 1

                // ISBN temizliği
                val cleanISBN = bookData.isbn.filter { it.isDigit() }

                // BookTemplate oluştur
                val template = bookData.toBookTemplate()

                // Template'i kaydet
                val templateResult = addBookTemplate(template)
                val templateId = templateResult.getOrNull() ?: return@forEachIndexed

                // Kopyaları oluştur
                val copies =
                    BarcodeGenerator.createBookCopies(
                        bookId = bookId,
                        isbn = cleanISBN,
                        copyCount = bookData.copyCount,
                        bookTemplateId = templateId
                    )

                // Kopyaları kaydet
                copies.forEach { copy -> addBookCopy(copy) }
            }

            println("✅✅✅ Tüm kitaplar (${validBooks.size} adet) başarıyla yüklendi!")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ JSON yükleme hatası: ${e.message}")
            Result.failure(FirebaseError.DataNotFound)
        }
    }

    /** İlk kurulum kontrolü */
    suspend fun checkAndPerformFirstLaunchSetup(): Result<Boolean> {
        return try {
            val settingsResult = fetchAppSettings()
            val settings = settingsResult.getOrNull()

            val needsSetup = settings == null || settings.isFirstLaunch
            Result.success(needsSetup)
        } catch (e: Exception) {
            Result.success(true) // Hata durumunda kurulum gerekli
        }
    }

    /** İlk kurulumu tamamla */
    suspend fun completeFirstLaunchSetup(): Result<Unit> {
        return try {
            val settings = AppSettings().withFirstLaunchComplete()
            updateAppSettings(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Trash & Restore Operations
    // ══════════════════════════════════════════════════════════════

    /** Kitap şablonunu çöpe at (soft delete) */
    suspend fun deleteBookTemplate(id: String): Result<Unit> {
        return softDelete(Collections.BOOK_TEMPLATES, id)
    }

    /** Öğrenciyi çöpe at */
    suspend fun deleteStudent(id: String): Result<Unit> {
        return softDelete(Collections.STUDENTS, id)
    }

    /** Kitap kopyasını çöpe at */
    suspend fun deleteBookCopy(id: String): Result<Unit> {
        return softDelete(Collections.BOOK_COPIES, id)
    }

    /** Silinmiş kitapları getir */
    suspend fun fetchDeletedBookTemplates(): Result<List<BookTemplate>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BOOK_TEMPLATES)
                    .whereEqualTo("isDeleted", true)
                    .orderBy("deletedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

            val items =
                snapshot.documents.mapNotNull {
                    it.toObject(BookTemplate::class.java)?.copy(id = it.id)
                }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Silinmiş öğrencileri getir */
    suspend fun fetchDeletedStudents(): Result<List<Student>> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.STUDENTS)
                    .whereEqualTo("isDeleted", true)
                    .orderBy("deletedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

            val items =
                snapshot.documents.mapNotNull {
                    it.toObject(Student::class.java)?.copy(id = it.id)
                }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Silinen kitap kopyalarının sayısını getir
     * iOS Karşılığı: getDeletedCopyCount(for:)
     */
    suspend fun fetchDeletedBookCopyCount(bookTemplateId: String): Result<Int> {
        return try {
            val snapshot =
                firestore
                    .collection(Collections.BOOK_COPIES)
                    .whereEqualTo("bookTemplateId", bookTemplateId)
                    .whereEqualTo("isDeleted", true)
                    .get()
                    .await()
            Result.success(snapshot.documents.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Kitap şablonunu ve kopyalarını geri yükle */
    suspend fun restoreBookTemplate(id: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // Template'i geri getir
            val templateRef = firestore.collection(Collections.BOOK_TEMPLATES).document(id)
            batch.update(
                templateRef,
                mapOf("isDeleted" to false, "deletedAt" to FieldValue.delete())
            )

            // Kopyaları bul ve geri getir
            val copiesSnapshot =
                firestore
                    .collection(Collections.BOOK_COPIES)
                    .whereEqualTo("bookTemplateId", id)
                    .whereEqualTo("isDeleted", true)
                    .get()
                    .await()

            copiesSnapshot.documents.forEach { doc ->
                val copyRef = firestore.collection(Collections.BOOK_COPIES).document(doc.id)
                batch.update(
                    copyRef,
                    mapOf("isDeleted" to false, "deletedAt" to FieldValue.delete())
                )
            }

            batch.commit().await()
            println("✅ Kitap ve ${copiesSnapshot.documents.size} kopyası geri yüklendi.")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Geri yükleme hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /** Öğrenciyi geri yükle */
    suspend fun restoreStudent(id: String): Result<Unit> {
        return restoreItem(Collections.STUDENTS, id)
    }

    /** Kitap şablonunu kalıcı olarak sil */
    suspend fun permanentlyDeleteBookTemplate(id: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // Template'i sil
            val templateRef = firestore.collection(Collections.BOOK_TEMPLATES).document(id)
            batch.delete(templateRef)

            // Kopyaları bul ve sil
            val copiesSnapshot =
                firestore
                    .collection(Collections.BOOK_COPIES)
                    .whereEqualTo("bookTemplateId", id)
                    .get()
                    .await()

            copiesSnapshot.documents.forEach { doc ->
                val copyRef = firestore.collection(Collections.BOOK_COPIES).document(doc.id)
                batch.delete(copyRef)
            }

            batch.commit().await()
            println("🔥 Kitap ve tüm kopyaları kalıcı olarak silindi.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Öğrenciyi kalıcı olarak sil */
    suspend fun permanentlyDeleteStudent(id: String): Result<Unit> {
        return hardDelete(Collections.STUDENTS, id)
    }

    // ── Helper Methods ──

    private suspend fun softDelete(collection: String, documentId: String): Result<Unit> {
        return try {
            firestore
                .collection(collection)
                .document(documentId)
                .update(mapOf("isDeleted" to true, "deletedAt" to Timestamp.now()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreItem(collection: String, documentId: String): Result<Unit> {
        return try {
            firestore
                .collection(collection)
                .document(documentId)
                .update(mapOf("isDeleted" to false, "deletedAt" to FieldValue.delete()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun hardDelete(collection: String, documentId: String): Result<Unit> {
        return try {
            firestore.collection(collection).document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Firebase Error Types
// ══════════════════════════════════════════════════════════════

sealed class FirebaseError(override val message: String) : Exception(message) {
    data object InvalidDocumentID : FirebaseError("Geçersiz doküman ID'si")
    data object NetworkError : FirebaseError("Ağ bağlantı hatası")
    data object AuthenticationRequired : FirebaseError("Kimlik doğrulama gerekli")
    data object PermissionDenied : FirebaseError("Erişim izni yok")
    data object DataNotFound : FirebaseError("Veri bulunamadı")
    data object DocumentAlreadyExists : FirebaseError("Doküman zaten mevcut")
    data object InvalidData : FirebaseError("Geçersiz veri formatı")
}