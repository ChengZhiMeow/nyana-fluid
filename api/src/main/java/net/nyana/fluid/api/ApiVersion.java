package net.nyana.fluid.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 语义化版本号（{@code major.minor.patch}）, 用于 {@link NyanaFluid#requireApi(String)} 等版本比较,.
 *
 * <p>只比较三段数字；{@code -SNAPSHOT}、{@code +build} 之类的后缀会被忽略,.
 */
public final class ApiVersion implements Comparable<ApiVersion> {
    /**
     * @throws IllegalArgumentException 任一部分为负数
     */
    public static ApiVersion of(int major, int minor, int patch) {
        return new ApiVersion(major, minor, patch);
    }

    /**
     * 解析 {@code "0.1"}、{@code "0.1.0"}、{@code "26.9.2-SNAPSHOT"} 这类字符串；缺省的部分按 0 处理,.
     *
     * @throws IllegalArgumentException 不是合法的版本字符串
     */
    public static ApiVersion parse(String text) {
        Objects.requireNonNull(text, "text");
        String core = text.trim();
        int cut = ApiVersion.indexOfAny(core, '-', '+', ' ');
        if (cut >= 0) core = core.substring(0, cut);
        String[] parts = core.split("\\.");
        if (core.isEmpty() || parts.length > 3) throw new IllegalArgumentException("Not a version: " + text);
        int[] values = new int[3];
        try {
            for (int i = 0; i < parts.length; i++) values[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Not a version: " + text, exception);
        }
        return new ApiVersion(values[0], values[1], values[2]);
    }

    private static int indexOfAny(String text, char... chars) {
        int best = -1;
        for (char c : chars) {
            int index = text.indexOf(c);
            if (index >= 0 && (best < 0 || index < best)) best = index;
        }
        return best;
    }

    private final int major;
    private final int minor;
    private final int patch;

    private ApiVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0)
            throw new IllegalArgumentException("Version parts must be non-negative");
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int major() {
        return this.major;
    }

    public int minor() {
        return this.minor;
    }

    public int patch() {
        return this.patch;
    }

    /**
     * 判断本版本是否不低于 {@code other},.
     */
    public boolean atLeast(ApiVersion other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(@NotNull ApiVersion other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;
        result = Integer.compare(this.minor, other.minor);
        return result != 0 ? result : Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ApiVersion version && version.major == this.major && version.minor == this.minor && version.patch == this.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch);
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor + "." + this.patch;
    }
}
