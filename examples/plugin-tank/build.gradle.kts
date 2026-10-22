dependencies {
    compileOnly(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("net.momirealms:craft-engine-core:${property("craftEngineVersion")}")
    compileOnly("net.momirealms:craft-engine-bukkit:${property("craftEngineVersion")}")
}

tasks.jar {
    archiveBaseName.set("NyanaFluidTankExample")
}
