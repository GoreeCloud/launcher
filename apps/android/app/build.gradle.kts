import org.gradle.api.JavaVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.goreecloud.launcher"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.goreecloud.launcher"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-alpha.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":gc-ui-compose"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.13.0")
    implementation("androidx.compose.material3:material3:1.5.0")
    testImplementation("junit:junit:4.13.2")
}
