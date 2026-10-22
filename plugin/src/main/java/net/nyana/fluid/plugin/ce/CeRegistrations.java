package net.nyana.fluid.plugin.ce;

import net.momirealms.craftengine.core.item.setting.ItemSettingsModifiers;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.item.ContainerSpec;
import net.nyana.fluid.plugin.render.CellConfigParser;
import net.nyana.fluid.plugin.render.GaugeConfigParser;
import net.nyana.fluid.plugin.render.ResourceGenerator;
import org.bukkit.plugin.Plugin;

public final class CeRegistrations {
    public static ResourceGenerator register(Plugin plugin) {
        CraftEngine engine = CraftEngine.instance();
        if (engine == null) throw new IllegalStateException("CraftEngine must load before NyanaFluid");

        ItemSettingsModifiers.register(Key.of("nyana_fluid", "container"), value -> {
            ContainerSpec spec = ContainerSpec.parse(value.getAsSection());
            return settings -> settings.addCustomData(ContainerSpec.SETTING, spec);
        });

        LootFunctions.register(Key.of("nyana-fluid", "preserve_fluid"), section -> new PreserveFluidLootFunction());
        FluidTagConfigParser tags = new FluidTagConfigParser();
        if (!engine.packManager().registerConfigSectionParser(tags))
            throw new IllegalStateException("CE rejected the fluid tag parser");
        FluidConfigParser fluids = new FluidConfigParser(tags, plugin);
        if (!engine.packManager().registerConfigSectionParser(fluids)) {
            throw new IllegalStateException("CE rejected the fluid parser");
        }
        GaugeConfigParser gauges = new GaugeConfigParser();
        CellConfigParser cells = new CellConfigParser();
        if (!engine.packManager().registerConfigSectionParser(gauges))
            throw new IllegalStateException("CE rejected the gauge parser");
        if (!engine.packManager().registerConfigSectionParser(cells))
            throw new IllegalStateException("CE rejected the fluid cell parser");
        return new ResourceGenerator(plugin, gauges, fluids::gauges, cells);
    }

    private CeRegistrations() {
    }
}
