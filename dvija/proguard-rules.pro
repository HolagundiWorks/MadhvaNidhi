-keep class com.paperdesk.launcher.** { *; }

# security-crypto pulls in Tink, which refers to annotation classes that are not packaged on Android.
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
