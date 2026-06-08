plugins {
    id("freedom.application")
}

android {
    namespace = "org.freedomsuite.search"

    defaultConfig {
        applicationId = "org.freedomsuite.search"
        versionCode = 1
        versionName = "0.1.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:search-api"))
}
