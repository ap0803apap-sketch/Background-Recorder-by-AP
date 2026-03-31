# Preserve AndroidX components
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# Preserve Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Preserve Kotlin metadata
-keep class kotlin.Metadata { *; }

# Preserve enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve data classes
-keep class com.ap.background.recorder.data.** { *; }
-keepclassmembers class com.ap.background.recorder.data.** { *; }

# Preserve service components
-keep class com.ap.background.recorder.services.** { *; }
-keepclassmembers class com.ap.background.recorder.services.** { *; }

# Preserve broadcast receivers
-keep class com.ap.background.recorder.receivers.** { *; }
-keepclassmembers class com.ap.background.recorder.receivers.** { *; }

# Preserve utility classes
-keep class com.ap.background.recorder.utils.** { *; }
-keepclassmembers class com.ap.background.recorder.utils.** { *; }

# Room database
-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    *;
}

# Biometric
-keep class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.** { *; }

# Camera2
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }

# Media3
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimization
-optimizationpasses 5
-dontskipnonpubliclibraryclasses
-verbose