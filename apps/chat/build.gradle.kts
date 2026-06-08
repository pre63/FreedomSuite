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
