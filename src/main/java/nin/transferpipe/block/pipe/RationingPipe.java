package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.util.forge.ForgeUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RationingPipe extends TransferPipe implements FunctionChanger {

    protected final int ration;

    public RationingPipe(int ration) {
        this.ration = ration;
    }

    public int getItemRation(Level level, BlockPos pos) {
        return ration;
    }

    public int getLiquidRation(Level level, BlockPos pos) {
        return ration * 250;
    }

    @Override
    public boolean isWorkPlace(Level level, BlockPos pos, @Nullable Direction dir) {
        return ForgeUtils.hasItemHandler(level, pos, dir)
                || ForgeUtils.hasFluidHandler(level, pos, dir);
    }

    @Override
    public List<Integer> storeAndChange(BlockPos pos, TileBaseTransferNode node) {
        var cache = List.of(node.itemRation, node.liquidRation);
        node.itemRation = getItemRation(node.level, pos);
        node.liquidRation = getLiquidRation(node.level, pos);
        return cache;
    }

    @Override
    public void restore(List<?> cache, TileBaseTransferNode node) {
        node.itemRation = (int) cache.get(0);
        node.liquidRation = (int) cache.get(1);
    }
}
