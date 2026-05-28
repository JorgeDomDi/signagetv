# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.signagetv.tv.data.api.** { *; }
-keep class com.signagetv.tv.data.ws.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
