package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.util.forge.ForgeUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class SortingPipe extends TransferPipe implements FunctionChanger {

    private final BiPredicate<List<Item>, Item> sorter;

    public SortingPipe(BiPredicate<List<Item>, Item> sorter){
        this.sorter = sorter;
    }

    @Override
    public Object storeAndChange(BlockPos pos, TileBaseTransferNode node) {
        var cache = node.sortingFunc;
        node.sortingFunc = sorter;
        return cache;
    }

    @Override
    public void restore(Object cache, TileBaseTransferNode node) {
        node.sortingFunc = (BiPredicate<List<Item>, Item>) cache;
    }

    @Override
    public boolean isWorkPlace(Level level, BlockPos pos, @Nullable Direction dir) {
        return ForgeUtils.hasItemHandler(level, pos, dir);
    }
}
