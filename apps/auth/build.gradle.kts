plugins {
    id("freedom.application")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.freedomsuite.auth"

    defaultConfig {
        applicationId = "org.freedomsuite.auth"
        versionCode = 1
        versionName = "1.0.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:totp"))
    implementation(project(":sync:freedom-sync-android"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.zxing.android.embedded)
}
