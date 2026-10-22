package net.nyana.fluid.api.transaction;

/**
 * 在错误的线程或错误的 Folia 区域访问了事务或存储,.
 *
 * <p>事务只能在打开它的线程上使用；世界上的存储只能在拥有它所在区块的区域线程上访问,.
 * {@link net.nyana.fluid.api.storage.FluidTransfer#tryMove} 会把它转换成
 * {@code TransferResult.Status.WRONG_REGION},.
 */
public class FluidAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FluidAccessException(String message) {
        super(message);
    }
}
