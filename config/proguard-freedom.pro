# Freedom Suite release ProGuard rules — strip all logging, no crash SDKs

# --- Strip Android Log ---
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# --- Strip Timber ---
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}
-assumenosideeffects class timber.log.Timber$Forest {
    public *** v(...);
    public *** d(...);
    public *** i(...);
    public *** w(...);
    public *** e(...);
    public *** wtf(...);
}

# --- Keep BuildConfig for privacy flags ---
-keep class org.freedomsuite.**.*BuildConfig { *; }

# --- Compose ---
-dontwarn androidx.compose.**

# --- SQLCipher ---
-keep class net.sqlcipher.** { *; }

# --- ONNX Runtime (Freedom Files ML) ---
-keep class ai.onnxruntime.** { *; }

# --- Strip known telemetry SDKs if ever accidentally added ---
-assumenosideeffects class com.google.firebase.** { *; }
-assumenosideeffects class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.errorprone.annotations.**
