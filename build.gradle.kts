// Top-level build.gradle.kts (Project level)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    kotlin("jvm") version "1.9.23" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

