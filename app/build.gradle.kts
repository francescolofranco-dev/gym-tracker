import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Play upload-key credentials live in an untracked root keystore.properties; without it,
// release artifacts are built unsigned so the project still builds on other machines.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

// Monotonic, reproducible versionCode: the number of commits reachable from HEAD.
// CI must check out full history (fetch-depth: 0) for this to match local builds.
val gitCommitCount = providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
    .standardOutput.asText.map { it.trim().toInt() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "dev.francescolofranco.gymtracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.lofrancolabs.gymtracker"
        minSdk = 31
        targetSdk = 37
        versionCode = gitCommitCount.get()
        versionName = "1.0.${gitCommitCount.get()}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // Production builds should exercise the same optimized code users install.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            // Installable on top of the normal debug app (same application id + debug key), but
            // runs the optimized, non-debuggable release code path used for honest frame testing.
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    playConfigs {
        // Benchmark builds are for local device testing only, never Play uploads.
        register("benchmark") { enabled.set(false) }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// Play uploads: `./gradlew :app:publishReleaseBundle` (see README "Play Store release").
// Credentials come from keystore.properties `playServiceAccount` or ANDROID_PUBLISHER_CREDENTIALS.
play {
    keystoreProperties.getProperty("playServiceAccount")?.let { serviceAccountCredentials.set(file(it)) }
    defaultToAppBundles.set(true)
    track.set("alpha")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.play.services.auth)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
