package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * 一次玩家交互的上下文：谁、用哪只手、点在哪个方块的哪个面。点的不是方块时，位置信息为空。
 */
public final class InteractionContext {
    /**
     * 没有方块位置的交互。
     */
    public static InteractionContext of(Player player, InteractionHand hand) {
        return new InteractionContext(player, hand, null, null, null);
    }

    /**
     * 点在方块上的交互。
     */
    public static InteractionContext of(Player player, InteractionHand hand, World world, BlockPos pos, @Nullable Direction side) {
        return new InteractionContext(player, hand, Objects.requireNonNull(world, "world"), Objects.requireNonNull(pos, "pos"), side);
    }

    private final Player player;
    private final InteractionHand hand;
    private final @Nullable World world;
    private final @Nullable BlockPos pos;
    private final @Nullable Direction side;

    private InteractionContext(Player player, InteractionHand hand, @Nullable World world, @Nullable BlockPos pos, @Nullable Direction side) {
        this.player = Objects.requireNonNull(player, "player");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.world = world;
        this.pos = pos;
        this.side = side;
    }

    public Player player() {
        return this.player;
    }

    public InteractionHand hand() {
        return this.hand;
    }

    public Optional<World> world() {
        return Optional.ofNullable(this.world);
    }

    public Optional<BlockPos> pos() {
        return Optional.ofNullable(this.pos);
    }

    public Optional<Direction> side() {
        return Optional.ofNullable(this.side);
    }
}
