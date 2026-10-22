package net.nyana.fluid.api.render;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.platform.DisplayBridge.ItemDisplayState;
import net.nyana.fluid.api.platform.PlatformBridge;
import net.nyana.fluid.api.platform.SchedulerBridge;
import net.nyana.fluid.api.storage.FluidSlotView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 储罐里的流体展示：每个槽一个只发包的物品展示实体，通过 CE 方块实体的渲染元素绑定到方块上，跟着区块显示、隐藏。
 * 多种流体按 {@link FluidTankController#displayOrder()} 从下往上叠放。液面变化时先换成新一档的模型、用缩放保持原来的高度，
 * 再插值到新高度。
 *
 * <p>子类实现 {@link #model(FluidVariant, int)} 决定每种流体显示什么；默认实现是 {@link DefaultFluidTankDisplay}。
 */
public abstract class FluidTankDisplay implements BlockEntityElement {
    private static final int FULL = 16;
    private final FluidTankController tank;
    private final TankRenderSpec spec;
    private final Layer[] layers;
    private final List<SchedulerBridge.Task> tasks = new ArrayList<>();

    protected FluidTankDisplay(FluidTankController tank, TankRenderSpec spec) {
        this.tank = tank;
        this.spec = spec;
        this.layers = new Layer[tank.spec().slots()];
        for (int i = 0; i < this.layers.length; i++) this.layers[i] = new Layer();
    }

    /**
     * 某种流体在 {@code height}（1..16 像素，满格 16）这一档显示的物品；返回 {@code null} 不显示这种流体。
     * 物品的模型应当是从底部到 {@code height} 像素高的液体；{@link #scalesModel(FluidVariant)} 为 {@code true} 时则是满格模型，由缩放压到这个高度。
     */
    protected abstract @Nullable ItemStack model(FluidVariant fluid, int height);

    /**
     * {@link #model} 返回的是否是满格模型（靠缩放到对应高度）。默认 {@code false}。
     */
    protected boolean scalesModel(FluidVariant fluid) {
        return false;
    }

    /**
     * 亮度 0..15，-1 不覆盖。默认：渲染设置 → 这种流体的单独设置 → 流体发光等级。
     */
    protected int brightness(FluidVariant fluid) {
        if (this.spec.brightness() >= 0) return this.spec.brightness();
        int custom = this.spec.fluid(fluid.fluidKey()).map(TankRenderSpec.FluidRender::brightness).orElse(-1);
        if (custom >= 0) return custom;
        int luminance = fluid.attributes().luminance();
        return luminance > 0 ? luminance : -1;
    }

    public TankRenderSpec spec() {
        return this.spec;
    }

    public FluidTankController tank() {
        return this.tank;
    }

    /**
     * 内容或选中变化后由储罐调用；{@code animate} 为 {@code false} 时直接跳到最终状态。
     */
    public void refresh(boolean animate) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        State[] targets = this.targets();
        if (bridge == null) {
            for (int i = 0; i < this.layers.length; i++) this.layers[i].state = targets[i];
            return;
        }
        this.cancelTasks();
        List<Player> viewers = this.viewers();
        boolean smooth = animate && this.spec.transitionTicks() > 0;
        List<Object> now = new ArrayList<>();
        List<Object> later = new ArrayList<>();
        List<Integer> removeLater = new ArrayList<>();
        for (int i = 0; i < this.layers.length; i++) {
            Layer layer = this.layers[i];
            State previous = layer.state;
            State target = targets[i];
            layer.state = target;
            if (target == null) {
                if (!layer.spawned) continue;
                if (smooth && previous != null) {
                    now.add(this.data(bridge, layer, previous, previous.height, 0, this.spec.transitionTicks(), true));
                    removeLater.add(layer.id);
                } else {
                    now.add(bridge.display().remove(layer.id));
                    layer.spawned = false;
                }
                continue;
            }
            boolean sameFluid = previous != null && layer.spawned && previous.fluid.equals(target.fluid);
            if (!layer.spawned) {
                now.add(bridge.display().spawnItemDisplay(layer.id, layer.uuid, this.x(), this.y(), this.z()));
                layer.spawned = true;
            }
            if (!smooth) {
                now.add(this.data(bridge, layer, target, target.height, 0, 0, false));
            } else if (!sameFluid) {
                now.add(this.data(bridge, layer, target, 0, 0, 0, false));
                later.add(this.data(bridge, layer, target, target.height, 0, this.spec.transitionTicks(), false));
            } else if (previous.height != target.height) {
                State start = new State(target.fluid, target.height, previous.bottom);
                now.add(this.data(bridge, layer, start, previous.height, 0, 0, false));
                later.add(this.data(bridge, layer, target, target.height, 0, this.spec.transitionTicks(), false));
            } else if (previous.bottom != target.bottom) {
                now.add(this.data(bridge, layer, target, target.height, 0, this.spec.transitionTicks(), false));
            }
        }
        this.send(viewers, now);
        CEWorld world = this.world();
        if (world == null) return;
        int chunkX = this.tank.blockEntity().pos.x() >> 4;
        int chunkZ = this.tank.blockEntity().pos.z() >> 4;
        if (!later.isEmpty()) {
            this.tasks.add(bridge.scheduler().runLater(world.world(), chunkX, chunkZ, this.spec.transitionDelayTicks(), () -> this.send(this.viewers(), later)));
        }
        if (!removeLater.isEmpty()) {
            int[] ids = removeLater.stream().mapToInt(Integer::intValue).toArray();
            this.tasks.add(bridge.scheduler().runLater(world.world(), chunkX, chunkZ, this.spec.transitionTicks() + 1L, () -> {
                this.send(this.viewers(), List.of(bridge.display().remove(ids)));
                for (Layer layer : this.layers) if (layer.state == null) layer.spawned = false;
            }));
        }
    }

    @Override
    public void show(Player player) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge == null) return;
        List<Object> packets = new ArrayList<>();
        for (Layer layer : this.layers) {
            if (layer.state == null) continue;
            packets.add(bridge.display().spawnItemDisplay(layer.id, layer.uuid, this.x(), this.y(), this.z()));
            packets.add(this.data(bridge, layer, layer.state, layer.state.height, 0, 0, false));
            layer.spawned = true;
        }
        if (!packets.isEmpty()) player.sendPackets(packets, false);
    }

    @Override
    public void hide(Player player) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge == null) return;
        int[] ids = new int[this.layers.length];
        for (int i = 0; i < ids.length; i++) ids[i] = this.layers[i].id;
        player.sendPacket(bridge.display().remove(ids), false);
    }

    @Override
    public void update(Player player) {
        this.hide(player);
        this.show(player);
    }

    @Override
    public void deactivate() {
        this.cancelTasks();
    }

    private State[] targets() {
        State[] targets = new State[this.layers.length];
        long capacity = this.tank.totalCapacity();
        if (capacity <= 0) return targets;
        int bottom = 0;
        for (int slot : this.tank.displayOrder()) {
            FluidSlotView view = this.tank.tanks().slot(slot);
            if (view.isBlank() || view.amount() <= 0 || bottom >= FluidTankDisplay.FULL) continue;
            int levels = this.spec.levels();
            int level = (int) Math.min(levels, Math.max(1, Math.ceil((double) view.amount() / capacity * levels)));
            int height = Math.min(FluidTankDisplay.FULL - bottom, Math.max(1, Math.round((float) FluidTankDisplay.FULL * level / levels)));
            targets[slot] = new State(view.variant(), height, bottom);
            bottom += height;
        }
        return targets;
    }

    private Object data(PlatformBridge bridge, Layer layer, State state, int shownHeight, int delay, int duration, boolean collapse) {
        float[] box = this.spec.bounds();
        float width = box[3] - box[0];
        float height = box[4] - box[1];
        float depth = box[5] - box[2];
        int modelHeight = state.height;
        boolean scales = this.scalesModel(state.fluid);
        float visible = collapse ? 0 : (float) shownHeight / FluidTankDisplay.FULL;
        float geometry = scales ? 1f : (float) modelHeight / FluidTankDisplay.FULL;
        float scaleY = geometry <= 0 ? 0 : height * visible / geometry;
        float bottom = box[1] - 0.5f + height * state.bottom / FluidTankDisplay.FULL;
        Vector3f extra = this.spec.scale();
        Vector3f scale = new Vector3f(width * extra.x, scaleY * extra.y, depth * extra.z);
        Vector3f translation = new Vector3f((box[0] + box[3]) / 2 - 0.5f, bottom + 0.5f * scaleY * extra.y, (box[2] + box[5]) / 2 - 0.5f)
                .add(this.spec.translation());
        Vector3f angles = this.spec.rotation();
        Quaternionf rotation = new Quaternionf().rotateXYZ((float) Math.toRadians(angles.x), (float) Math.toRadians(angles.y), (float) Math.toRadians(angles.z));
        int light = this.brightness(state.fluid);
        ItemDisplayState display = new ItemDisplayState(this.model(state.fluid, modelHeight), translation, scale, rotation,
                light < 0 ? -1 : light << 4 | light << 20, this.spec.viewRange());
        return bridge.display().itemDisplayData(layer.id, display, delay, duration);
    }

    private void send(List<Player> viewers, List<Object> packets) {
        if (packets.isEmpty()) return;
        for (Player viewer : viewers) viewer.sendPackets(packets, false);
    }

    private List<Player> viewers() {
        CEWorld world = this.world();
        if (world == null) return List.of();
        BlockEntity entity = this.tank.blockEntity();
        CEChunk chunk = world.getChunkAtIfLoaded(entity.pos.x() >> 4, entity.pos.z() >> 4);
        return chunk == null ? List.of() : chunk.getTrackedBy();
    }

    private @Nullable CEWorld world() {
        BlockEntity entity = this.tank.blockEntity();
        return entity == null ? null : entity.world;
    }

    private double x() {
        return this.tank.blockEntity().pos.x() + 0.5;
    }

    private double y() {
        return this.tank.blockEntity().pos.y() + 0.5;
    }

    private double z() {
        return this.tank.blockEntity().pos.z() + 0.5;
    }

    private void cancelTasks() {
        for (SchedulerBridge.Task task : this.tasks) task.cancel();
        this.tasks.clear();
    }

    /**
     * 一种流体在液罐里的显示：模型高度和底部，单位都是像素（满格 16）。
     */
    private record State(FluidVariant fluid, int height, int bottom) {
    }

    private static final class Layer {
        private final int id = NyanaFluid.service().bridge() == null ? 0 : NyanaFluid.service().bridge().display().nextEntityId();
        private final UUID uuid = UUID.randomUUID();
        private @Nullable State state;
        private boolean spawned;
    }
}
