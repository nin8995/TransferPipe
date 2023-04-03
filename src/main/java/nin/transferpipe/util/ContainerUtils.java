package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class ContainerUtils {

    public static boolean hasContainer(Level level, BlockPos pos) {
        return getContainer(level, pos) != null;
    }

    public static void forContainer(Level level, BlockPos pos, Direction dir, BiConsumer<Container, Direction> func) {
        var container = getContainer(level, pos);
        if (container != null)
            func.accept(container, dir);
    }

    @Nullable
    public static Container getContainer(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container c ? c
                : level.getBlockState(pos).getBlock() instanceof WorldlyContainerHolder holder ? holder.getContainer(level.getBlockState(pos), level, pos)
                : null;
    }
}
