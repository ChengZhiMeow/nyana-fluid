package net.nyana.example.tank.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.example.tank.block.ExampleTankController;
import net.nyana.example.tank.block.type.EditMode;
import net.nyana.example.tank.menu.type.MenuType;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.item.DetachedItemAccess;
import net.nyana.fluid.api.item.ItemAccess;
import net.nyana.fluid.api.render.FluidCells;
import net.nyana.fluid.api.render.FluidModels;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.storage.FluidTransfer;
import net.nyana.fluid.api.transaction.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TankMenu implements InventoryHolder {
    public static final int INPUT = 8;
    public static final int OUTPUT = 26;
    static final int ICON = 0;
    static final int SCREEN = 1;
    static final int MODE = 18;
    static final int METER = 19;
    static final int ARROW = 17;
    static final int GAUGE_FIRST_COLUMN = 3;
    static final int GAUGE_COLUMNS = 4;
    static final int GAUGE_ROWS = 3;
    static final Key CELLS = Key.of("example_plugin", "tank_cells");
    static final Key CELLS_TICKS = Key.of("example_plugin", "tank_cells_ticks");
    static final Key METER_GAUGE = Key.of("example_plugin", "tank_meter");
    private static final Key AIR = Key.of("minecraft", "air");
    private static final Key MODE_ICON = Key.of("example_plugin", "gui/mode");
    private static final int GUI_WIDTH = 176;

    public static void open(Player player, ExampleTankController tank) {
        TankMenu menu = tank.menuType() == MenuType.SINGLE ? new SingleTankMenu(tank) : new MixedTankMenu(tank);
        tank.menus().add(menu);
        menu.refresh();
        player.openInventory(menu.inventory);
    }

    static Component title(String background, String name) {
        int width = name.codePoints().map(c -> c < 0x80 ? 6 : 9).sum() - 1;
        int shift = (TankMenu.GUI_WIDTH - width) / 2 - (TankMenu.GUI_WIDTH + 1);
        return Component.text()
                .append(Component.text("<shift:-8><image:" + background + ">", NamedTextColor.WHITE))
                .append(Component.text("<shift:" + shift + ">" + name))
                .build();
    }

    static ItemStack icon(@Nullable FluidVariant fluid, List<Component> lore) {
        if (fluid == null) return TankMenu.named(TankMenu.blank(), Component.text("空"), lore);
        return TankMenu.named(FluidCells.cells(1, 1, List.of(FluidStack.of(fluid, 1)), 1)[0][0], fluid.displayName(), lore);
    }

    static ItemStack blank() {
        return FluidModels.item(TankMenu.AIR);
    }

    static ItemStack named(ItemStack stack, Component name, List<Component> lore) {
        stack.setData(DataComponentTypes.ITEM_NAME, name);
        List<Component> lines = new ArrayList<>(lore.size());
        for (Component line : lore)
            lines.add(line.colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        return stack;
    }

    protected final ExampleTankController tank;
    private final Inventory inventory;

    TankMenu(ExampleTankController tank, String background) {
        this.tank = tank;
        this.inventory = Bukkit.createInventory(this, 27, TankMenu.title(background, tank.title()));
        ItemStack filler = TankMenu.blank();
        filler.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        for (int slot = 0; slot < 27; slot++)
            if (slot != TankMenu.INPUT && slot != TankMenu.OUTPUT) this.inventory.setItem(slot, filler);
        this.inventory.setItem(TankMenu.ARROW, TankMenu.named(TankMenu.blank(), Component.text("容器处理"), List.of(Component.text("上面放容器, 处理后的结果在下面"))));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public final void refresh() {
        EditMode mode = this.tank.mode();
        ItemStack button = FluidModels.item(TankMenu.MODE_ICON);
        button.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(mode.icon()).build());
        this.inventory.setItem(TankMenu.MODE, TankMenu.named(button, Component.text("容器模式：" + mode.label(), NamedTextColor.WHITE),
                List.of(Component.text(mode.description()), Component.text("点击切换", NamedTextColor.DARK_GRAY))));
        this.refreshContents();
    }

    abstract void refreshContents();

    public void click(Player player, int slot) {
        if (slot != TankMenu.MODE) return;
        this.tank.cycleMode();
        this.tank.refreshMenus();
        this.processInput();
    }

    public void processInput() {
        ItemStack stack = this.inventory.getItem(TankMenu.INPUT);
        if (stack == null || stack.isEmpty()) return;
        DetachedItemAccess access = ItemAccess.detached(BukkitAdaptor.adapt(stack.asOne()));
        FluidStorage item = access.fluidStorage();
        if (item == null) return;
        EditMode mode = this.tank.mode();
        try (Transaction transaction = Transaction.openRoot()) {
            long moved = 0;
            if (mode != EditMode.FILL)
                moved = FluidTransfer.move(item, this.tank.tanks(), fluid -> true, Long.MAX_VALUE, transaction);
            if (moved == 0 && mode != EditMode.EMPTY) {
                moved = FluidTransfer.move(this.tank.tanks(), item, fluid -> true, Long.MAX_VALUE, transaction);
            }
            if (moved == 0) return;
            List<Item> results = access.results();
            ItemStack result = results.isEmpty() || !(results.get(0) instanceof BukkitItem bukkit) ? null : bukkit.getBukkitItem().clone();
            ItemStack output = this.inventory.getItem(TankMenu.OUTPUT);
            boolean emptyOutput = output == null || output.isEmpty();
            if (result != null && !emptyOutput && (!output.isSimilar(result) || output.getAmount() + result.getAmount() > output.getMaxStackSize()))
                return;
            transaction.commit();
            this.inventory.setItem(TankMenu.INPUT, stack.getAmount() > 1 ? stack.subtract() : null);
            if (result != null)
                this.inventory.setItem(TankMenu.OUTPUT, emptyOutput ? result : output.add(result.getAmount()));
        }
    }

    public void close(Player player) {
        this.tank.menus().remove(this);
        for (int slot : new int[]{TankMenu.INPUT, TankMenu.OUTPUT}) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            this.inventory.setItem(slot, null);
            player.getInventory().addItem(stack).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    public void closeAll() {
        for (HumanEntity viewer : List.copyOf(this.inventory.getViewers())) viewer.closeInventory();
    }

    void set(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
    }
}
