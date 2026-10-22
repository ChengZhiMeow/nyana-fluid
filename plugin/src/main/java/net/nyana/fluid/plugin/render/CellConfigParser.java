package net.nyana.fluid.plugin.render;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.render.FluidModels;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CellConfigParser extends IdSectionConfigParser {
    private static final LoadingStage STAGE = new LoadingStage("nyana-fluid:fluid_cells");
    private final Map<Key, Style> styles = new LinkedHashMap<>();

    @Override
    public Key type() {
        return Key.of("nyana-fluid", "fluid_cells");
    }

    @Override
    public String[] sectionId() {
        return new String[]{"nyana-fluid:fluid_cells"};
    }

    @Override
    public LoadingStage loadingStage() {
        return CellConfigParser.STAGE;
    }

    @Override
    public List<LoadingStage> dependencies() {
        return List.of(LoadingStages.TEMPLATE, LoadingStages.CONFIG_FACTORY);
    }

    @Override
    public synchronized void preProcess() {
        this.styles.clear();
    }

    @Override
    public synchronized int count() {
        return this.styles.size();
    }

    @Override
    protected synchronized void parseSection(Pack pack, Path path, Key id, ConfigSection section) {
        String overlay = section.getString("overlay", (String) null);
        this.styles.put(id, new Style(Key.of(section.getString("texture", FluidModels.DEFAULT_TEXTURE.toString())),
                overlay == null ? null : Key.of(overlay)));
    }

    public synchronized Map<Key, Style> styles() {
        Map<Key, Style> all = new LinkedHashMap<>();
        all.put(FluidModels.FLUID_CELL, new Style(FluidModels.DEFAULT_TEXTURE, null));
        all.putAll(this.styles);
        return all;
    }

    public record Style(Key texture, @Nullable Key overlay) {
    }
}
