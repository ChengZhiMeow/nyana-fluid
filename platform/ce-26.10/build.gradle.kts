// TickerBridge for CraftEngine 26.10+, compiled against the real 26.10 API (SleepingBlockEntityTicker,
// BlockEntityController.subscribeChunkLoad). Its classes are only loaded after the plugin has probed that API.
dependencies {
    compileOnly(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("net.momirealms:craft-engine-core:${property("craftEngineNativeTickerVersion")}")
    compileOnly("net.momirealms:craft-engine-bukkit:${property("craftEngineNativeTickerVersion")}")
}
