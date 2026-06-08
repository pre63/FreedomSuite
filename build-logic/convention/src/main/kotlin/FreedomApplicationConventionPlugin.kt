import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class FreedomApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val appName = path.substringAfterLast(":").replaceFirstChar { it.uppercase() }
                .let { name ->
                    when (name) {
                        "Inbox" -> "Freedom Inbox"
                        "Calendar" -> "Freedom Calendar"
                        "Messages" -> "Freedom Messages"
                        "Chat" -> "Freedom Chat"
                        "Auth" -> "Freedom Auth"
                        "Files" -> "Freedom Files"
                        "Keyboard" -> "Freedom Keyboard"
                        "Search" -> "Freedom Search"
                        else -> "Freedom $name"
                    }
                }

            extensions.configure<ApplicationExtension> {
                compileSdk = 35

                defaultConfig {
                    minSdk = 26
                    targetSdk = 35
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    ndk {
                        // F-Droid / real devices only — drop x86 emulator ABIs to shrink native ML libs.
                        abiFilters += listOf("arm64-v8a", "armeabi-v7a")
                    }
                }

                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                }

                buildFeatures {
                    compose = true
                    buildConfig = true
                }

                flavorDimensions += "environment"
                productFlavors {
                    create("dev") {
                        dimension = "environment"
                        applicationIdSuffix = ".dev"
                        versionNameSuffix = "-dev"
                        buildConfigField("String", "APP_NAME", "\"$appName (Dev)\"")
                        buildConfigField("boolean", "PRIVACY_STRICT", "false")
                    }
                    create("prod") {
                        dimension = "environment"
                        buildConfigField("String", "APP_NAME", "\"$appName\"")
                        buildConfigField("boolean", "PRIVACY_STRICT", "true")
                    }
                }

                buildTypes {
                    debug { isDebuggable = true }
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        isDebuggable = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            rootProject.file("config/proguard-freedom.pro"),
                        )
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                sourceSets {
                    getByName("main") {
                        res.srcDirs(rootProject.file("config/res"))
                    }
                }
            }

            dependencies {
                add("implementation", project(":core:logging"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:crypto"))
                add("implementation", project(":core:storage"))
                add("implementation", project(":core:network"))
                add("implementation", project(":core:account"))
                add("implementation", project(":core:privacy"))
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("implementation", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icons").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
                add("testImplementation", libs.findLibrary("junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-espresso-core").get())
                add("devImplementation", libs.findLibrary("timber").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-fragment-ktx").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", project(":core:applock"))
            }
        }
    }
}
