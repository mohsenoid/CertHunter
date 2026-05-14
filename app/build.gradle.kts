plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

val releaseStoreFile = providers.gradleProperty("releaseStoreFile").orNull
val releaseStorePassword = providers.gradleProperty("releaseStorePassword").orNull
val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orNull
val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orNull

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
    autoCorrect = true
    reports {
        html.required = true
        xml.required = false
        txt.required = false
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

android {
    namespace = "com.mohsenoid.certhunter"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mohsenoid.certhunter"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = rootProject.file(releaseStoreFile)
            }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // ─── AndroidX core ───────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    coreLibraryDesugaring(libs.android.tools.desugar.jdk.libs)

    // ─── Jetpack Compose ─────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // ─── Navigation ──────────────────────────────────────────────────────────
    implementation(libs.bundles.navigation3)

    // ─── Dependency injection ─────────────────────────────────────────────────
    implementation(libs.bundles.koin)

    // ─── Networking / image loading ───────────────────────────────────────────
    implementation(libs.coil.compose)

    // ─── Serialization ────────────────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ─── Logging ─────────────────────────────────────────────────────────────
    implementation(platform(libs.klogx.bom))
    implementation(libs.bundles.klogx)

    // ─── Utilities ────────────────────────────────────────────────────────────
    implementation(libs.kotlin.result)

    // ─── Static analysis ──────────────────────────────────────────────────────
    detektPlugins(libs.detekt.formatting)

    // ─── Unit tests ───────────────────────────────────────────────────────────
    testImplementation(libs.bundles.unit.test)
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit5.engine)

    // ─── Instrumented tests ───────────────────────────────────────────────────
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.compose.android.test)

    // ─── Debug only ───────────────────────────────────────────────────────────
    debugImplementation(libs.bundles.compose.debug)
}
