# Room — entity field names map to DB column names; obfuscating them breaks the DB.
-keep class dev.a10101100.snipcraft.core.database.** { *; }

# kotlinx.serialization — @Serializable classes used for JSON backup and diagnostics export.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * { *; }

# WorkManager — worker class names are stored in WorkRequest and must survive R8.
-keep class dev.a10101100.snipcraft.core.accessibility.HealthWatchdogWorker { *; }

# Hilt — generated component and factory class names must be preserved.
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class **_HiltComponents* { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }

# Timber
-dontwarn org.jetbrains.annotations.**
