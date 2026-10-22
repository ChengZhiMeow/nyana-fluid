package net.nyana.fluid.api.internal;

import net.nyana.fluid.api.platform.BridgeProvider;
import net.nyana.fluid.api.platform.PlatformInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Picks one implementation per bridge: among the providers that support the environment, the highest
 * priority wins; equal priorities keep the order they were listed in. A provider whose {@code create} throws is reported
 * and the next one is tried, so a newer implementation that fails to load (wrong CE build, missing class) falls back to
 * the older one instead of stopping the plugin.
 */
public final class BridgeSelection {
    /**
     * @throws IllegalStateException no provider supports the environment, or all that do failed
     */
    public static <B> Selected<B> select(String bridge, List<? extends BridgeProvider<? extends B>> providers, PlatformInfo info,
                                         BiConsumer<String, Throwable> onFailure) {
        List<BridgeProvider<? extends B>> candidates = new ArrayList<>();
        for (BridgeProvider<? extends B> provider : providers) {
            if (provider.supports(info)) candidates.add(provider);
        }
        candidates.sort(Comparator.comparingInt((BridgeProvider<? extends B> provider) -> provider.priority()).reversed());
        List<String> skipped = new ArrayList<>();
        for (BridgeProvider<? extends B> provider : candidates) {
            try {
                B created = provider.create(info);
                if (created == null) throw new IllegalStateException("create returned null");
                return new Selected<>(created, provider.name(), List.copyOf(skipped));
            } catch (RuntimeException | LinkageError failure) {
                skipped.add(provider.name() + " (" + failure + ")");
                onFailure.accept(provider.name(), failure);
            }
        }
        throw new IllegalStateException("No " + bridge + " bridge can run here" + (skipped.isEmpty() ? "" : "; failed: " + skipped));
    }

    private BridgeSelection() {
    }

    /**
     * The chosen implementation.
     */
    public record Selected<B>(B bridge, String provider, List<String> skipped) {
    }
}
