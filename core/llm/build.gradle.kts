plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.freedomsuite.core.llm"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") { dimension = "environment" }
        create("prod") { dimension = "environment" }
    }
    androidResources {
        noCompress += listOf("onnx", "data")
    }
    packaging {
        jniLibs {
            pickFirsts += listOf(
                "lib/arm64-v8a/libonnxruntime.so",
                "lib/x86_64/libonnxruntime.so",
            )
        }
    }
}

val genAiAar = rootProject.file("libs/onnxruntime-genai-android.aar")

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":core:network"))
    implementation(libs.okhttp)

    if (genAiAar.exists()) {
        implementation(files(genAiAar))
    } else {
        logger.warn(
            "onnxruntime-genai AAR missing — run ./scripts/fetch-genai-aar.sh. " +
                "On-device LLM will report model runtime unavailable.",
        )
    }

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.json:json:20240303")
}
