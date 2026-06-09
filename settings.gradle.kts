pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FreedomSuite"

// Shared core
include(":core:applock")
include(":core:logging")
include(":core:ui")
include(":core:crypto")
include(":core:network")
include(":core:storage")
include(":core:account")
include(":core:privacy")
include(":core:totp")
include(":core:ml")
include(":core:llm")
include(":core:keyboard")
include(":core:search-api")

// Protocol adapters
include(":protocol:imap")
include(":protocol:smtp")
include(":protocol:caldav")
include(":protocol:ical")
include(":protocol:mime")

include(":core:calendar-api")

// Cross-device sync
include(":sync:freedom-sync-android")

// Integration testing
include(":testing:mock-server")
include(":testing:integration")

// Applications
include(":apps:inbox")
include(":apps:calendar")
include(":apps:messages")
include(":apps:chat")
include(":apps:auth")
include(":apps:files")
include(":apps:keyboard")
include(":apps:search")
