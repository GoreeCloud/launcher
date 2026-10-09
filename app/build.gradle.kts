import java.time.Duration

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room3")
}

val developmentVersionCode =
    sequenceOf(
        providers.environmentVariable("GOREECLOUD_DEV_VERSION_CODE").orNull,
        providers.environmentVariable("GITHUB_RUN_NUMBER").orNull,
    ).mapNotNull { raw -> raw?.toIntOrNull()?.takeIf { it > 0 } }
        .firstOrNull()
        ?: 1

val developmentKeystorePath =
    providers.environmentVariable("GOREECLOUD_DEV_KEYSTORE_PATH").orNull
val developmentKeystorePassword =
    providers.environmentVariable("GOREECLOUD_DEV_KEYSTORE_PASSWORD").orNull
val developmentKeyAlias =
    providers.environmentVariable("GOREECLOUD_DEV_KEY_ALIAS").orNull
val developmentKeyPassword =
    providers.environmentVariable("GOREECLOUD_DEV_KEY_PASSWORD").orNull
val developmentSigningValues =
    listOf(
        developmentKeystorePath,
        developmentKeystorePassword,
        developmentKeyAlias,
        developmentKeyPassword,
    )
val developmentSigningRequested = developmentSigningValues.any { !it.isNullOrBlank() }
val developmentSigningConfigured = developmentSigningValues.all { !it.isNullOrBlank() }

if (developmentSigningRequested && !developmentSigningConfigured) {
    throw GradleException(
        "Development signing configuration is incomplete. Provide all GOREECLOUD_DEV_KEYSTORE_* " +
            "environment variables or none of them.",
    )
}

if (developmentSigningConfigured && !file(developmentKeystorePath!!).isFile) {
    throw GradleException("Configured Development keystore path does not point to a file.")
}

android {
    namespace = "com.goreecloud.launcher"
    compileSdk = 36

    signingConfigs {
        if (developmentSigningConfigured) {
            create("development") {
                storeFile = file(developmentKeystorePath!!)
                storePassword = developmentKeystorePassword!!
                keyAlias = developmentKeyAlias!!
                keyPassword = developmentKeyPassword!!
            }
        }
    }
    defaultConfig {
        applicationId = "com.goreecloud.launcher"
        minSdk = 29
        targetSdk = 36
        versionCode = developmentVersionCode
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // AndroidJUnitRunner per-test timeout belongs in the DSL; a Gradle -P argument
        // is incompatible with configuration caching and may not reach the runner.
        testInstrumentationRunnerArguments["timeout_msec"] = "60000"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            if (developmentSigningConfigured) {
                signingConfig = signingConfigs.getByName("development")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// Keep JVM unit-test hangs observable and bounded. A healthy Launcher unit suite completes in
// seconds; this generous task timeout fails closed instead of consuming the entire CI job budget.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    timeout.set(Duration.ofMinutes(10))
    testLogging {
        events("started", "passed", "skipped", "failed")
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.animation:animation:1.8.2")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("androidx.room3:room3-runtime:3.0.3")
    implementation("androidx.sqlite:sqlite-framework:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    ksp("androidx.room3:room3-compiler:3.0.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.8.2")
}
