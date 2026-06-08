plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.freedomsuite.core.privacy"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") { dimension = "environment" }
        create("prod") { dimension = "environment" }
    }
}

