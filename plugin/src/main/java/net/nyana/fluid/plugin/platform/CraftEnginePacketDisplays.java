package net.nyana.fluid.plugin.platform;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.nyana.fluid.api.internal.ItemStacks;
import net.nyana.fluid.api.platform.DisplayBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CraftEnginePacketDisplays implements DisplayBridge {
    @Override
    public int nextEntityId() {
        return EntityUtils.ENTITY_COUNTER.incrementAndGet();
    }

    @Override
    public Object spawnItemDisplay(int id, UUID uuid, double x, double y, double z) {
        return ClientboundAddEntityPacketProxy.INSTANCE.newInstance(id, uuid, x, y, z, 0f, 0f, EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0d);
    }

    @Override
    public Object remove(int... ids) {
        return ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(IntList.of(ids));
    }

    @Override
    public Object itemDisplayData(int id, ItemDisplayState state, int interpolationDelay, int interpolationDuration) {
        List<Object> values = new ArrayList<>();
        DisplayData.TransformationInterpolationDelay.addEntityData(interpolationDelay, values);
        DisplayData.TransformationInterpolationDuration.addEntityData(interpolationDuration, values);
        DisplayData.Translation.addEntityData(state.translation(), values);
        DisplayData.Scale.addEntityData(state.scale(), values);
        DisplayData.LeftRotation.addEntityData(state.leftRotation(), values);
        DisplayData.BrightnessOverride.addEntityData(state.brightness(), values);
        DisplayData.ViewRange.addEntityData(state.viewRange(), values);
        DisplayData.ItemDisplayData.ItemStack.addEntityData(ItemStacks.toCe(state.item()).minecraftItem(), values);
        return ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(id, values);
    }
}
