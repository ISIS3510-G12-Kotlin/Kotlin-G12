// 📁 build.gradle.kts (nivel PROYECTO)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false

    // Plugin de Google Services para Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false
}
