package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.property.ConnectionStates;
import nin.transferpipe.block.property.FlowStates;
import nin.transferpipe.block.property.TPProperties;

public class TPUtil {

    public static BlockState defaultPipeState() {
        return TransferPipe.TRANSFER_PIPE.get().defaultBlockState();
    }

    public static BlockState getPipeState(Level l, BlockPos bp) {
        var bs = l.getBlockState(bp).hasProperty(TPProperties.FLOW) ? l.getBlockState(bp)
                : l.getBlockEntity(bp) instanceof TransferNodeBlockEntity be ? be.getPipeState()
                : defaultPipeState();
        return setConnections(l, bp, bs);
    }

    public static BlockState setConnections(Level l, BlockPos bp, BlockState bs) {
        for (Direction d : Direction.stream().toList())
            bs = bs.setValue(TPProperties.CONNECTIONS.get(d), isPipe(l, bp, d) && canGo(l, bp, d) ? ConnectionStates.PIPE : canConnect(l, bp, d) && bs.getValue(TPProperties.FLOW) != FlowStates.IGNORE ? ConnectionStates.MACHINE : ConnectionStates.NONE);
        if (l.getBlockState(bp).getBlock() == TransferPipe.TRANSFER_NODE_ITEM.get())
            bs = bs.setValue(TPProperties.CONNECTIONS.get(l.getBlockState(bp).getValue(TransferNodeBlock.FACING)), ConnectionStates.NONE);
        return bs;
    }

    public static BlockState cycleFlow(Level l, BlockPos bp, BlockState bs) {
        return setConnections(l, bp, bs.setValue(TPProperties.FLOW, FlowStates.getNext(l, bp, bs)));
    }


    public static boolean isPipe(LevelAccessor l, BlockPos p, Direction d) {
        var bs = l.getBlockState(p.relative(d));
        var b = bs.getBlock();
        return b == TransferPipe.TRANSFER_PIPE.get()
                || (b == TransferPipe.TRANSFER_NODE_ITEM.get() && bs.getValue(TransferNodeBlock.FACING) != d.getOpposite());
    }

    public static boolean canGo(LevelAccessor l, BlockPos p, Direction d) {
        var me = l.getBlockEntity(p) instanceof TransferNodeBlockEntity be ? be.getPipeState() : l.getBlockState(p);
        var you = l.getBlockEntity(p.relative(d)) instanceof TransferNodeBlockEntity be ? be.getPipeState() : l.getBlockState(p.relative(d));
        return !(isOneWay(me, d) && isOneWay(you, d.getOpposite()));
    }

    public static boolean isOneWay(BlockState bs, Direction d) {
        if (!bs.hasProperty(TPProperties.FLOW))//ここには何故か初見さんきちゃう
            return false;

        var s = bs.getValue(TPProperties.FLOW);
        return !(s == FlowStates.fromDirection(d) || s == FlowStates.ALL || s == FlowStates.IGNORE);
    }

    public static boolean canConnect(LevelAccessor l, BlockPos p, Direction d) {
        return l.getBlockEntity(p.relative(d)) instanceof Container || l.getBlockState(p.relative(d)).getBlock() instanceof WorldlyContainerHolder;
    }
}
