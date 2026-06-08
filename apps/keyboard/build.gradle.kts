plugins {
    id("freedom.application")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.freedomsuite.keyboard"

    defaultConfig {
        applicationId = "org.freedomsuite.keyboard"
        versionCode = 1
        versionName = "0.1.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:keyboard"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
