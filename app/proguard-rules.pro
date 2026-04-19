# ─── Narrately ProGuard Rules ───

# Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep data classes for Room entities
-keep class com.example.pdfreader.data.** { *; }

# PDFBox
-dontwarn org.bouncycastle.**
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }

# TTS
-keep class android.speech.tts.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Compose — keep @Composable functions
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# R8 full mode
-allowaccessmodification
-repackageclasses
