package net.nyana.fluid.api;

/**
 * 服务器上安装的 nyana-fluid 版本低于 addon 要求时, 由 {@link NyanaFluid#requireApi(String)} 抛出,.
 *
 * <p>消息里同时写明需要的版本和实际版本, 方便服主直接看懂,.
 */
public class UnsupportedApiVersionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final transient ApiVersion required;
    private final transient ApiVersion actual;

    public UnsupportedApiVersionException(ApiVersion required, ApiVersion actual) {
        super("This plugin needs nyana-fluid API " + required + " or newer, but the server has " + actual
                + ". Update NyanaFluid.");
        this.required = required;
        this.actual = actual;
    }

    public ApiVersion required() {
        return this.required;
    }

    public ApiVersion actual() {
        return this.actual;
    }
}
