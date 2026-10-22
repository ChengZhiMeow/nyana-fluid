package net.nyana.fluid.plugin.ce;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.fluid.FluidAttributes;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.api.item.BucketSpec;
import net.nyana.fluid.plugin.render.GaugeSpec;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

final class FluidConfigParser extends IdSectionConfigParser {
    static final LoadingStage STAGE = new LoadingStage("nyana-fluid:fluids");
    private final FluidTagConfigParser tags;
    private final Plugin plugin;
    private final Logger logger;
    private final List<BucketSpec> pendingContainers = new ArrayList<>();
    private final List<Registration> containers = new ArrayList<>();
    private final Map<Key, FluidAttributes> fluids = new LinkedHashMap<>();
    private final Map<Key, Set<Key>> declaredTags = new LinkedHashMap<>();
    private final Map<Key, Map<Key, GaugeSpec>> gauges = new LinkedHashMap<>();

    FluidConfigParser(FluidTagConfigParser tags, Plugin plugin) {
        this.tags = tags;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public Key type() {
        return Key.of("nyana-fluid", "fluids");
    }

    @Override
    public String[] sectionId() {
        return new String[]{"nyana-fluid:fluids"};
    }

    @Override
    public LoadingStage loadingStage() {
        return FluidConfigParser.STAGE;
    }

    @Override
    public List<LoadingStage> dependencies() {
        return List.of(LoadingStages.TEMPLATE, LoadingStages.CONFIG_FACTORY, FluidTagConfigParser.STAGE);
    }

    @Override
    public synchronized void preProcess() {
        this.fluids.clear();
        this.declaredTags.clear();
        this.pendingContainers.clear();
        this.gauges.clear();
    }

    @Override
    public synchronized int count() {
        return this.fluids.size();
    }

    @Override
    protected synchronized void parseSection(Pack pack, Path path, Key id, ConfigSection section) {
        this.fluids.put(id, FluidConfigs.attributes(section));
        this.pendingContainers.addAll(FluidConfigs.containers(id, section));
        for (Key tag : FluidConfigs.keys(section.get("tags"))) {
            Key cleaned = tag.toString().startsWith("#") ? Key.of(tag.toString().substring(1)) : tag;
            this.declaredTags.computeIfAbsent(cleaned, ignored -> new HashSet<>()).add(id);
        }
        if (section.get("gauges") instanceof Map<?, ?> map) {
            map.forEach((gauge, value) -> {
                if (value instanceof Map<?, ?> spec) {
                    this.gauges.computeIfAbsent(Key.of(String.valueOf(gauge)), ignored -> new LinkedHashMap<>()).put(id, GaugeSpec.parse(spec));
                }
            });
        }
    }

    synchronized Map<Key, Map<Key, GaugeSpec>> gauges() {
        Map<Key, Map<Key, GaugeSpec>> copy = new LinkedHashMap<>();
        this.gauges.forEach((gauge, fluids) -> copy.put(gauge, Map.copyOf(fluids)));
        return copy;
    }

    @Override
    public synchronized void postProcess() {
        Map<Key, Set<Key>> allTags = new LinkedHashMap<>(this.tags.tags());
        this.declaredTags.forEach((tag, members) -> allTags.computeIfAbsent(tag, ignored -> new HashSet<>()).addAll(members));
        Core.get().fluids.replaceConfig(this.fluids, allTags);
        Set<Key> overridden = Core.get().fluids.overriddenConfigKeys();
        if (!overridden.isEmpty())
            this.logger.warning("Code-registered fluids override configured ones: " + overridden);
        for (Registration registration : this.containers) registration.close();
        this.containers.clear();
        for (BucketSpec spec : this.pendingContainers)
            this.containers.add(Core.get().containers.registerBucket(this.plugin, spec));
    }
}
