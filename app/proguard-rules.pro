# --- Optimización General de R8 ---
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# --- Jetpack Compose ---
-keepclassmembers class * extends androidx.compose.ui.node.Owner { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep @interface androidx.compose.runtime.Composable

# --- Retrofit & OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Gson / Serialization ---
# Preservar los DTOs para evitar que la ofuscación rompa el parsing JSON
-keep class com.example.inframonitor.data.models.** { *; }
-keepclassmembers class com.example.inframonitor.data.models.** { *; }
-keep class com.example.inframonitor.viewmodel.DashboardData { *; }

# Gson specific
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Vico (Graphs) ---
-keep class com.patrykandpatrick.vico.** { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    val handler;
}

# --- Logs ---
# Eliminar logs de sistema en Release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
