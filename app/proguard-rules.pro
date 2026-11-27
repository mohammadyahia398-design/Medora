# Medora release rules (R8). Room and WorkManager ship their own rules.

# Readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# PDFBox for Android: loads fonts, glyph lists and color profiles by name from assets,
# and refers to optional image decoders that are not bundled.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.**
-dontwarn org.apache.**

# Room entities and DAOs (kept by name so the generated code and migrations always match).
-keep class com.example.medora.data.** { *; }
