import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mdnote.markdownnote"
    compileSdk = 36

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    defaultConfig {
        applicationId = "com.mdnote.markdownnote"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.environmentVariable("APP_VERSION_CODE")
            .orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("APP_VERSION_NAME")
            .orNull ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 签名配置
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) load(f.inputStream())
    }
    val releaseStoreFile = System.getenv("RELEASE_STORE_FILE")
        ?: localProps.getProperty("RELEASE_STORE_FILE")
    val releaseStorePassword = System.getenv("RELEASE_STORE_PASSWORD")
        ?: localProps.getProperty("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
        ?: localProps.getProperty("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        ?: localProps.getProperty("RELEASE_KEY_PASSWORD")
    val releaseRequested = gradle.startParameter.taskNames.any { taskName ->
        taskName.substringAfterLast(':').matches(
            Regex("(assemble|bundle|package|install|publish).*Release", RegexOption.IGNORE_CASE)
        )
    }
    if (releaseRequested) {
        require(!releaseStoreFile.isNullOrBlank()) { "Release signing requires RELEASE_STORE_FILE." }
        require(!releaseStorePassword.isNullOrBlank()) { "Release signing requires RELEASE_STORE_PASSWORD." }
        require(!releaseKeyAlias.isNullOrBlank()) { "Release signing requires RELEASE_KEY_ALIAS." }
        require(!releaseKeyPassword.isNullOrBlank()) { "Release signing requires RELEASE_KEY_PASSWORD." }
        require(file(releaseStoreFile).isFile) {
            "Release keystore does not exist: $releaseStoreFile"
        }
    }
    signingConfigs {
        create("release") {
            storeFile = file(releaseStoreFile ?: "missing-release-keystore")
            storePassword = releaseStorePassword.orEmpty()
            keyAlias = releaseKeyAlias.orEmpty()
            keyPassword = releaseKeyPassword.orEmpty()
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 打包时排除不必要的文件
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
        }
    }
}

// 排除冲突的 Jetbrains annotations 库
configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coil - 图片加载
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Markwon - Markdown 渲染引擎
    implementation(libs.markwon.core)
    implementation(libs.markwon.syntax.highlight)
    implementation(libs.markwon.image.coil)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.inline.parser)

    // OkHttp - AI API 请求
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // Security - 加密存储
    implementation(libs.security.crypto)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
