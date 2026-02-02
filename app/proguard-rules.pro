# ── LibraryAuApp ProGuard Rules ──

# ── Firebase Firestore ──
-keep class com.mehmetmertmazici.libraryauapp.data.model.** { *; }
-keepclassmembers class com.mehmetmertmazici.libraryauapp.data.model.** { *; }

# ── Kotlin Serialization ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class com.mehmetmertmazici.libraryauapp.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Gson ──
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# ── Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Compose ──
-dontwarn androidx.compose.**