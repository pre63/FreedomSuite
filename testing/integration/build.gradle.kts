plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.freedomsuite.testing.integration"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs(
                    "-Djava.net.preferIPv4Stack=true",
                    "-DsocksProxyHost=",
                    "-DsocksProxyPort=",
                )
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") { dimension = "environment" }
        create("prod") { dimension = "environment" }
    }
}

dependencies {
    testImplementation(project(":testing:mock-server"))
    testImplementation(project(":protocol:imap"))
    testImplementation(project(":protocol:smtp"))
    testImplementation(project(":protocol:caldav"))
    testImplementation(project(":core:account"))
    testImplementation(project(":core:totp"))
    testImplementation(project(":core:crypto"))
    testImplementation(project(":sync:freedom-sync-android"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp)
}
