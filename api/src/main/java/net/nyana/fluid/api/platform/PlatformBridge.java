package net.nyana.fluid.api.platform;

import org.jetbrains.annotations.ApiStatus;

/**
 * 各个版本 bridge 的入口。只由 NyanaFluid 插件实现。
 */
@ApiStatus.Internal
public interface PlatformBridge {
    RegionBridge region();

    SchedulerBridge scheduler();

    /**
     * tick 门控：CE 26.9 软门控 / CE 26.10+ 原生休眠。
     */
    TickerBridge ticker();

    ItemDataBridge itemData();

    /**
     * 方块状态修改与 NMS 参数转换。
     */
    BlockStateBridge blockState();

    /**
     * 原版物品、音效 id 的版本差异。
     */
    VanillaBridge vanilla();

    /**
     * 只发包、不存盘的展示实体。
     */
    DisplayBridge display();
}
