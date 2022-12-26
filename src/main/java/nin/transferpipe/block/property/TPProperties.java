package nin.transferpipe.block.property;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class TPProperties {
    public static final EnumProperty<FlowStates> FLOW = EnumProperty.create("flow", FlowStates.class);
    public static final Map<Direction, EnumProperty<ConnectionStates>> CONNECTIONS = Direction.stream().collect(Collectors
            .toMap(UnaryOperator.identity(), d -> EnumProperty.create(d.getName(), ConnectionStates.class)));
}
