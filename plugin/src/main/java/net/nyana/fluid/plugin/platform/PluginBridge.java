package net.nyana.fluid.plugin.platform;

import net.nyana.fluid.api.platform.*;

import java.util.Map;
import java.util.stream.Collectors;

public record PluginBridge(RegionBridge region, SchedulerBridge scheduler, TickerBridge ticker, ItemDataBridge itemData,
                           BlockStateBridge blockState, VanillaBridge vanilla, DisplayBridge display,
                           Map<String, String> providers) implements PlatformBridge {
    public PluginBridge {
        providers = Map.copyOf(providers);
    }

    public String describe() {
        return PluginBridges.ORDER.stream()
                .map(bridge -> bridge + "=" + this.providers.getOrDefault(bridge, "?"))
                .collect(Collectors.joining(", "));
    }
}
