# Код приложения оставляем как есть: R8 сжимает только библиотеки.
# Так ничего не ломается из-за рефлексии (WorkManager, виджет, HCE-сервис NFC).
-keep class ntmt.schedule.** { *; }
-keepattributes SourceFile,LineNumberTable
-dontwarn org.jetbrains.annotations.**
