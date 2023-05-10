package nin.transferpipe.block.pipe;


import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;
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
            state = state.setValue(Pipe.CONNECTIONS.get(d), dirToConnection.apply(d));

        return state;
    }

    public static void forEach(Consumer<Connection> func) {
        for (Connection c : values())
            func.accept(c);
    }
}
