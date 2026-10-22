package net.nyana.fluid.api.internal;

import net.nyana.fluid.api.fluid.FluidVariant;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * The only two non-negotiable preconditions of storage operations: no blank resource, no negative amount.
 */
@ApiStatus.Internal
public final class Preconditions {
    public static void checkResource(FluidVariant resource, long amount) {
        Objects.requireNonNull(resource, "resource");
        if (resource.isBlank()) throw new IllegalArgumentException("Resource must not be blank");
        if (amount < 0) throw new IllegalArgumentException("Amount must not be negative: " + amount);
    }

    public static void checkAmount(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must not be negative: " + amount);
    }

    /**
     * Guards against storage implementations returning more than they were asked for.
     */
    public static long checkResult(long result, long maximum) {
        if (result < 0 || result > maximum)
            throw new IllegalStateException("Storage violated the amount contract: " + result + " / " + maximum);
        return result;
    }

    private Preconditions() {
    }
}
