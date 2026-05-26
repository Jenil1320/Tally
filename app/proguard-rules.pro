# Hilt
-keep class dagger.hilt.android.internal.** { *; }

# Room — keep entities and DAOs that R8 might otherwise strip.
-keep class com.tally.data.db.** { *; }

# Tink + Keystore reflection
-keep class com.google.crypto.tink.** { *; }

# Kotlin reflection (used by some Compose previews)
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
