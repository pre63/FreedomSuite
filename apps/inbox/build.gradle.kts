plugins {
    id("freedom.application")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.freedomsuite.inbox"

    defaultConfig {
        applicationId = "org.freedomsuite.inbox"
        versionCode = 1
        versionName = "1.0.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":protocol:imap"))
    implementation(project(":protocol:smtp"))
    implementation(project(":protocol:ical"))
    implementation(project(":protocol:mime"))
    implementation(project(":core:calendar-api"))
    implementation(project(":core:search-api"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
