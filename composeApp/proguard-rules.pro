# R8 rules for the Boka release build.
#
# kotlinx.serialization (1.5+), Ktor, OkHttp and Media3 all ship their own R8 rules,
# so these are belt-and-braces: an API response must never fail to decode because
# R8 removed a compiler-generated serializer for one of our DTOs.
-keep,includedescriptorclasses class ke.co.brivont.boka.data.**$$serializer { *; }
-keepclassmembers class ke.co.brivont.boka.data.** {
    *** Companion;
}
-keepclasseswithmembers class ke.co.brivont.boka.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / OkHttp reference optional JVM-only libraries that are absent on Android.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# Keep line numbers so release crash stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
