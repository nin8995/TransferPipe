package nin.transferpipe.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ShapeUtil {

    public static Vec3 rotate(Vec3 v, Direction d) {
        var rightAngle = (float) (Math.PI / 2);
        var toRotate = v.add(-.5, -.5, -.5);
        var rotated = switch (d) {
            case DOWN -> toRotate.xRot(rightAngle);
            case UP -> toRotate.xRot(-rightAngle);
            case NORTH -> toRotate;
            case SOUTH -> toRotate.yRot(rightAngle * 2);
            case WEST -> toRotate.yRot(rightAngle);
            case EAST -> toRotate.yRot(-rightAngle);
        };
        return rotated.add(.5, .5, .5);
    }

    public static VoxelShape rotate(VoxelShape shape, Direction d) {
        List<VoxelShape> shapes = new ArrayList<>();
        for (AABB aabb : shape.toAabbs()) {
            var start = new Vec3(aabb.minX, aabb.minY, aabb.minZ);
            var end = new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ);
            shapes.add(Shapes.create(new AABB(rotate(start, d), rotate(end, d))));
        }
        var united = Shapes.empty();
        for (VoxelShape s : shapes)
            united = Shapes.or(united, s);
        return united;
    }

    public static Map<Direction, VoxelShape> getRotatedShapes(VoxelShape s) {
        return Direction.stream().collect(Collectors.toMap(UnaryOperator.identity(), d -> rotate(s, d)));
    }
}
