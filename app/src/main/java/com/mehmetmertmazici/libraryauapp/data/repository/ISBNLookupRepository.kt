package com.mehmetmertmazici.libraryauapp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ISBNLookupRepository
 * ISBN'den kitap bilgisi çekme servisi
 *
 * iOS Karşılığı: ISBNLookupService.swift
 * iOS: Combine Publisher → Android: suspend function
 */
@Singleton
class ISBNLookupRepository @Inject constructor() {

    /**
     * Google Books API ile ISBN'den kitap bilgilerini çek
     * Başarısız olursa Open Library'yi dener
     */
    suspend fun fetchBookInfo(isbn: String): BookInfo? = withContext(Dispatchers.IO) {
        // ISBN'i temizle
        val cleanISBN = isbn.replace("-", "").trim()

        // Önce Google Books'u dene
        val googleResult = fetchFromGoogleBooks(cleanISBN)
        if (googleResult != null && googleResult.isValid) {
            return@withContext googleResult
        }

        // Başarısız olursa Open Library'yi dene
        return@withContext fetchFromOpenLibrary(cleanISBN)
    }

    /**
     * Google Books API
     */
    private fun fetchFromGoogleBooks(isbn: String): BookInfo? {
        return try {
            val urlString = "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseGoogleBooksResponse(response)
            } else {
                println("❌ Google Books API hatası: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            println("❌ Google Books API hatası: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Google Books yanıtını parse et
     */
    private fun parseGoogleBooksResponse(jsonString: String): BookInfo? {
        return try {
            val json = JSONObject(jsonString)
            val totalItems = json.optInt("totalItems", 0)

            if (totalItems == 0) return null

            val items = json.optJSONArray("items") ?: return null
            val firstItem = items.optJSONObject(0) ?: return null
            val volumeInfo = firstItem.optJSONObject("volumeInfo") ?: return null

            val title = volumeInfo.optString("title").takeIf { it.isNotEmpty() }
            val authorsArray = volumeInfo.optJSONArray("authors")
            val authors = if (authorsArray != null) {
                (0 until authorsArray.length()).mapNotNull { authorsArray.optString(it) }
                    .joinToString(", ")
            } else null

            val publisher = volumeInfo.optString("publisher").takeIf { it.isNotEmpty() }
            val description = volumeInfo.optString("description").takeIf { it.isNotEmpty() }
            val categoriesArray = volumeInfo.optJSONArray("categories")
            val category = categoriesArray?.optString(0)
            val publishedDate = volumeInfo.optString("publishedDate").takeIf { it.isNotEmpty() }

            BookInfo(
                title = title,
                author = authors,
                publisher = publisher,
                description = description,
                category = category,
                publishedDate = publishedDate
            )
        } catch (e: Exception) {
            println("❌ JSON parse hatası: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Open Library API (Yedek)
     */
    private fun fetchFromOpenLibrary(isbn: String): BookInfo? {
        return try {
            val urlString = "https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseOpenLibraryResponse(response, isbn)
            } else {
                println("❌ Open Library API hatası: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            println("❌ Open Library API hatası: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Open Library yanıtını parse et
     */
    private fun parseOpenLibraryResponse(jsonString: String, isbn: String): BookInfo? {
        return try {
            val json = JSONObject(jsonString)
            val bookData = json.optJSONObject("ISBN:$isbn") ?: return null

            val title = bookData.optString("title").takeIf { it.isNotEmpty() }

            val authorsArray = bookData.optJSONArray("authors")
            val authors = if (authorsArray != null) {
                (0 until authorsArray.length()).mapNotNull {
                    authorsArray.optJSONObject(it)?.optString("name")
                }.joinToString(", ")
            } else null

            val publishersArray = bookData.optJSONArray("publishers")
            val publisher = publishersArray?.optJSONObject(0)?.optString("name")

            BookInfo(
                title = title,
                author = authors,
                publisher = publisher,
                description = null,
                category = null,
                publishedDate = null
            )
        } catch (e: Exception) {
            println("❌ Open Library JSON parse hatası: ${e.localizedMessage}")
            null
        }
    }
}

/**
 * Kitap Bilgisi Model
 */
data class BookInfo(
    val title: String?,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val category: String?,
    val publishedDate: String?
) {
    val isValid: Boolean
        get() = !title.isNullOrBlank() || !author.isNullOrBlank()
}