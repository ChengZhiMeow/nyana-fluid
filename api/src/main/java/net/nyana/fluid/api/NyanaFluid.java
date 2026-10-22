package net.nyana.fluid.api;

import net.nyana.fluid.api.fluid.FluidRegistry;
import net.nyana.fluid.api.fluid.component.FluidComponentRegistry;
import net.nyana.fluid.api.internal.NyanaFluidService;
import net.nyana.fluid.api.internal.StandaloneService;
import net.nyana.fluid.api.item.FluidContainers;
import net.nyana.fluid.api.persist.FluidCodec;
import net.nyana.fluid.api.platform.PlatformInfo;
import net.nyana.fluid.api.recipe.FluidIngredientRegistry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * nyana-fluid 的静态入口。
 *
 * <p>服务器上由 NyanaFluid 插件在启用时安装真正的服务；在没有安装插件的环境（例如独立工具）里，
 * 会自动退回到一个只在内存里工作的独立实现，注册表、编解码器、事务和默认存储都能照常使用，
 * 只是没有方块刷新、事件派发和版本 bridge。
 */
public final class NyanaFluid {
    private static final ApiVersion API_VERSION = ApiVersion.of(0, 1, 0);
    private static volatile @Nullable NyanaFluidService installed;
    private static volatile @Nullable NyanaFluidService standalone;

    /**
     * 当前 API 的版本号。
     */
    public static ApiVersion apiVersion() {
        return NyanaFluid.API_VERSION;
    }

    /**
     * 要求服务器上的 API 至少是某个版本，不满足时立即给出可读的报错，
     * 而不是等到运行时才出现 {@link NoSuchMethodError}。建议在 addon 的 {@code onLoad} 里调用。
     *
     * @throws UnsupportedApiVersionException 版本不够
     */
    public static void requireApi(String minimum) {
        ApiVersion required = ApiVersion.parse(minimum);
        if (!NyanaFluid.API_VERSION.atLeast(required))
            throw new UnsupportedApiVersionException(required, NyanaFluid.API_VERSION);
    }

    /**
     * NyanaFluid 插件是否已经启用完成。启用完成时会派发
     * {@link net.nyana.fluid.api.event.NyanaFluidReadyEvent}。
     */
    public static boolean isReady() {
        NyanaFluidService service = NyanaFluid.installed;
        return service != null && service.ready();
    }

    /**
     * 流体注册表：随时可以注册、更新、注销和枚举流体。
     */
    public static FluidRegistry fluids() {
        return NyanaFluid.service().fluids();
    }

    /**
     * 组件类型注册表。组件类型要先注册，才能写进持久化数据。
     */
    public static FluidComponentRegistry components() {
        return NyanaFluid.service().components();
    }

    /**
     * 配方原料类型注册表。
     */
    public static FluidIngredientRegistry ingredients() {
        return NyanaFluid.service().ingredients();
    }

    /**
     * 物品容器注册（桶型容器、数据型容器规格）。
     */
    public static FluidContainers containers() {
        return NyanaFluid.service().containers();
    }

    /**
     * 流体数据的 NBT 编解码器，格式见 {@code docs/DATA-FORMAT.md}。
     */
    public static FluidCodec codec() {
        return NyanaFluid.service().codec();
    }

    /**
     * 当前运行环境的信息：MC / CE 版本、是否 Folia 等。
     */
    public static PlatformInfo platform() {
        return NyanaFluid.service().platform();
    }

    /**
     * 安装运行时服务。只由 NyanaFluid 插件调用。
     *
     * @throws IllegalStateException 已经安装了另一个服务
     */
    @ApiStatus.Internal
    public static synchronized void install(NyanaFluidService service) {
        Objects.requireNonNull(service, "service");
        if (NyanaFluid.installed != null && NyanaFluid.installed != service)
            throw new IllegalStateException("A NyanaFluid service is already installed");
        NyanaFluid.installed = service;
    }

    /**
     * 卸载运行时服务。只由 NyanaFluid 插件在禁用时调用。
     */
    @ApiStatus.Internal
    public static synchronized void uninstall(NyanaFluidService service) {
        if (NyanaFluid.installed == service) NyanaFluid.installed = null;
    }

    /**
     * 当前生效的服务：已安装的服务，否则是独立实现。
     */
    @ApiStatus.Internal
    public static NyanaFluidService service() {
        NyanaFluidService service = NyanaFluid.installed;
        if (service != null) return service;
        NyanaFluidService fallback = NyanaFluid.standalone;
        if (fallback == null) {
            synchronized (NyanaFluid.class) {
                fallback = NyanaFluid.standalone;
                if (fallback == null) NyanaFluid.standalone = fallback = new StandaloneService();
            }
        }
        return fallback;
    }

    private NyanaFluid() {
    }
}
