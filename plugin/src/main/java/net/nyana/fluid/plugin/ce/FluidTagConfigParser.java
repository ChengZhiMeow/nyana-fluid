package net.nyana.fluid.plugin.ce;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.IdValueConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.*;

final class FluidTagConfigParser extends IdValueConfigParser {
    static final LoadingStage STAGE = new LoadingStage("nyana-fluid:fluid_tags");
    private final Map<Key, List<String>> pending = new LinkedHashMap<>();

    @Override
    public Key type() {
        return Key.of("nyana-fluid", "fluid_tags");
    }

    @Override
    public String[] sectionId() {
        return new String[]{"nyana-fluid:fluid_tags"};
    }

    @Override
    public LoadingStage loadingStage() {
        return FluidTagConfigParser.STAGE;
    }

    @Override
    public List<LoadingStage> dependencies() {
        return List.of(LoadingStages.TEMPLATE, LoadingStages.CONFIG_FACTORY);
    }

    @Override
    public synchronized void preProcess() {
        this.pending.clear();
    }

    @Override
    public synchronized int count() {
        return this.pending.size();
    }

    @Override
    protected synchronized void parseValue(Pack pack, Path filePath, Key id, ConfigValue value) {
        Object raw = value.value();
        List<String> members = raw instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of(String.valueOf(raw));
        this.pending.put(id, members);
    }

    synchronized Map<Key, Set<Key>> tags() {
        Map<Key, Set<Key>> result = new LinkedHashMap<>();
        for (Map.Entry<Key, List<String>> entry : this.pending.entrySet()) {
            Set<Key> members = new LinkedHashSet<>();
            for (String member : entry.getValue()) {
                if (member.startsWith("#")) {
                    List<String> nested = this.pending.get(Key.of(member.substring(1)));
                    if (nested != null)
                        for (String inner : nested) if (!inner.startsWith("#")) members.add(Key.of(inner));
                } else {
                    members.add(Key.of(member));
                }
            }
            result.put(entry.getKey(), members);
        }
        return result;
    }
}
