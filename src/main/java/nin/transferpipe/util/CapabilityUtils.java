package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.items.IItemHandler;

public class CapabilityUtils {

    public static void forItemHandler(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IItemHandler> func) {
        var optional = getItemHandlerOptional(level, pos, dir);
        if (optional != null)
            optional.ifPresent(func);
    }

    public static boolean hasItemHandler(Level level, BlockPos pos, Direction dir) {
        var optional = getItemHandlerOptional(level, pos, dir);
        return optional != null && optional.isPresent();
    }

    public static LazyOptional<IItemHandler> getItemHandlerOptional(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        return be != null ? be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir) : null;
    }
}
