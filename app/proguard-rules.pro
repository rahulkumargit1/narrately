# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools ProGuard configuration.

# Keep Room entities
-keep class com.example.pdfreader.data.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# Keep PDFBox
-keep class com.tomroush.pdfbox.** { *; }
-keep class org.apache.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.apache.**
