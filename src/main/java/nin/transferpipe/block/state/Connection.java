package nin.transferpipe.block.state;


import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.block.TransferPipeBlock;

import java.util.function.Function;

public enum Connection implements StringRepresentable {
    NONE,
    PIPE,
    MACHINE;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public static BlockState map(BlockState state, Function<Direction, Connection> dirToConnection) {
        for (Direction d : Direction.values())
            state = state.setValue(TransferPipeBlock.CONNECTIONS.get(d), dirToConnection.apply(d));

        return state;
    }
}
