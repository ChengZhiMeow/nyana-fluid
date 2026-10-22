val bundled: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation(project(":api"))
    compileOnly(project(":platform:ce-26.9"))
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("net.momirealms:craft-engine-core:${property("craftEngineVersion")}")
    compileOnly("net.momirealms:craft-engine-bukkit:${property("craftEngineVersion")}")
    compileOnly("net.momirealms:craft-engine-bukkit-proxy:${property("craftEngineVersion")}")

    bundled(project(":api"))
    bundled(project(":platform:ce-26.9"))
    bundled(project(":platform:ce-26.10"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.jar {
    archiveBaseName.set("NyanaFluid")
    dependsOn(bundled)
    from({ bundled.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
