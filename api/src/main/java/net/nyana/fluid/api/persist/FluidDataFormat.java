package net.nyana.fluid.api.persist;

/**
 * 数据格式的常量,.读和写共用同一组上限：写端超限直接拒绝, 不会写出读端读不了的数据,.
 */
public final class FluidDataFormat {
    /**
     * 物品上的根键, 也是方块数据的默认 {@code data_key},.
     */
    public static final String ROOT_KEY = "nyana-fluid";
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_SLOTS = 64;
    /**
     * 每个槽最多组件数,.
     */
    public static final int MAX_COMPONENTS = 64;
    /**
     * key 的最大长度（字符）,.
     */
    public static final int MAX_KEY_LENGTH = 256;
    /**
     * 读取时最多连续执行的迁移步数,.
     */
    public static final int MAX_MIGRATION_STEPS = 8;

    private FluidDataFormat() {
    }
}
