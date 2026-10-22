package net.nyana.fluid.plugin.platform;

import net.nyana.fluid.api.ApiVersion;
import net.nyana.fluid.api.internal.BridgeSelection;
import net.nyana.fluid.api.platform.*;
import net.nyana.fluid.platform.ce269.SoftTickerBridge;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class PluginBridges {
    static final List<String> ORDER = List.of("region", "scheduler", "ticker", "itemData", "blockState", "vanilla", "display");
    private static final String NATIVE_TICKER_BRIDGE = "net.nyana.fluid.platform.ce2610.NativeTickerBridge";
    private static final ApiVersion NATIVE_SLEEP_SINCE = ApiVersion.of(26, 10, 0);

    public static PluginBridge select(Plugin plugin, PlatformInfo info) {
        Map<String, String> chosen = new HashMap<>();
        RegionBridge region = PluginBridges.pick(plugin, info, chosen, "region", List.of(
                BridgeProvider.of("paper-region", 0, any -> true, any -> new PaperRegionBridge())));
        SchedulerBridge scheduler = PluginBridges.pick(plugin, info, chosen, "scheduler", List.of(
                BridgeProvider.of("craftengine-executor", 0, any -> true, any -> new CraftEngineScheduler())));
        TickerBridge ticker = PluginBridges.pick(plugin, info, chosen, "ticker", List.<BridgeProvider<? extends TickerBridge>>of(

                BridgeProvider.of("ce-26.10-native-sleep", 10,
                        server -> server.nativeSleepingTicker() && server.craftEngineVersion().atLeast(PluginBridges.NATIVE_SLEEP_SINCE),
                        server -> PluginBridges.nativeTicker(plugin)),
                BridgeProvider.of("ce-26.9-soft-gate", 0, any -> true, any -> new SoftTickerBridge(plugin, scheduler))));
        ItemDataBridge itemData = PluginBridges.pick(plugin, info, chosen, "itemData", List.of(
                BridgeProvider.of("direct", 0, any -> true, any -> ItemDataBridge.direct())));
        BlockStateBridge blockState = PluginBridges.pick(plugin, info, chosen, "blockState", List.of(
                BridgeProvider.of("reflective", 0, any -> true, any -> new ReflectiveBlockStateBridge())));
        VanillaBridge vanilla = PluginBridges.pick(plugin, info, chosen, "vanilla", List.of(
                BridgeProvider.of("default", 0, any -> true, any -> new DefaultVanillaBridge())));
        DisplayBridge display = PluginBridges.pick(plugin, info, chosen, "display", List.of(
                BridgeProvider.of("craftengine-packets", 0, any -> true, any -> new CraftEnginePacketDisplays())));
        return new PluginBridge(region, scheduler, ticker, itemData, blockState, vanilla, display, chosen);
    }

    private static <B> B pick(Plugin plugin, PlatformInfo info, Map<String, String> chosen, String bridge,
                              List<? extends BridgeProvider<? extends B>> providers) {
        BridgeSelection.Selected<B> selected = BridgeSelection.select(bridge, providers, info, (provider, failure) ->
                plugin.getLogger().log(Level.WARNING, "The " + bridge + " bridge " + provider + " failed to start; trying the next one", failure));
        chosen.put(bridge, selected.provider());
        return selected.bridge();
    }

    private static TickerBridge nativeTicker(Plugin plugin) {
        try {
            Class<?> type = Class.forName(PluginBridges.NATIVE_TICKER_BRIDGE, true, PluginBridges.class.getClassLoader());
            return (TickerBridge) type.getConstructor(Plugin.class).newInstance(plugin);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot load " + PluginBridges.NATIVE_TICKER_BRIDGE, failure);
        }
    }

    private PluginBridges() {
    }
}
