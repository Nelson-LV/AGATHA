# Add project specific ProGuard rules here.
# See https://developer.android.com/build/shrink-code for more details.

# Keep Retrofit service interfaces and their generic signatures.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# kotlinx.serialization keeps its own consumer rules; nothing extra needed here for now.
