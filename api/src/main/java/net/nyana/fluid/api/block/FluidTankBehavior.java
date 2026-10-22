package net.nyana.fluid.api.block;

import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.DoubleBlockHalf;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.item.FluidInteractions;
import net.nyana.fluid.api.platform.BlockStateBridge;
import net.nyana.fluid.api.platform.PlatformBridge;
import net.nyana.fluid.api.storage.FluidStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 储罐方块行为基类：拿流体容器右键存取，其他情况交给 {@code useWithoutItem}，没有提示和外观。
 * NyanaFluid 不注册储罐类型，插件用 {@link #factory()} 注册或继承。
 */
public class FluidTankBehavior extends BukkitBlockBehavior implements EntityBlock {
    /**
     * 给 CE {@code BlockBehaviors.register} 用的工厂，规格按 {@link TankSpec#parse} 从方块配置里读。
     * 插件用它注册自己的储罐类型，例如 {@code BlockBehaviors.register(Key.of("myplugin", "tank"), FluidTankBehavior.factory())}。
     */
    public static BlockBehaviorFactory<FluidTankBehavior> factory() {
        return (block, section) -> new FluidTankBehavior(block, TankSpec.parse(section));
    }

    private final TankSpec spec;
    private int controllerId;

    protected FluidTankBehavior(BlockDefinition block, TankSpec spec) {
        super(block);
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    public TankSpec spec() {
        return this.spec;
    }

    /**
     * CE 分配的 controller 序号，用来在组合 controller 里找到自己的那一个。
     */
    public int controllerId() {
        return this.controllerId;
    }

    /**
     * 创建 controller。覆写它来换成自己的 controller。
     */
    protected FluidTankController createController(BlockEntity blockEntity) {
        return new FluidTankController(blockEntity, this.spec);
    }

    /**
     * 取某个位置上属于这个行为的 controller；是多方块储罐的部分时取主方块的。
     */
    public @Nullable FluidTankController controllerAt(CEWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null || blockEntity.controller == null) return null;
        FluidTankPart part = blockEntity.controller.getAt(FluidTankPart.class, this.controllerId);
        if (part != null) return part.main();
        return blockEntity.controller.getAt(FluidTankController.class, this.controllerId);
    }

    /**
     * 多方块储罐：{@code state} 不是主方块时，返回主方块相对它的偏移，这个方块就只是转发给主方块的 {@link FluidTankPart}。
     * 默认识别 CE {@code double_high_block} 的上半（{@code half=upper}），主方块是下面那格。
     */
    protected @Nullable BlockPos mainOffset(ImmutableBlockState state) {
        Property<?> half = state.getProperty("half");
        return half != null && state.getNullable(half) == DoubleBlockHalf.UPPER ? new BlockPos(0, -1, 0) : null;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        BlockPos offset = this.mainOffset(blockEntity.blockState);
        return offset == null ? this.createController(blockEntity) : new FluidTankPart(blockEntity, this, offset);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    /**
     * 拿流体容器时存取；空手或拿着别的物品时返回 {@code TRY_EMPTY_HAND}，交给 {@link #useWithoutItem}（例如打开菜单）。
     */
    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getItem() == null || context.getItem().isEmpty()) return InteractionResult.TRY_EMPTY_HAND;
        FluidTankController controller = this.controllerAt(context.getWorld().storageWorld(), context.getClickedPos());
        FluidStorage target = controller == null ? null : controller.fluidStorage(context.getClickedFace());
        if (target == null) return InteractionResult.TRY_EMPTY_HAND;
        InteractionResult result = FluidInteractions.interact(context, target);
        return result == InteractionResult.PASS ? InteractionResult.TRY_EMPTY_HAND : result;
    }

    /**
     * 从 CE 透传的原版方块回调参数（{@code neighborChanged}、{@code getAnalogOutputSignal} 等的 {@code args}）里找到储罐。
     */
    protected @Nullable FluidTankController controllerFromArgs(Object[] args) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge == null || args == null || args.length < 3) return null;
        BlockStateBridge.BlockLocation location = bridge.blockState().locate(args[1], args[2]);
        if (location == null) return null;
        CEWorld world = location.world().storageWorld();
        return world == null ? null : this.controllerAt(world, location.pos());
    }
}
