plugins {
    id("java")
    id("net.neoforged.moddev.legacyforge")
}

val modId = property("mod_id") as String

// Shared base version (root gradle.properties) + Minecraft suffix, e.g. 1.0.3-1.20.1
val fullVersion = "${property("mod_version")}-${property("deps.minecraft")}"
version = fullVersion
group = property("mod_group_id") as String
base.archivesName = modId

java.toolchain.languageVersion = JavaLanguageVersion.of((property("java_version") as String).toInt())

repositories {
    // Lost Cities comes from CurseMaven (the McJty maven is not needed here and
    // is slow/unreachable at times, which breaks dynamic-version lookups).
    // CurseMaven for mods
    maven("https://cursemaven.com") {
        name = "CurseMaven"
        content { includeGroup("curse.maven") }
    }
}

// Compile Stonecutter's generated output (guards applied), not the raw root sources.
val scOut = layout.buildDirectory.dir("generated/stonecutter/main")
sourceSets.main {
    java.setSrcDirs(listOf(files(scOut.map { it.dir("java") }).builtBy("stonecutterGenerate")))
    resources.setSrcDirs(listOf(files(scOut.map { it.dir("resources") }).builtBy("stonecutterGenerate")))
}

legacyForge {
    version = "${property("deps.minecraft")}-${property("deps.forge")}"

    runs {
        register("client") {
            client()
            gameDirectory = file("run/")
        }
        register("server") {
            server()
            gameDirectory = file("run/")
            programArgument("--nogui")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    // Lost Cities: available during development (remapped), not a compile dependency.
    modRuntimeOnly("curse.maven:the-lost-cities-269024:${property("deps.lostcities")}")
}

val metadataProps = mapOf(
    "mod_id" to modId,
    "mod_name" to property("mod_name") as String,
    "mod_license" to property("mod_license") as String,
    "mod_version" to fullVersion,
    "mod_authors" to property("mod_authors") as String,
    "mod_description" to property("mod_description") as String,
    "minecraft_version_range" to property("minecraft_version_range") as String,
    "forge_version_range" to property("forge_version_range") as String,
    "loader_version_range" to property("loader_version_range") as String,
    "pack_format" to property("pack_format") as String,
)

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.properties(metadataProps)
    expand(metadataProps)
    // Stonecutter-processed templates; only Forge metadata (no neoforge.mods.toml).
    from(scOut.map { it.dir("templates") }) { include("META-INF/mods.toml", "pack.mcmeta") }
    dependsOn("stonecutterGenerate")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main { resources.srcDir(generateModMetadata) }
legacyForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}
