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

/** All installable application modules (dev daily driver). */
val devAppModules = fdroidAppModules + listOf(":apps:chat")

tasks.register<Exec>("mlIntegrationTest") {
    group = "verification"
    description = "Runs ONNX vision/OCR regression tests (Python + bundled models)"
    workingDir(rootDir)
    commandLine("bash", "-c", "./scripts/setup-ml-tools.sh && .venv-ml-quantize/bin/python scripts/ml-integration-test.py")
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests against local mock mailbox.org servers and on-device ML"
    dependsOn(
        ":testing:mock-server:test",
        ":testing:integration:testDevDebugUnitTest",
        ":core:ml:testDevDebugUnitTest",
        "mlIntegrationTest",
    )
}

tasks.register("storageAudit") {
    group = "verification"
    description = "Audits codebase for unencrypted local storage"
    dependsOn(":testing:mock-server:test")
}

tasks.register("privacyAudit") {
    group = "verification"
    description = "Audits third-party deps, manifests, network egress, and privacy enforcement"
    dependsOn(
        ":testing:mock-server:test",
        fdroidAppModules.map { "$it:assembleProdRelease" },
    )
}

tasks.register("assembleFdroidRelease") {
    group = "build"
    description = "Assembles prodRelease APKs for all F-Droid apps"
    dependsOn(fdroidAppModules.map { "$it:assembleProdRelease" })
}

tasks.register("assembleDevDebug") {
    group = "build"
    description = "Assembles devDebug APKs for all apps (use for emulator/device install)"
    dependsOn(devAppModules.map { "$it:assembleDevDebug" })
}

tasks.register("installDevDebugApps") {
    group = "install"
    description = "Installs devDebug APKs sequentially on the connected device/emulator"
    val installTasks = devAppModules.map { path ->
        project(path).tasks.named("installDevDebug")
    }
    installTasks.forEach { dependsOn(it) }
    for (i in 1 until installTasks.size) {
        installTasks[i].configure { mustRunAfter(installTasks[i - 1]) }
    }
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
