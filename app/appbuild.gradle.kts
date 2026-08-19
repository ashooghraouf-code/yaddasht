plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ir.yaddasht.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.yaddasht.app"
        minSdk = 26
        target
