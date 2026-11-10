# Project-specific R8 rules. Libraries used here (Hilt, Room, kotlinx.serialization, Ktor, OkHttp,
# Coil, Supabase) ship their own consumer rules; add rules here only for a verified R8 problem,
# with a comment explaining which class needed it.

# Keep line numbers for readable crash reports, but hide original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Ktor references optional JVM-only classes that do not exist on Android.
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
