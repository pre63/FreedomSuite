plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.7.3")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
}

gradlePlugin {
    plugins {
        register("freedomApplication") {
            id = "freedom.application"
            implementationClass = "FreedomApplicationConventionPlugin"
        }
        register("freedomLibrary") {
            id = "freedom.library"
            implementationClass = "FreedomLibraryConventionPlugin"
        }
    }
}
