plugins {
    id("freedom.application")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.freedomsuite.messages"

    defaultConfig {
        applicationId = "org.freedomsuite.messages"
        versionCode = 1
        versionName = "1.0.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:search-api"))
    implementation(project(":sync:freedom-sync-android"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
