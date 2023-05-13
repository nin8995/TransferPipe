package nin.transferpipe.util.transferpipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public enum RedstoneBehavior {
    ACTIVE_LOW,
    ACTIVE_HIGH,
    ALWAYS,
    NEVER;

    public boolean isActive(int signal) {
        return switch (this) {
            case ACTIVE_LOW -> signal == 0;
            case ACTIVE_HIGH -> signal != 0;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }

    public boolean isActive(Level level, BlockPos pos) {
        return isActive(level.getBestNeighborSignal(pos));
    }
}
