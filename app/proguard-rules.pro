# ============================================================
# DronePass Android - ProGuard/R8 Rules
# ============================================================

# ── Crash Reporting ──────────────────────────────────────────
# 크래시 리포트에서 원본 라인 번호를 유지합니다.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Release Logging ──────────────────────────────────────────
# Release 빌드에서는 사용자/동기화 식별자가 포함될 수 있는 android.util.Log 호출을 제거합니다.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ── Hilt / Dagger ────────────────────────────────────────────
# Hilt가 생성한 컴포넌트 및 모듈을 유지합니다.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <init>(...);
}
# Hilt generated components
-keep class **_HiltModules* { *; }
-keep class **_HiltComponents* { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }

# ── Room ─────────────────────────────────────────────────────
# Room 엔티티 및 DAO를 유지합니다.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ── Retrofit + OkHttp ────────────────────────────────────────
# Retrofit API 인터페이스를 유지합니다.
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation interface retrofit2.Response
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Moshi ────────────────────────────────────────────────────
# Moshi JSON 직렬화 클래스를 유지합니다.
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}
# Moshi Kotlin reflection
-keep class kotlin.reflect.jvm.internal.** { *; }

# ── Firebase ─────────────────────────────────────────────────
# Firebase SDK 클래스를 유지합니다.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Crashlytics
-keepattributes *Annotation*
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# ── Naver Maps SDK ───────────────────────────────────────────
# 네이버 지도 SDK 클래스를 유지합니다.
-keep class com.naver.maps.** { *; }
-keep interface com.naver.maps.** { *; }
-dontwarn com.naver.maps.**

# ── Kotlin Coroutines ────────────────────────────────────────
# 코루틴 내부 클래스를 유지합니다.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.android.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Kotlin Serialization ────────────────────────────────────
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }

# ── Data Classes (도메인 모델 및 원격 데이터 클래스) ─────────
# 직렬화에 사용되는 데이터 클래스를 유지합니다.
-keep class com.ScienceFiction.DronePassAndroid.domain.model.** { *; }
-keep class com.ScienceFiction.DronePassAndroid.core.data.remote.** { *; }

# ── Google Credential Manager / Google Sign-In ───────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

# ── AndroidX Security (EncryptedSharedPreferences) ───────────
-keep class androidx.security.crypto.** { *; }

# ── Jetpack Compose ──────────────────────────────────────────
# Compose는 R8 full mode와 호환되지만, 안정성을 위해 유지합니다.
-dontwarn androidx.compose.**

# ── General Android ──────────────────────────────────────────
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
