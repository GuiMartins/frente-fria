# kotlinx.serialization: o backup e o conteúdo usam serializers gerados.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.frentefria.** {
    *** Companion;
}
-keepclasseswithmembers class com.frentefria.** {
    kotlinx.serialization.KSerializer serializer(...);
}
