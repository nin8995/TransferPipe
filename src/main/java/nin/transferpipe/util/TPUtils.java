package nin.transferpipe.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import nin.transferpipe.TransferPipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class TPUtils {

    /**
     * 当たり判定
     */

    public static Map<Direction, VoxelShape> getRotatedShapes(VoxelShape s) {
        return Direction.stream().collect(Collectors.toMap(UnaryOperator.identity(), d -> rotate(s, d)));
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

    /**
     * レンダー
     */

    public static void renderBlockStateWithoutSeed(BlockState blockState, Level level, BlockPos pos, BlockRenderDispatcher renderer, PoseStack pose, MultiBufferSource mbs, int overlay) {
        var model = renderer.getBlockModel(blockState);
        model.getRenderTypes(blockState, RandomSource.create(), ModelData.EMPTY).forEach(renderType ->
                renderer.getModelRenderer().tesselateBlock(level, model, blockState, pos, pose, mbs.getBuffer(renderType),
                        false, RandomSource.create(), 0, overlay, ModelData.EMPTY, renderType));

    }

    /**
     * パーティクル
     */

    public static void addParticle(ServerLevel sl, SimpleParticleType particle, Vec3 pos, Vec3 vel, int speed) {
        sl.players().forEach(sp -> sl.sendParticles(sp, particle, true, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, speed));
    }

    /**
     * その他
     */

    public static <T> T getRandomlyFrom(Collection<T> c, RandomSource rand) {
        if (c.isEmpty())
            return null;

        var list = new ArrayList<>(c);
        return list.get((int) (rand.nextFloat() * list.size()));//0<nextFloat<1のため配列の範囲外エラーは起きない
    }

    public static ResourceLocation modLoc(String id) {
        return new ResourceLocation(TransferPipe.MODID, id);
    }
}
