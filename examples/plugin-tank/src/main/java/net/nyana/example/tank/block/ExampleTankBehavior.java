package net.nyana.example.tank.block;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.nyana.example.tank.menu.TankMenu;
import net.nyana.example.tank.menu.type.MenuType;
import net.nyana.fluid.api.block.FluidTankBehavior;
import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.block.TankSpec;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.fluid.FluidVariant;
import org.bukkit.entity.Player;

public final class ExampleTankBehavior extends FluidTankBehavior {
    private static long[] amounts(FluidTankController tank) {
        long[] amounts = new long[tank.tanks().slotCount()];
        for (int slot = 0; slot < amounts.length; slot++) amounts[slot] = tank.tanks().slot(slot).amount();
        return amounts;
    }

    private static FluidVariant[] fluids(FluidTankController tank) {
        FluidVariant[] fluids = new FluidVariant[tank.tanks().slotCount()];
        for (int slot = 0; slot < fluids.length; slot++) fluids[slot] = tank.tanks().slot(slot).variant();
        return fluids;
    }

    private final MenuType menuType;
    private final String title;

    public ExampleTankBehavior(BlockDefinition block, MenuType menuType, ConfigSection section) {
        super(block, TankSpec.parse(section));
        this.menuType = menuType;
        this.title = section.getString("title", menuType == MenuType.SINGLE ? "流体储罐" : "混合储罐");
    }

    @Override
    protected FluidTankController createController(BlockEntity blockEntity) {
        return new ExampleTankController(blockEntity, super.spec(), this.menuType, this.title);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        FluidTankController tank = super.controllerAt(context.getWorld().storageWorld(), context.getClickedPos());
        if (tank == null) return super.useOnBlock(context, state);
        long[] before = ExampleTankBehavior.amounts(tank);
        FluidVariant[] fluids = ExampleTankBehavior.fluids(tank);
        InteractionResult result = super.useOnBlock(context, state);
        if (context.getPlayer() == null || !(context.getPlayer().platformPlayer() instanceof Player player))
            return result;
        long[] after = ExampleTankBehavior.amounts(tank);
        for (int slot = 0; slot < after.length; slot++) {
            long change = after[slot] - before[slot];
            if (change == 0) continue;
            FluidVariant fluid = change > 0 ? tank.tanks().slot(slot).variant() : fluids[slot];
            long total = 0;
            for (long amount : after) total += amount;
            player.sendActionBar(Component.text((change > 0 ? "存入 " : "取出 ") + FluidAmount.format(Math.abs(change)) + " ")
                    .append(fluid.displayName())
                    .append(Component.text(", 罐内 " + FluidAmount.format(total, tank.totalCapacity()))));
            break;
        }
        return result;
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        FluidTankController tank = super.controllerAt(context.getWorld().storageWorld(), context.getClickedPos());
        if (!(tank instanceof ExampleTankController example) || context.getPlayer() == null
                || !(context.getPlayer().platformPlayer() instanceof Player player)) return InteractionResult.PASS;
        TankMenu.open(player, example);
        return InteractionResult.SUCCESS_AND_CANCEL;
    }
}
