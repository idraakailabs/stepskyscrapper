# Step Skyscraper R8 rules. Most libraries ship consumer rules; these cover reflection-based pieces.

# kotlinx.serialization: type-safe navigation routes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ai.idraak.stepskyscraper.**$$serializer { *; }

# Glance receivers are referenced from the manifest.
-keep class ai.idraak.stepskyscraper.widget.** extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# Crashlytics: readable stack traces with uploaded mapping files.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
