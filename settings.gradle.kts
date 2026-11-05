pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        // Each node is "<mc>-<loader>" and uses build.<loader>.gradle.kts.
        fun entry(mc: String, loader: String, script: String = loader) {
            version("$mc-$loader", mc).buildscript = "build.$script.gradle.kts"
        }
        entry("1.21.1", "neoforge")
        entry("1.20.1", "forge")
        entry("1.18.2", "forge")

        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "The-Lost-Cities-Autoloader"
