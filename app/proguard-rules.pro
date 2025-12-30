# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================
# Firebase Cloud Messaging (FCM)
# ============================================================
# Keep FCM service class
-keep class com.emisure.app.EmisureFCMService { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Firebase model classes (for data deserialization)
-keepclassmembers class com.google.firebase.messaging.RemoteMessage { *; }
-keepclassmembers class com.google.firebase.messaging.RemoteMessage$Notification { *; }

# ============================================================
# Notifications
# ============================================================
# Keep NotificationHelper (singleton object)
-keep class com.emisure.app.NotificationHelper { *; }

# Keep NotificationCompat classes
-keep class androidx.core.app.NotificationCompat** { *; }
-keep class androidx.core.app.NotificationManagerCompat { *; }

# ============================================================
# Device Admin / Device Policy
# ============================================================
# Keep Device Admin Receiver (required for device owner functionality)
-keep class com.emisure.app.EmisureDeviceAdminReceiver { *; }
-keep class com.emisure.app.DevicePolicyService { *; }

# ============================================================
# Crashlytics
# ============================================================
# Keep Crashlytics classes for proper crash reporting
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Crashlytics NDK
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# ============================================================
# General Android / Kotlin
# ============================================================
# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ============================================================
# Compose (if using R8 full mode)
# ============================================================
-dontwarn androidx.compose.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}