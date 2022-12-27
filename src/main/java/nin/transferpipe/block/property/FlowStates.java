package nin.transferpipe.block.property;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.util.PipeStateUtil;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FlowStates implements StringRepresentable {
    ALL,
    UP,
    DOWN,
    NORTH,
    EAST,
    SOUTH,
    WEST,
    NONE,
    IGNORE;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public static Stream<FlowStates> stream() {
        return Arrays.stream(FlowStates.values());
    }

    public static FlowStates fromDirection(Direction d) {
        return Arrays.stream(FlowStates.values()).filter(f -> f.name().equals(d.name())).findFirst().get();
    }

    public FlowStates next() {
        return Arrays.stream(FlowStates.values()).filter(fs -> fs.ordinal() == this.ordinal() + 1).findFirst().orElse(ALL);
    }

    public static FlowStates getNext(Level l, BlockPos bp, FlowStates currentFlow) {
        var omitted = l.getBlockState(bp).getBlock() == TransferPipe.TRANSFER_NODE_ITEM.get() ? l.getBlockState(bp).getValue(TransferNodeBlock.FACING) : null;
        var validStates = FlowStates.stream().collect(Collectors.toSet());
        var nonValidStates = Direction.stream()
                .filter(d -> !PipeStateUtil.isPipe(l, bp, d) || d == omitted)
                .map(FlowStates::fromDirection).collect(Collectors.toSet());
        if (nonValidStates.size() == 6) {
            nonValidStates.add(NONE);
        } else if (nonValidStates.size() == 5) {
            nonValidStates = Direction.stream().map(FlowStates::fromDirection).collect(Collectors.toSet());
        }
        validStates.removeAll(nonValidStates);
        var searching = currentFlow.next();
        while (true) {
            if (validStates.contains(searching))
                return searching;
            searching = searching.next();
        }
    }
}
