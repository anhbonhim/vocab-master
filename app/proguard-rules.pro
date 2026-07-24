# Keep Hilt generated classes
-keep class * extends top.heymoe.hilt.components.Component
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class dagger.hilt.internal.GeneratedComponentManagerHolder { *; }
-keep interface * extends dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep @dagger.hilt.InstallIn class * { *; }
-keep class * extends android.app.Application { *; }
-keep class com.nhimz.vocabmaster.VocabApplication_GeneratedInjector { *; }
-keep class * implements com.nhimz.vocabmaster.VocabApplication_GeneratedInjector { *; }