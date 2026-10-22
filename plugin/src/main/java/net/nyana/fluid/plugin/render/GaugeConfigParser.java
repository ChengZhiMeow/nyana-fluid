package net.nyana.fluid.plugin.render;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GaugeConfigParser extends IdSectionConfigParser {
    private static final LoadingStage STAGE = new LoadingStage("nyana-fluid:gauges");
    private final Map<Key, GaugeSpec> defaults = new LinkedHashMap<>();
    private final Map<Key, Map<Key, GaugeSpec>> fluids = new LinkedHashMap<>();

    @Override
    public Key type() {
        return Key.of("nyana-fluid", "gauges");
    }

    @Override
    public String[] sectionId() {
        return new String[]{"nyana-fluid:gauges"};
    }

    @Override
    public LoadingStage loadingStage() {
        return GaugeConfigParser.STAGE;
    }

    @Override
    public List<LoadingStage> dependencies() {
        return List.of(LoadingStages.TEMPLATE, LoadingStages.CONFIG_FACTORY);
    }

    @Override
    public synchronized void preProcess() {
        this.defaults.clear();
        this.fluids.clear();
    }

    @Override
    public synchronized int count() {
        return this.defaults.size();
    }

    @Override
    protected synchronized void parseSection(Pack pack, Path path, Key id, ConfigSection section) {
        this.defaults.put(id, GaugeSpec.parse(section.values()));
        if (section.get("fluids") instanceof Map<?, ?> map) {
            Map<Key, GaugeSpec> fluids = new LinkedHashMap<>();
            map.forEach((fluid, value) -> {
                if (value instanceof Map<?, ?> override)
                    fluids.put(Key.of(String.valueOf(fluid)), GaugeSpec.parse(override));
            });
            this.fluids.put(id, fluids);
        }
    }

    public synchronized Map<Key, GaugeSpec> defaults() {
        return Map.copyOf(this.defaults);
    }

    public synchronized Map<Key, Map<Key, GaugeSpec>> fluids() {
        return Map.copyOf(this.fluids);
    }
}
