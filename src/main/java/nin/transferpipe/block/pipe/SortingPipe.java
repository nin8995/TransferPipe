package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.node.BaseTileNode;
import nin.transferpipe.util.forge.ForgeUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;

public class SortingPipe extends Pipe implements FunctionChanger {

    public final BiPredicate<List<Item>, Item> sortingFunc;

    public SortingPipe(BiPredicate<List<Item>, Item> sortingFunc) {
        this.sortingFunc = sortingFunc;
    }

    @Override
    public Object storeAndChange(BlockPos pos, BaseTileNode<?> node) {
        var cache = node.sortingFunc;
        node.sortingFunc = sortingFunc;
        return cache;
    }

    @Override
    public void restore(Object cache, BaseTileNode<?> node) {
        node.sortingFunc = (BiPredicate<List<Item>, Item>) cache;
    }

    @Override
    public boolean isWorkPlace(Level level, BlockPos pos, @Nullable Direction dir) {
        return ForgeUtils.hasItemHandler(level, pos, dir);
    }
}
