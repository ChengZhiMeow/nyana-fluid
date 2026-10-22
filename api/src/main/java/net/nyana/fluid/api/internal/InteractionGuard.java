package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers players' fluid interactions so the plugin can cancel the client's follow-up use-item packets.
 */
public final class InteractionGuard {
    /**
     * How many player ticks after an interaction its follow-up packets may arrive. The two packets of one click can
     * straddle a tick; the client repeats right-click at most every 4 ticks, so 2 never swallows a new click.
     */
    public static final int WINDOW_TICKS = 2;
    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();

    /**
     * Records that a player's click was handled as a fluid interaction (successful or refused).
     */
    public static void mark(UUID player, InteractionHand hand, int tick) {
        InteractionGuard.MARKS.put(player, new Mark(hand, tick));
    }

    /**
     * The hand of the player's fluid interaction within {@link #WINDOW_TICKS}, if any.
     */
    public static @Nullable InteractionHand recent(UUID player, int tick) {
        Mark mark = InteractionGuard.MARKS.get(player);
        if (mark == null) return null;
        int age = tick - mark.tick;
        if (age < 0 || age > InteractionGuard.WINDOW_TICKS) {
            InteractionGuard.MARKS.remove(player, mark);
            return null;
        }
        return mark.hand;
    }

    /**
     * Drops the player's mark, e.g. when they leave.
     */
    public static void forget(UUID player) {
        InteractionGuard.MARKS.remove(player);
    }

    private InteractionGuard() {
    }

    private record Mark(InteractionHand hand, int tick) {
    }
}
