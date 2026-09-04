# Naze Vault - keep everything needed for reflection-free Compose app.
# Add project specific ProGuard rules here.

-keepattributes *Annotation*
-keepclassmembers class kotlin.Metadata { *; }

# Keep model classes used for local JSON index persistence
-keep class com.naze.vault.data.model.** { *; }
