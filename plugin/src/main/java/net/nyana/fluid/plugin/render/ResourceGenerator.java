package net.nyana.fluid.plugin.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackCacheEvent;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.fluid.Fluid;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.api.render.FluidCells;
import net.nyana.fluid.api.render.FluidModels;
import net.nyana.fluid.plugin.render.GaugeSpec.Direction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class ResourceGenerator implements Listener {
    private static final String NAMESPACE = "nyana-fluid";
    private static final Key TRANSPARENT = Key.of(ResourceGenerator.NAMESPACE, "gui/transparent");
    private static final int FULL = 16;
    private static final Map<String, Object> GUI_IDENTITY = Map.of("gui", Map.of(
            "rotation", List.of(0, 0, 0), "translation", List.of(0, 0, 0), "scale", List.of(1, 1, 1)));

    private static Map<String, Object> gaugeItem(GaugeSpec spec, Object model) {
        return spec.oversized() ? Map.of("oversized_in_gui", true, "model", model) : Map.of("model", model);
    }

    private static Map<String, Object> tintFromData() {
        return Map.of("type", "minecraft:custom_model_data", "index", 0, "default", -1);
    }

    static BufferedImage crop(BufferedImage source, int level, int levels, Direction direction) {
        int[] box = ResourceGenerator.opaqueBox(source);
        BufferedImage frame = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        if (box == null) return frame;
        int width = box[2] - box[0];
        int height = box[3] - box[1];
        int keptWidth = direction.horizontal() ? (level * width + levels - 1) / levels : width;
        int keptHeight = direction.horizontal() ? height : (level * height + levels - 1) / levels;
        int x = direction == Direction.LEFT ? box[2] - keptWidth : box[0];
        int y = direction == Direction.UP ? box[3] - keptHeight : box[1];
        for (int px = x; px < x + keptWidth; px++) {
            for (int py = y; py < y + keptHeight; py++) frame.setRGB(px, py, source.getRGB(px, py));
        }
        return frame;
    }

    static int @Nullable [] opaqueBox(BufferedImage image) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (image.getRGB(x, y) >>> 24 == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX + 1, maxY + 1};
    }

    private static Map<String, Object> face(List<Integer> uv) {
        return Map.of("uv", uv, "texture", "#fluid", "tintindex", 0);
    }

    private static Map<String, Object> constant() {
        return Map.of("type", "minecraft:constant", "value", -1);
    }

    private final Plugin plugin;
    private final Path root;
    private final GaugeConfigParser gauges;
    private final Supplier<Map<Key, Map<Key, GaugeSpec>>> fluidGauges;
    private final CellConfigParser cells;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final Map<Path, byte[]> files = new LinkedHashMap<>();

    public ResourceGenerator(Plugin plugin, GaugeConfigParser gauges, Supplier<Map<Key, Map<Key, GaugeSpec>>> fluidGauges, CellConfigParser cells) {
        this.plugin = plugin;
        this.root = plugin.getDataFolder().toPath().resolve("generated-pack");
        this.gauges = gauges;
        this.fluidGauges = fluidGauges;
        this.cells = cells;
    }

    @EventHandler
    public void onCache(AsyncResourcePackCacheEvent event) {
        try {
            this.generate();
            event.registerExternalResourcePack(this.root);
        } catch (IOException | RuntimeException failure) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to generate fluid models", failure);
        }
    }

    private synchronized void generate() throws IOException {
        this.files.clear();
        this.writePng(this.texture(ResourceGenerator.TRANSPARENT), new BufferedImage(ResourceGenerator.FULL, ResourceGenerator.FULL, BufferedImage.TYPE_INT_ARGB));
        this.tankFluids();
        for (Map.Entry<Key, CellConfigParser.Style> style : this.cells.styles().entrySet())
            this.cellStyle(style.getKey(), style.getValue());
        this.gauges();
        this.sync();
    }

    private void sync() throws IOException {
        for (Map.Entry<Path, byte[]> file : this.files.entrySet()) {
            Path path = file.getKey();
            if (Files.isRegularFile(path) && Arrays.equals(Files.readAllBytes(path), file.getValue())) continue;
            Files.createDirectories(path.getParent());
            Files.write(path, file.getValue());
        }
        try (Stream<Path> paths = Files.walk(this.root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (path.equals(this.root)) continue;
                if (Files.isDirectory(path)) {
                    try (Stream<Path> children = Files.list(path)) {
                        if (children.findAny().isPresent()) continue;
                    }
                } else if (this.files.containsKey(path)) {
                    continue;
                }
                Files.delete(path);
            }
        }
    }

    private void tankFluids() throws IOException {
        for (int height = 1; height <= ResourceGenerator.FULL; height++) {
            Map<String, Object> faces = new LinkedHashMap<>();
            List<Integer> top = List.of(0, 0, ResourceGenerator.FULL, ResourceGenerator.FULL);
            List<Integer> side = List.of(0, ResourceGenerator.FULL - height, ResourceGenerator.FULL, ResourceGenerator.FULL);
            faces.put("down", ResourceGenerator.face(top));
            faces.put("up", ResourceGenerator.face(top));
            for (String direction : List.of("north", "south", "west", "east"))
                faces.put(direction, ResourceGenerator.face(side));
            this.writeJson(this.model(Key.of(ResourceGenerator.NAMESPACE, "tank_fluid_shape/" + height)), Map.of(
                    "textures", Map.of("particle", "#fluid"),
                    "elements", List.of(Map.of("from", List.of(0, 0, 0), "to", List.of(ResourceGenerator.FULL, height, ResourceGenerator.FULL), "shade", false, "faces", faces))));
        }
        for (Fluid fluid : NyanaFluid.fluids().all()) {
            String texture = FluidModels.texture(fluid.attributes()).toString();
            int tint = FluidModels.tint(fluid.attributes());
            for (int height = 1; height <= ResourceGenerator.FULL; height++) {
                Key model = FluidModels.tankFluid(fluid.key(), height);
                this.writeJson(this.model(model), Map.of("parent", ResourceGenerator.NAMESPACE + ":tank_fluid_shape/" + height,
                        "textures", Map.of("fluid", texture, "particle", texture)));
                this.writeJson(this.item(model), Map.of("model", Map.of("type", "minecraft:model", "model", model.toString(),
                        "tints", List.of(Map.of("type", "minecraft:constant", "value", tint)))));
            }
        }
    }

    private void cellStyle(Key style, CellConfigParser.Style config) throws IOException {
        Key texture = config.texture();
        String base = style.namespace() + ":nyana_fluid_cell/" + style.value();
        this.writeJson(this.model(Key.of(base + "/bounds")), Map.of(
                "gui_light", "front",
                "textures", Map.of("bounds", ResourceGenerator.TRANSPARENT.toString(), "particle", ResourceGenerator.TRANSPARENT.toString()),
                "elements", List.of(Map.of("from", List.of(-1, -1, 7.9), "to", List.of(17, 17, 7.9),
                        "faces", Map.of("south", Map.of("uv", List.of(0, 0, ResourceGenerator.FULL, ResourceGenerator.FULL), "texture", "#bounds")))),
                "display", ResourceGenerator.GUI_IDENTITY));
        List<Fluid> textured = NyanaFluid.fluids().all().stream()
                .filter(fluid -> fluid.attributes().texture().isPresent() && !FluidModels.texture(fluid.attributes()).equals(texture)).toList();
        List<Object> models = new ArrayList<>();
        models.add(Map.of("type", "minecraft:model", "model", base + "/bounds"));
        for (int layer = 0; layer < FluidCells.MAX_LAYERS; layer++) {
            Object fallback = this.cellLayer(base + "/layer_" + layer, texture, layer);
            List<Object> cases = new ArrayList<>();
            for (Fluid fluid : textured) {
                Key key = fluid.key();
                String name = base + "/fluid/" + key.namespace() + "/" + key.value() + "/layer_" + layer;
                cases.add(Map.of("when", key.toString(), "model", this.cellLayer(name, FluidModels.texture(fluid.attributes()), layer)));
            }
            models.add(cases.isEmpty() ? fallback : Map.of("type", "minecraft:select", "property", "minecraft:custom_model_data",
                    "index", layer, "cases", cases, "fallback", fallback));
        }
        if (config.overlay() != null) {
            String overlay = config.overlay().toString();
            this.writeJson(this.model(Key.of(base + "/overlay")), Map.of(
                    "gui_light", "front",
                    "textures", Map.of("overlay", overlay, "particle", overlay),
                    "elements", List.of(Map.of("from", List.of(-1, -1, 8 + FluidCells.MAX_LAYERS * 0.1), "to", List.of(17, 17, 8 + FluidCells.MAX_LAYERS * 0.1), "shade", false,
                            "faces", Map.of("south", Map.of("uv", List.of(0, 0, ResourceGenerator.FULL, ResourceGenerator.FULL), "texture", "#overlay")))),
                    "display", ResourceGenerator.GUI_IDENTITY));
            models.add(Map.of("type", "minecraft:model", "model", base + "/overlay"));
        }
        this.writeJson(this.item(style), Map.of("oversized_in_gui", true, "model", Map.of("type", "minecraft:composite", "models", models)));
    }

    private Object cellLayer(String base, Key texture, int layer) throws IOException {
        double z = 8 + (FluidCells.MAX_LAYERS - 1 - layer) * 0.1;
        List<Object> entries = new ArrayList<>();
        for (int height = 1; height <= FluidCells.CELL_PIXELS; height++) {
            String name = base + "/" + height;
            double uvTop = ResourceGenerator.FULL - height * (double) ResourceGenerator.FULL / FluidCells.CELL_PIXELS;
            this.writeJson(this.model(Key.of(name)), Map.of(
                    "gui_light", "front",
                    "textures", Map.of("fluid", texture.toString(), "particle", texture.toString()),
                    "elements", List.of(Map.of("from", List.of(-1, -1, z), "to", List.of(17, height - 1, z), "shade", false,
                            "faces", Map.of("south", Map.of("uv", List.of(0, uvTop, ResourceGenerator.FULL, ResourceGenerator.FULL), "texture", "#fluid", "tintindex", 0)))),
                    "display", ResourceGenerator.GUI_IDENTITY));
            entries.add(Map.of("threshold", height - 0.5, "model", Map.of("type", "minecraft:model", "model", name,
                    "tints", List.of(Map.of("type", "minecraft:custom_model_data", "index", layer, "default", -1)))));
        }
        return Map.of("type", "minecraft:range_dispatch", "property", "minecraft:custom_model_data", "index", layer,
                "entries", entries, "fallback", Map.of("type", "minecraft:empty"));
    }

    private void gauges() {
        Map<Key, GaugeSpec> defaults = this.gauges.defaults();
        Map<Key, Map<Key, GaugeSpec>> fluids = new LinkedHashMap<>();
        this.gauges.fluids().forEach((gauge, specs) -> fluids.computeIfAbsent(gauge, ignored -> new LinkedHashMap<>()).putAll(specs));
        this.fluidGauges.get().forEach((gauge, specs) -> fluids.computeIfAbsent(gauge, ignored -> new LinkedHashMap<>()).putAll(specs));
        Set<Key> ids = new LinkedHashSet<>(defaults.keySet());
        ids.addAll(fluids.keySet());
        Map<Key, GaugeTable> tables = new HashMap<>();
        for (Key id : ids) {
            GaugeSpec base = defaults.get(id);
            try {
                List<Key> fallback = base == null ? null : this.gaugeLevels(id, base, "default");
                Map<Key, List<Key>> byFluid = new HashMap<>();
                for (Map.Entry<Key, GaugeSpec> entry : fluids.getOrDefault(id, Map.of()).entrySet()) {
                    Key fluid = entry.getKey();
                    byFluid.put(fluid, this.gaugeLevels(id, entry.getValue().over(base), "fluid/" + fluid.namespace() + "/" + fluid.value()));
                }
                tables.put(id, new GaugeTable(fallback, byFluid));
            } catch (IOException | RuntimeException failure) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to generate gauge " + id, failure);
            }
        }
        Core.get().gauges = (gauge, fluid, fraction) -> {
            GaugeTable table = tables.get(gauge);
            if (table == null) return null;
            List<Key> levels = fluid == null ? table.fallback() : table.fluids().getOrDefault(fluid, table.fallback());
            if (levels == null) return null;
            int count = levels.size() - 1;
            if (fraction <= 0 || count == 0) return levels.get(0);
            return levels.get(Math.max(1, Math.min(count, (int) Math.ceil(fraction * count))));
        };
    }

    private List<Key> gaugeLevels(Key id, GaugeSpec spec, String variant) throws IOException {
        String base = "gauge/" + id.namespace() + "/" + id.value() + "/" + variant;
        List<Key> levels = new ArrayList<>();
        levels.add(spec.background() != null || spec.overlay() != null ? this.textureLevel(Key.of(ResourceGenerator.NAMESPACE, base + "/empty"), spec, null) : null);
        if (spec.typeOrDefault() == GaugeSpec.Type.SPLIT) {
            if (spec.texture() == null) return levels;
            BufferedImage source = this.readTexture(spec.texture());
            int[] box = ResourceGenerator.opaqueBox(source);
            if (box == null)
                throw new IllegalArgumentException("Gauge texture " + spec.texture() + " is fully transparent");
            Direction direction = spec.directionOrDefault();
            int count = spec.amount() != null && spec.amount() > 0 ? spec.amount() : direction.horizontal() ? box[2] - box[0] : box[3] - box[1];
            for (int level = 1; level <= count; level++) {
                Key frame = Key.of(ResourceGenerator.NAMESPACE, "gauge_frames/" + id.namespace() + "/" + id.value() + "/" + variant + "/" + level);
                this.writePng(this.texture(frame), ResourceGenerator.crop(source, level, count, direction));
                levels.add(this.textureLevel(Key.of(ResourceGenerator.NAMESPACE, base + "/" + level), spec, frame));
            }
            return levels;
        }
        if (spec.items() == null || spec.items().isEmpty()) return levels;
        TreeMap<Integer, GaugeSpec.Entry> items = new TreeMap<>(spec.items());
        int count = spec.amount() != null && spec.amount() > 0 ? spec.amount() : items.lastKey();
        if (items.containsKey(0))
            levels.set(0, this.entryLevel(Key.of(ResourceGenerator.NAMESPACE, base + "/0"), spec, items.get(0)));
        Map<Integer, Key> written = new HashMap<>();
        for (int level = 1; level <= count; level++) {
            Map.Entry<Integer, GaugeSpec.Entry> found = items.floorEntry(level);
            if (found == null || found.getKey() == 0) {
                levels.add(levels.get(0));
                continue;
            }
            Key model = written.get(found.getKey());
            if (model == null) {
                model = this.entryLevel(Key.of(ResourceGenerator.NAMESPACE, base + "/" + found.getKey()), spec, found.getValue());
                written.put(found.getKey(), model);
            }
            levels.add(model);
        }
        return levels;
    }

    private Key entryLevel(Key key, GaugeSpec spec, GaugeSpec.Entry entry) {
        return switch (entry.type()) {
            case TEXTURE -> this.textureLevel(key, spec, entry.id());
            case ITEM -> entry.id();
            case MODEL -> {
                this.writeJson(this.item(key), ResourceGenerator.gaugeItem(spec, Map.of("type", "minecraft:model", "model", entry.id().toString(),
                        "tints", List.of(spec.tinted() ? ResourceGenerator.tintFromData() : ResourceGenerator.constant()))));
                yield key;
            }
        };
    }

    private Key textureLevel(Key key, GaugeSpec spec, @Nullable Key fill) {
        Map<String, Object> textures = new LinkedHashMap<>();
        List<Object> tints = new ArrayList<>();
        if (spec.background() != null) {
            textures.put("layer" + textures.size(), spec.background().toString());
            tints.add(ResourceGenerator.constant());
        }
        if (fill != null) {
            textures.put("layer" + textures.size(), fill.toString());
            tints.add(spec.tinted() ? ResourceGenerator.tintFromData() : ResourceGenerator.constant());
        }
        if (spec.overlay() != null) {
            textures.put("layer" + textures.size(), spec.overlay().toString());
            tints.add(ResourceGenerator.constant());
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("parent", spec.parentOrDefault().toString());
        json.put("textures", textures);
        if (spec.display() != null) json.put("display", spec.display());
        this.writeJson(this.model(key), json);
        this.writeJson(this.item(key), ResourceGenerator.gaugeItem(spec, Map.of("type", "minecraft:model", "model", key.toString(), "tints", tints)));
        return key;
    }

    private BufferedImage readTexture(Key texture) throws IOException {
        String relative = "assets/" + texture.namespace() + "/textures/" + texture.value() + ".png";
        for (Pack pack : CraftEngine.instance().packManager().loadedPacks()) {
            for (Path folder : pack.resourcePackFolders()) {
                Path file = folder.resolve(relative);
                if (Files.isRegularFile(file)) {
                    BufferedImage image = ImageIO.read(file.toFile());
                    if (image != null) return image;
                }
            }
        }
        throw new IOException("Texture " + texture + " not found in any CraftEngine pack");
    }

    private Path model(Key key) {
        return this.root.resolve("assets/" + key.namespace() + "/models/" + key.value() + ".json");
    }

    private Path item(Key key) {
        return this.root.resolve("assets/" + key.namespace() + "/items/" + key.value() + ".json");
    }

    private Path texture(Key key) {
        return this.root.resolve("assets/" + key.namespace() + "/textures/" + key.value() + ".png");
    }

    private void writeJson(Path path, Object json) {
        this.files.put(path, this.gson.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    private void writePng(Path path, BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        this.files.put(path, out.toByteArray());
    }

    private record GaugeTable(@Nullable List<Key> fallback, Map<Key, List<Key>> fluids) {
    }
}
