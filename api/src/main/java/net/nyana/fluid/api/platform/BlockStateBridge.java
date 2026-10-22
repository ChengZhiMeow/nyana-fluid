package net.nyana.fluid.api.platform;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * 把 CE 方块行为回调里的 NMS 参数转换成 CE 类型,.CE 方块行为的 {@code Object[] args}
 * 按 MC 版本排列, 转换放在这里, 行为基类不直接依赖易变的签名,.
 */
@ApiStatus.Internal
public interface BlockStateBridge {
    /**
     * 把 NMS 的 level 和方块坐标转换成 CE 的世界和坐标,.用于 {@code neighborChanged}、{@code getAnalogOutputSignal}
     * 这类回调（它们的 {@code args[1]} 是 level, {@code args[2]} 是坐标）,.
     */
    @Nullable BlockLocation locate(Object level, Object position);

    /**
     * 世界里的一个方块位置,.
     */
    record BlockLocation(World world, BlockPos pos) {
    }
}
