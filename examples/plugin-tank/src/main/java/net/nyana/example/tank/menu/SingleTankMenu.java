package net.nyana.example.tank.menu;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.example.tank.block.ExampleTankController;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.render.FluidCells;
import net.nyana.fluid.api.render.FluidGauges;
import net.nyana.fluid.api.storage.FluidSlotView;
import org.bukkit.inventory.ItemStack;

import java.util.List;

final class SingleTankMenu extends TankMenu {
    private static final Key BOTTLE = Key.of("example_plugin", "tank_bottle");

    SingleTankMenu(ExampleTankController tank) {
        super(tank, "example_plugin:gui_tank_single");
    }

    @Override
    void refreshContents() {
        FluidSlotView view = this.tank.tanks().slot(0);
        FluidVariant fluid = view.isBlank() ? null : view.variant();
        long amount = view.amount();
        long capacity = view.capacity();
        Component name = fluid == null ? Component.text("空") : fluid.displayName();
        List<Component> lore = List.of(Component.text(FluidAmount.format(amount, capacity)));
        super.set(TankMenu.ICON, TankMenu.icon(fluid, lore));
        super.set(TankMenu.SCREEN, TankMenu.named(FluidGauges.item(SingleTankMenu.BOTTLE, fluid, amount, capacity), name, lore));
        super.set(TankMenu.METER, TankMenu.named(FluidGauges.item(TankMenu.METER_GAUGE, fluid, amount, capacity), name, lore));
        List<FluidStack> layers = fluid == null ? List.of() : List.of(FluidStack.of(fluid, amount));
        ItemStack[][] cells = FluidCells.cells(TankMenu.CELLS, TankMenu.GAUGE_COLUMNS, TankMenu.GAUGE_ROWS, layers, capacity);
        ItemStack[][] ticks = FluidCells.cells(TankMenu.CELLS_TICKS, TankMenu.GAUGE_COLUMNS, TankMenu.GAUGE_ROWS, layers, capacity);
        for (int row = 0; row < TankMenu.GAUGE_ROWS; row++) {
            for (int column = 0; column < TankMenu.GAUGE_COLUMNS; column++) {
                ItemStack cell = column == TankMenu.GAUGE_COLUMNS - 1 ? ticks[row][column] : cells[row][column];
                super.set(row * 9 + TankMenu.GAUGE_FIRST_COLUMN + column, TankMenu.named(cell, name, lore));
            }
        }
    }
}
