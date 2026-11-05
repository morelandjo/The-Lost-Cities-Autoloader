@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
}

stonecutter active file(".sc_active_version")

stonecutter parameters {
    // Defines `forge` / `neoforge` constants for `//? loader {` guards,
    // derived from the node id suffix (e.g. "1.20.1-forge" -> forge).
    constants.match(current.project.substringAfterLast('-'), "forge", "neoforge")
}

tasks.register("buildAll") {
    group = "build"
    description = "Build every supported target"
    dependsOn(
        ":1.21.1-neoforge:build",
        ":1.20.1-forge:build",
        ":1.18.2-forge:build",
    )
}
