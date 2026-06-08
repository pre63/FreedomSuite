plugins {
    id("freedom.application")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.freedomsuite.files"

    defaultConfig {
        applicationId = "org.freedomsuite.files"
        versionCode = 1
        versionName = "1.0.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        noCompress += "onnx"
    }
}

dependencies {
    implementation(project(":core:ml"))
    implementation(project(":core:search-api"))
    implementation(project(":sync:freedom-sync-android"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
