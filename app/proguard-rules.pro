# Add project specific ProGuard rules here.
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep class com.tv.terminal.data.remote.model.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.exoplayer.** { *; }