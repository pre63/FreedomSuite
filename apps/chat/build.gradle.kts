plugins {
    id("freedom.application")
}

android {
    namespace = "org.freedomsuite.chat"

    defaultConfig {
        applicationId = "org.freedomsuite.chat"
        versionCode = 1
        versionName = "0.1.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:llm"))
    implementation(project(":core:ml"))
    implementation(project(":core:keyboard"))
    implementation(project(":core:crypto"))
    implementation(libs.okhttp)
}
