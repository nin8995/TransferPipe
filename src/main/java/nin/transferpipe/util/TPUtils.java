package nin.transferpipe.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.mixin.AtlasAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

    public static void renderBlockStateWithoutSeed(BlockState blockState, Level level, BlockPos pos, BlockRenderDispatcher renderer, PoseStack pose, MultiBufferSource mbs, int nazo) {
        var model = renderer.getBlockModel(blockState);
        model.getRenderTypes(blockState, level.random, ModelData.EMPTY).forEach(renderType ->
                renderer.getModelRenderer().tesselateBlock(level, model, blockState, pos, pose, mbs.getBuffer(renderType),
                        true, level.random, nazo, OverlayTexture.NO_OVERLAY));

    }

    public static void renderLiquid(FluidStack liquid, PoseStack pose, int x, int y, int size) {
        var color = IClientFluidTypeExtensions.of(liquid.getFluid()).getTintColor();

        renderWithColor(color, () -> forStillFluidSprite(liquid, sprite -> blit(sprite, pose, x, y, size)));
    }

    public static void renderWithColor(int argb, Runnable renderer) {
        float a = ARGB32.alpha(argb) / 255F;
        float r = ARGB32.red(argb) / 255F;
        float g = ARGB32.green(argb) / 255F;
        float b = ARGB32.blue(argb) / 255F;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);

        renderer.run();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    public static void forStillFluidSprite(FluidStack fluidStack, Consumer<TextureAtlasSprite> func) {
        var fluid = fluidStack.getFluid();
        var renderProperties = IClientFluidTypeExtensions.of(fluid);
        var fluidStill = renderProperties.getStillTexture(fluidStack);

        var sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(fluidStill);
        if (sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
            func.accept(sprite);
    }

    public static void blit(TextureAtlasSprite sprite, PoseStack pose, int x, int y, int blitSize) {
        var atlas = (AtlasAccessor) Minecraft.getInstance().getModelManager().getAtlas(sprite.atlasLocation());
        var image = sprite.contents().getOriginalImage();

        var textureLoc = sprite.atlasLocation();
        var textureWidth = atlas.getWidth();
        var textureHeight = atlas.getHeight();
        var imageStartX = sprite.getX();
        var imageStartY = sprite.getY();
        var imageSize = Math.min(image.getWidth(), image.getHeight());

        TPUtils.blit(textureLoc, pose, x, y, blitSize, textureWidth, textureHeight, imageStartX, imageStartY, imageSize);
    }

    public static void blit(ResourceLocation texture, PoseStack pose, int x, int y, int blitSize, int textureWidth, int textureHeight, int imageStartX, int imageStartY, int imageSize) {
        var scale = imageSize / blitSize;

        textureWidth /= scale;
        textureHeight /= scale;
        imageStartX /= scale;
        imageStartY /= scale;
        imageSize /= scale;

        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, imageStartX, imageStartY, imageSize, imageSize, textureWidth, textureHeight);
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

    public static String toMilliBucket(int amount) {
        return String.format("%,d", amount) + "mb";
    }

    public static String toFE(int energy) {
        return String.format("%,d", energy) + "FE";
    }
}
