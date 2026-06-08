plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

/** Apps intended for F-Droid (excludes placeholder chat). */
val fdroidAppModules = listOf(
    ":apps:inbox",
    ":apps:calendar",
    ":apps:messages",
    ":apps:auth",
    ":apps:files",
    ":apps:keyboard",
    ":apps:search",
)

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests against local mock mailbox.org servers"
    dependsOn(
        ":testing:mock-server:test",
        ":testing:integration:testDevDebugUnitTest",
    )
}

tasks.register("storageAudit") {
    group = "verification"
    description = "Audits codebase for unencrypted local storage"
    dependsOn(":testing:mock-server:test")
}

tasks.register("privacyAudit") {
    group = "verification"
    description = "Audits third-party deps, analytics SDKs, and privacy enforcement"
    dependsOn(
        ":testing:mock-server:test",
        fdroidAppModules.map { "$it:dependencies" },
    )
}

tasks.register("assembleFdroidRelease") {
    group = "build"
    description = "Assembles prodRelease APKs for all F-Droid apps"
    dependsOn(fdroidAppModules.map { "$it:assembleProdRelease" })
}

tasks.register("fdroidVerify") {
    group = "verification"
    description = "Full F-Droid CI gate: audits, integration tests, prod release builds"
    dependsOn(
        "integrationTest",
        "privacyAudit",
        "assembleFdroidRelease",
    )
}
