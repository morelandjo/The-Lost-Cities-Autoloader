plugins {
    id("java-library")
    id("maven-publish")
    id("net.neoforged.moddev")
    id("idea")
}

val modId = property("mod_id") as String

// Shared base version (root gradle.properties) + Minecraft suffix, e.g. 1.0.3-1.21.1
val fullVersion = "${property("mod_version")}-${property("deps.minecraft")}"
version = fullVersion
group = property("mod_group_id") as String
base.archivesName = modId

java.toolchain.languageVersion = JavaLanguageVersion.of((property("java_version") as String).toInt())

repositories {
    // CurseMaven for mods
    maven("https://cursemaven.com") {
        name = "CurseMaven"
        content { includeGroup("curse.maven") }
    }
}

neoForge {
    version = property("deps.neoforge") as String

    if (hasProperty("deps.parchment")) {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        parchment {
            minecraftVersion = mc
            mappingsVersion = ver
        }
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        // Launches GameTestServer and runs all registered gametests, then exits.
        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", modId, "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

// Compile Stonecutter's generated output (guards applied), not the raw root sources.
val scOut = layout.buildDirectory.dir("generated/stonecutter/main")
sourceSets.main {
    java.setSrcDirs(listOf(files(scOut.map { it.dir("java") }).builtBy("stonecutterGenerate")))
    resources.setSrcDirs(listOf(files(scOut.map { it.dir("resources") }).builtBy("stonecutterGenerate")))
}

// Present for runtime testing but not published as a dependency.
val localRuntime = configurations.register("localRuntime")
configurations.runtimeClasspath { extendsFrom(localRuntime.get()) }

dependencies {
    // Lost Cities: available during development but not required for distribution.
    localRuntime("curse.maven:the-lost-cities-269024:${property("deps.lostcities")}")
}

val metadataProps = mapOf(
    "minecraft_version" to property("deps.minecraft") as String,
    "minecraft_version_range" to property("minecraft_version_range") as String,
    "neo_version" to property("deps.neoforge") as String,
    "neo_version_range" to property("neo_version_range") as String,
    "loader_version_range" to property("loader_version_range") as String,
    "mod_id" to modId,
    "mod_name" to property("mod_name") as String,
    "mod_license" to property("mod_license") as String,
    "mod_version" to fullVersion,
    "mod_authors" to property("mod_authors") as String,
    "mod_description" to property("mod_description") as String,
)
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.properties(metadataProps)
    expand(metadataProps)
    // Stonecutter-processed templates; only this loader's metadata file.
    from(scOut.map { it.dir("templates") }) { include("META-INF/neoforge.mods.toml") }
    dependsOn("stonecutterGenerate")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main { resources.srcDir(generateModMetadata) }
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven("file://${project.projectDir}/repo")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
