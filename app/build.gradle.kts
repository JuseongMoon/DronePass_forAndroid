import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// local.properties에서 API 키 로드
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

// keystore.properties 에서 Release 서명 정보 로드 (CI/로컬 모두 지원)
// 파일이 없으면 release 빌드는 debug 키로 폴백한다. Play Store 출시 전 반드시 추가 필요.
val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

android {
    namespace = "com.ScienceFiction.DronePassAndroid"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ScienceFiction.DronePassAndroid"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Manifest placeholders (AndroidManifest.xml에서 사용)
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = localProperties.getProperty("NAVER_MAP_CLIENT_ID", "")

        // API 키 (local.properties에서 로드)
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"${localProperties.getProperty("NAVER_MAP_CLIENT_ID", "")}\"")
        buildConfigField("String", "NAVER_MAP_CLIENT_SECRET", "\"${localProperties.getProperty("NAVER_MAP_CLIENT_SECRET", "")}\"")
        buildConfigField("String", "VWORLD_API_KEY", "\"${localProperties.getProperty("VWORLD_API_KEY", "")}\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"${localProperties.getProperty("WEB_CLIENT_ID", "")}\"")
    }

    signingConfigs {
        // Release 서명 설정. keystore.properties + 키스토어 파일이 존재할 때만 생성한다.
        // 파일이 없으면 buildTypes.release 가 debug 키로 폴백 (Play Store 업로드 불가).
        val hasReleaseKeystore = keystoreProperties.getProperty("storeFile")?.let { path ->
            rootProject.file(path).exists()
        } ?: false

        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Firebase google-services.json 의 패키지명과 일치시키기 위해 applicationIdSuffix 미사용.
            // dev/prod 분리가 필요해지면 productFlavors 로 분리하고 Firebase 콘솔에 별도 앱 등록 필요.
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // keystore.properties 가 있으면 release 서명 사용, 없으면 debug 폴백
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // 네이버 Maps SDK
    implementation("com.naver.maps:map-sdk:3.23.1")

    // Google Play Services - Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coroutines Play Services (await() for Task)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Accompanist Permissions (권한 처리)
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room DB
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Retrofit / OkHttp / Moshi
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // Security (EncryptedSharedPreferences)
    implementation(libs.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
