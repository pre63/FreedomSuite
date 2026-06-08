plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.freedomsuite.core.logging"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("boolean", "PRIVACY_STRICT", "false")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("boolean", "PRIVACY_STRICT", "true")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    add("devImplementation", libs.timber)
}
