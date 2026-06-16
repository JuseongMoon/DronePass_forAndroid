import java.util.Properties
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    jacoco
}

// local.properties에서 API 키 로드
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}
val webClientId = localProperties.getProperty("WEB_CLIENT_ID")?.trim().orEmpty()

// keystore.properties 에서 Release 서명 정보 로드 (CI/로컬 모두 지원)
val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

val releaseStoreFilePath = keystoreProperties.getProperty("storeFile")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
val releaseStoreFile = releaseStoreFilePath?.let { rootProject.file(it) }
val releaseStorePassword = keystoreProperties.getProperty("storePassword")
    ?.trim()
    ?.takeIf { it.isNotEmpty() && it != "YOUR_STORE_PASSWORD" }
val releaseKeyAlias = keystoreProperties.getProperty("keyAlias")
    ?.trim()
    ?.takeIf { it.isNotEmpty() && it != "YOUR_KEY_ALIAS" }
val releaseKeyPassword = keystoreProperties.getProperty("keyPassword")
    ?.trim()
    ?.takeIf { it.isNotEmpty() && it != "YOUR_KEY_PASSWORD" }
val hasReleaseSigningConfig = releaseStoreFile?.exists() == true &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null
val releaseSigningErrorMessage =
    "Release signing is not configured. Copy keystore.properties.example to " +
        "keystore.properties, fill the real signing values, and make sure storeFile exists."
val releaseWebClientIdErrorMessage =
    "Google sign-in WEB_CLIENT_ID is not configured. Set WEB_CLIENT_ID in " +
        "local.properties to the Firebase Web client ID before building release artifacts."
val releaseReadinessErrorMessage: String?
    get() = listOfNotNull(
        releaseSigningErrorMessage.takeUnless { hasReleaseSigningConfig },
        releaseWebClientIdErrorMessage.takeUnless { webClientId.isNotBlank() },
    ).takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")

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
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    signingConfigs {
        // Release 서명 설정. keystore.properties + 키스토어 파일이 완전할 때만 생성한다.
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            signingConfig = signingConfigs.findByName("release")
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

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val shapeParsingCoverageExecutionData = fileTree(layout.buildDirectory) {
    include(
        "jacoco/testDebugUnitTest.exec",
        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
    )
}

val shapeParsingCoverageClassDirectories = files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include(
            "com/ScienceFiction/DronePassAndroid/core/data/remote/firebase/ShapeFirebaseStoreKt.class",
        )
    },
)

tasks.register<JacocoReport>("shapeParsingCoverageReport") {
    dependsOn("testDebugUnitTest")

    executionData(shapeParsingCoverageExecutionData)
    classDirectories.setFrom(shapeParsingCoverageClassDirectories)
    sourceDirectories.setFrom(files("src/main/java"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("shapeParsingCoverageVerification") {
    dependsOn("shapeParsingCoverageReport")

    executionData(shapeParsingCoverageExecutionData)
    classDirectories.setFrom(shapeParsingCoverageClassDirectories)
    sourceDirectories.setFrom(files("src/main/java"))

    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

dependencies {
    // appcompat 1.7.0 — AppCompatDelegate.setApplicationLocales / LocaleListCompat 사용 (Phase 3 언어 변경).
    // iOS UserDefaults `AppleLanguages` 대응. minSdk 28 호환.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // 네이버 Maps SDK
    implementation(libs.naver.map)

    // Google Play Services - Location
    implementation(libs.play.services.location)

    // Coroutines Play Services (await() for Task)
    implementation(libs.kotlinx.coroutines.play.services)

    // Accompanist Permissions (권한 처리)
    implementation(libs.accompanist.permissions)

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

val validateReleaseReadiness by tasks.registering {
    doLast {
        releaseReadinessErrorMessage?.let { errorMessage ->
            throw GradleException(errorMessage)
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach {
        dependsOn(validateReleaseReadiness)
    }

gradle.taskGraph.whenReady {
    if (allTasks.any { it.name == "assembleRelease" || it.name == "bundleRelease" }) {
        releaseReadinessErrorMessage?.let { errorMessage ->
            throw GradleException(errorMessage)
        }
    }
}
