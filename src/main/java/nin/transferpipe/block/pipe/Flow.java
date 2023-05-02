package nin.transferpipe.block.pipe;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import nin.transferpipe.util.java.JavaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Flow implements StringRepresentable {

    ALL,//全方向に通す
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST,
    BLOCK,//全方向に通さない(入ったっ切り)
    IGNORE;//全方向通すが、機械は無視

    @Override
    public @NotNull String getSerializedName() {
        return this.name().toLowerCase();
    }

    public static Stream<Flow> stream() {
        return Arrays.stream(Flow.values());
    }

    public static Flow fromDir(Direction d) {
        return Flow.values()[d.ordinal() + 1];
    }

    @Nullable
    public Direction toDir() {
        var i = ordinal() - 1;
        return 0 <= i && i < Direction.values().length
               ? Direction.values()[i]
               : null;
    }

    public boolean canFlow(Flow to, Direction dir) {
        return this.openTo(dir) || to.openTo(dir.getOpposite());
    }

    public boolean openTo(Direction dir) {
        return this == Flow.fromDir(dir) || this == Flow.ALL || this == Flow.IGNORE;
    }

    public static Set<Flow> directionalFlows() {
        return JavaUtils.map(Direction.stream(), Flow::fromDir);
    }

    public static Set<Flow> nonDirectionalFlows() {
        var flows = stream().collect(Collectors.toSet());
        flows.removeAll(directionalFlows());
        return flows;
    }

}
