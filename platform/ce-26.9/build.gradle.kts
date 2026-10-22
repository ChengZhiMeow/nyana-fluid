// TickerBridge for CraftEngine 26.9.x, which has no native sleeping ticker: a software gate.
dependencies {
    compileOnly(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("net.momirealms:craft-engine-core:${property("craftEngineVersion")}")
    compileOnly("net.momirealms:craft-engine-bukkit:${property("craftEngineVersion")}")
}
