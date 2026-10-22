package net.nyana.example.tank.block;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.nyana.example.tank.block.type.EditMode;
import net.nyana.example.tank.menu.TankMenu;
import net.nyana.example.tank.menu.type.MenuType;
import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.block.TankSpec;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ExampleTankController extends FluidTankController {
    private static final String MODE = "example_plugin:edit_mode";
    private final MenuType menuType;
    private final String title;
    private final Set<TankMenu> menus = new CopyOnWriteArraySet<>();
    private EditMode mode = EditMode.BOTH;

    ExampleTankController(BlockEntity blockEntity, TankSpec spec, MenuType menuType, String title) {
        super(blockEntity, spec);
        this.menuType = menuType;
        this.title = title;
    }

    public MenuType menuType() {
        return this.menuType;
    }

    public String title() {
        return this.title;
    }

    public Set<TankMenu> menus() {
        return this.menus;
    }

    public EditMode mode() {
        return this.mode;
    }

    public void cycleMode() {
        this.mode = this.mode.next();
        BlockEntity entity = super.blockEntity();
        if (entity != null && entity.world != null) entity.world.blockEntityChanged(entity.pos);
    }

    public void refreshMenus() {
        for (TankMenu menu : this.menus) menu.refresh();
    }

    @Override
    public void saveCustomData(CompoundTag data) {
        super.saveCustomData(data);
        data.putString(ExampleTankController.MODE, this.mode.name());
    }

    @Override
    public void loadCustomData(CompoundTag data) {
        super.loadCustomData(data);
        try {
            this.mode = EditMode.valueOf(data.getString(ExampleTankController.MODE, EditMode.BOTH.name()));
        } catch (IllegalArgumentException unknown) {
            this.mode = EditMode.BOTH;
        }
    }

    @Override
    protected void onContentsChanged() {
        this.refreshMenus();
    }

    @Override
    public void onRemove() {
        for (TankMenu menu : List.copyOf(this.menus)) menu.closeAll();
        super.onRemove();
    }
}
