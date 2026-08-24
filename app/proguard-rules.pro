# Security Protection & Obfuscation Rules
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Protect App package & Data Models
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }

# Keep Firebase Auth and Firestore
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.paging.**

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

