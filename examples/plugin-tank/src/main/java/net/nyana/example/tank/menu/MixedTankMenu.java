package net.nyana.example.tank.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.example.tank.block.ExampleTankController;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.render.FluidCells;
import net.nyana.fluid.api.render.FluidGauges;
import net.nyana.fluid.api.render.FluidModels;
import net.nyana.fluid.api.storage.FluidSlotView;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class MixedTankMenu extends TankMenu {
    private static final Key PAGE = Key.of("example_plugin", "gui/page");
    private static final int PREVIOUS = 9;
    private static final int NEXT = 10;

    private static List<Component> summary(List<FluidStack> layers, long capacity) {
        List<Component> lore = new ArrayList<>();
        long total = 0;
        for (int i = layers.size() - 1; i >= 0; i--) {
            FluidStack stack = layers.get(i);
            total += stack.amount();
            lore.add(Component.text(i == 0 ? "▶ " : "   ", NamedTextColor.YELLOW).append(stack.variant().displayName())
                    .append(Component.text(" " + FluidAmount.format(stack.amount()), NamedTextColor.GRAY)));
        }
        lore.add(Component.text("合计 " + FluidAmount.format(total, capacity), NamedTextColor.GRAY));
        lore.add(Component.text("点击一层液体或 ◀ ▶ 切换选中的流体", NamedTextColor.DARK_GRAY));
        return lore;
    }

    MixedTankMenu(ExampleTankController tank) {
        super(tank, "example_plugin:gui_tank_mixed");
    }

    @Override
    void refreshContents() {
        List<FluidStack> layers = this.layers();
        long capacity = this.tank.totalCapacity();
        List<Integer> pages = this.pages();
        int selected = this.tank.displayOrder().isEmpty() ? -1 : this.tank.displayOrder().get(0);
        FluidSlotView view = selected < 0 ? null : this.tank.tanks().slot(selected);
        FluidVariant fluid = view == null ? null : view.variant();
        long amount = view == null ? 0 : view.amount();
        List<Component> lore = List.of(Component.text(FluidAmount.format(amount) + " / " + FluidAmount.format(capacity)));
        Component name = fluid == null ? Component.text("空") : fluid.displayName();

        super.set(TankMenu.ICON, TankMenu.icon(fluid, lore));
        if (pages.isEmpty()) {
            super.set(TankMenu.SCREEN, TankMenu.named(TankMenu.blank(), name, List.of()));
        } else {
            int index = pages.indexOf(selected) + 1;
            ItemStack page = FluidModels.item(MixedTankMenu.PAGE);
            page.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(index + "/" + pages.size()).build());
            super.set(TankMenu.SCREEN, TankMenu.named(page, Component.text("第 " + index + " / " + pages.size() + " 种"), List.of(name)));
        }
        super.set(MixedTankMenu.PREVIOUS, TankMenu.named(TankMenu.blank(), Component.text("上一种流体", NamedTextColor.WHITE), List.of(Component.text("选中的流体放在最下面, 取出时优先取它"))));
        super.set(MixedTankMenu.NEXT, TankMenu.named(TankMenu.blank(), Component.text("下一种流体", NamedTextColor.WHITE), List.of(Component.text("选中的流体放在最下面, 取出时优先取它"))));
        super.set(TankMenu.METER, TankMenu.named(FluidGauges.item(TankMenu.METER_GAUGE, fluid, amount, capacity), name, lore));

        ItemStack[][] cells = FluidCells.cells(TankMenu.CELLS, TankMenu.GAUGE_COLUMNS, TankMenu.GAUGE_ROWS, layers, capacity);
        ItemStack[][] ticks = FluidCells.cells(TankMenu.CELLS_TICKS, TankMenu.GAUGE_COLUMNS, TankMenu.GAUGE_ROWS, layers, capacity);
        List<Component> summary = MixedTankMenu.summary(layers, capacity);
        for (int row = 0; row < TankMenu.GAUGE_ROWS; row++) {
            int layer = FluidCells.layerAt(row, TankMenu.GAUGE_ROWS, layers, capacity);
            Component rowName = layer < 0 ? Component.text("空") : layers.get(layer).variant().displayName();
            for (int column = 0; column < TankMenu.GAUGE_COLUMNS; column++) {
                ItemStack cell = column == TankMenu.GAUGE_COLUMNS - 1 ? ticks[row][column] : cells[row][column];
                super.set(row * 9 + TankMenu.GAUGE_FIRST_COLUMN + column, TankMenu.named(cell, rowName, summary));
            }
        }
    }

    @Override
    public void click(Player player, int slot) {
        if (slot == MixedTankMenu.PREVIOUS || slot == MixedTankMenu.NEXT) {
            List<Integer> pages = this.pages();
            if (pages.size() < 2) return;
            int current = Math.max(0, pages.indexOf(this.tank.displayOrder().get(0)));
            int next = (current + (slot == MixedTankMenu.NEXT ? 1 : -1) + pages.size()) % pages.size();
            this.tank.select(pages.get(next));
            this.tank.refreshMenus();
            return;
        }
        int column = slot % 9 - TankMenu.GAUGE_FIRST_COLUMN;
        if (column >= 0 && column < TankMenu.GAUGE_COLUMNS) {
            int layer = FluidCells.layerAt(slot / 9, TankMenu.GAUGE_ROWS, this.layers(), this.tank.totalCapacity());
            if (layer < 0) return;
            this.tank.select(this.tank.displayOrder().get(layer));
            this.tank.refreshMenus();
            return;
        }
        super.click(player, slot);
    }

    private List<Integer> pages() {
        List<Integer> pages = new ArrayList<>();
        for (int slot = 0; slot < this.tank.tanks().slotCount(); slot++)
            if (!this.tank.tanks().slot(slot).isBlank()) pages.add(slot);
        return pages;
    }

    private List<FluidStack> layers() {
        List<FluidStack> layers = new ArrayList<>();
        for (int slot : this.tank.displayOrder()) {
            FluidSlotView view = this.tank.tanks().slot(slot);
            layers.add(FluidStack.of(view.variant(), view.amount()));
        }
        return layers;
    }
}
