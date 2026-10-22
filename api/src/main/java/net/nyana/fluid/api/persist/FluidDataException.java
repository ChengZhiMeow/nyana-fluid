package net.nyana.fluid.api.persist;

/**
 * 流体数据格式不合法,.由组件 codec 和数据迁移抛出, 最终会让整份数据读成
 * {@link FluidReadResult.Status#INVALID}, 原始数据原样保留,.
 */
public class FluidDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public FluidDataException(String message) {
        super(message);
    }

    public FluidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
