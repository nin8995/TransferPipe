package nin.transferpipe.util.minecraft;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.mixin.AtlasAccessor;
import nin.transferpipe.util.java.Consumer3;
import nin.transferpipe.util.java.ExceptionPredicate;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface MCUtils {

    /**
     * VoxelShape
     */
    static Map<Direction, VoxelShape> getRotatedShapes(VoxelShape s) {
        return Direction.stream().collect(Collectors.toMap(UnaryOperator.identity(), d -> rotate(s, d)));
    }

    static VoxelShape rotate(VoxelShape shape, Direction d) {
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

    static Vec3 rotate(Vec3 v, Direction d) {
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
     * Render
     */
    static void renderBlockStateWithoutSeed(BlockState blockState, Level level, BlockPos pos, BlockRenderDispatcher renderer, PoseStack pose, MultiBufferSource mbs, int nazo) {
        var model = renderer.getBlockModel(blockState);
        model.getRenderTypes(blockState, level.random, ModelData.EMPTY).forEach(renderType ->
                renderer.getModelRenderer().tesselateBlock(level, model, blockState, pos, pose, mbs.getBuffer(renderType),
                        true, level.random, nazo, OverlayTexture.NO_OVERLAY));

    }

    static void renderLiquid(Fluid fluid, PoseStack pose, int x, int y, int size) {
        var color = IClientFluidTypeExtensions.of(fluid).getTintColor();

        renderWithColor(color, () -> forStillFluidSprite(fluid, sprite -> blit(sprite, pose, x, y, size)));
    }

    static void renderWithColor(int argb, Runnable renderer) {
        float a = FastColor.ARGB32.alpha(argb) / 255F;
        float r = FastColor.ARGB32.red(argb) / 255F;
        float g = FastColor.ARGB32.green(argb) / 255F;
        float b = FastColor.ARGB32.blue(argb) / 255F;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);

        renderer.run();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    static void forStillFluidSprite(Fluid fluid, Consumer<TextureAtlasSprite> func) {
        var renderProperties = IClientFluidTypeExtensions.of(fluid);
        var fluidStill = renderProperties.getStillTexture();

        var sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(fluidStill);
        if (sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
            func.accept(sprite);
    }

    static void blit(TextureAtlasSprite sprite, PoseStack pose, int x, int y, int blitSize) {
        var atlas = (AtlasAccessor) Minecraft.getInstance().getModelManager().getAtlas(sprite.atlasLocation());
        var image = sprite.contents().getOriginalImage();

        var textureLoc = sprite.atlasLocation();
        var textureWidth = atlas.getWidth();
        var textureHeight = atlas.getHeight();
        var imageStartX = sprite.getX();
        var imageStartY = sprite.getY();
        var imageSize = Math.min(image.getWidth(), image.getHeight());

        blit(textureLoc, pose, x, y, blitSize, textureWidth, textureHeight, imageStartX, imageStartY, imageSize);
    }

    static void blit(ResourceLocation texture, PoseStack pose, int x, int y, int blitSize, int textureWidth, int textureHeight, int imageStartX, int imageStartY, int imageSize) {
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
     * Resource
     */
    static BufferedImage getImage(ResourceLocation loc) {
        return getImage(Minecraft.getInstance().getResourceManager().getResource(loc).get());
    }

    static BufferedImage getImage(Resource resource) {
        try {
            return ImageIO.read(new ByteArrayInputStream(resource.open().readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Direction
     */
    static Vec3 getDirectionalCenter(BlockPos pos, Direction dir) {
        return pos.getCenter().relative(dir, 0.5);
    }

    static Vec3 toVector(Direction dir, double length) {
        return Vec3.atLowerCornerOf(dir.getNormal()).scale(length);
    }

    static Vec3 relative(BlockPos pos, Direction dir, double length) {
        return getDirectionalCenter(pos, dir.getOpposite()).add(toVector(dir, length));
    }

    static Quaternionf rotation(Direction dir) {
        var q = new Quaternionf();
        var pi = (float) Math.PI;
        return switch (dir) {
            case DOWN -> q.rotationX(-pi / 2);
            case UP -> q.rotationX(pi / 2);
            case NORTH -> q;
            case WEST -> q.rotationY(pi / 2);
            case SOUTH -> q.rotationY(pi);
            case EAST -> q.rotationY(-pi / 2);
        };
    }

    static BlockPos toPos(Vector3f v) {
        return BlockPos.containing(v.x, v.y, v.z);
    }

    static <V> Map<Direction, V> dirMap(Function<Direction, V> mapper) {
        return Direction.stream().collect(Collectors.toMap(d -> d, mapper));
    }

    Set<Direction> horizontals = Direction.stream().filter(d -> !(d == Direction.UP || d == Direction.DOWN)).collect(Collectors.toSet());

    static Stream<Direction> horizontalDirectionsExcept(Direction exception) {
        return horizontals.stream().filter(d -> d != exception);
    }

    /**
     * Creative Tab
     */
    @Nullable
    static CreativeModeTab getFirstlyContainedTab(Item checked) {
        //modタブがあればそこだけ重点的に検索
        var modid = BuiltInRegistries.ITEM.getKey(checked).getNamespace();
        var modTab = getFirstlyContainedTab(checked, CreativeModeTabs.allTabs().stream()
                .filter(tab -> CreativeModeTabRegistry.getName(tab).getNamespace().equals(modid))
                .toList());
        if (modTab != null)
            return modTab;

        //無ければ全タブ検索
        return getFirstlyContainedTab(checked, CreativeModeTabs.allTabs());
    }

    @Nullable
    static CreativeModeTab getFirstlyContainedTab(Item checked, List<CreativeModeTab> tabs) {
        for (CreativeModeTab tab : tabs)
            if (tab.getDisplayItems().stream().anyMatch(itemStack -> itemStack.is(checked)))
                return tab;
        return null;
    }

    /**
     * Tag
     */
    static boolean sameExcept(ItemStack a, ItemStack b, String tagKey) {
        return same(copyWithRmv(a, tagKey), copyWithRmv(b, tagKey));
    }

    static void removeTag(ItemStack item, String key) {
        if (item.hasTag() && item.getTag().contains(key))
            item.getTag().remove(key);
    }

    @Nullable
    static TagKey<Item> getCommonTag(List<Item> item) {
        return item.stream()
                .map(i -> i.builtInRegistryHolder().tags()).map(Stream::toList)
                .min(Comparator.comparingLong(List::size)).get().stream()
                .filter(tag -> item.stream().allMatch(i -> i.builtInRegistryHolder().is(tag)))
                .findFirst().orElse(null);
    }

    /**
     * ItemStack
     */
    static ItemStack copyWithSub(ItemStack item, ItemStack sub) {
        return copyWithSub(item, sub.getCount());
    }

    static ItemStack copyWithSub(ItemStack item, int sub) {
        return item.copyWithCount(item.getCount() - sub);
    }

    static ItemStack copyWithAdd(ItemStack item, ItemStack add) {
        return copyWithAdd(item, add.getCount());
    }

    static ItemStack copyWithAdd(ItemStack item, int add) {
        return item.copyWithCount(item.getCount() + add);
    }

    static ItemStack copyWithScale(ItemStack item, int scale) {
        return item.copyWithCount(item.getCount() * scale);
    }

    static List<ItemStack> scaleItems(List<ItemStack> items, int scale) {
        return items.stream().map(i -> copyWithScale(i, scale)).toList();
    }

    static List<Item> reduceAir(List<Item> items) {
        return items.stream().filter(i -> i != Items.AIR).toList();
    }

    static boolean isAnyOf(ItemStack item, Item... items) {
        return Arrays.stream(items).anyMatch(item::is);
    }

    static int computeInt(ItemStack item, String key, int initialValue) {
        return compute(item.getOrCreateTag(), key, CompoundTag::getInt, CompoundTag::putInt, initialValue);
    }

    static boolean computeBoolean(ItemStack item, String key) {
        return compute(item.getOrCreateTag(), key, CompoundTag::getBoolean, CompoundTag::putBoolean, false);
    }

    static CompoundTag computeTag(ItemStack item, String key) {
        return compute(item.getOrCreateTag(), key, CompoundTag::getCompound, CompoundTag::put, new CompoundTag());
    }

    static <T> T compute(CompoundTag tag, String key, BiFunction<CompoundTag, String, T> getter, Consumer3<CompoundTag, String, T> putter, T initialValue) {
        if (!tag.contains(key))
            putter.accept(tag, key, initialValue);

        return getter.apply(tag, key);
    }

    static boolean same(ItemStack a, ItemStack b) {
        return (a.isEmpty() && b.isEmpty()) || ItemHandlerHelper.canItemStacksStack(a, b);
    }

    static ItemStack copyWithRmv(ItemStack item, String tagKey) {
        var copy = item.copy();
        removeTag(copy, tagKey);
        return copy;
    }

    /**
     * その他
     */
    static <T> T getRandomlyFrom(Collection<T> c, RandomSource rand) {
        if (c.isEmpty())
            return null;

        var list = new ArrayList<>(c);
        return list.get((int) (rand.nextFloat() * list.size()));//0<nextFloat<1のため配列の範囲外エラーは起きない
    }

    static List<Entity> getEntities(Level level, AABB box, Predicate<Entity> filter) {
        return level.getEntities(new EntityTypeTest<>() {
            @Override
            public Entity tryCast(Entity p_156918_) {
                return p_156918_;
            }

            @Override
            public Class<? extends Entity> getBaseClass() {
                return Entity.class;
            }
        }, box, filter);
    }

    static <T> List<T> getMappableMappedEntities(Level level, AABB box, Function<Entity, T> throwableMapper) {
        return getEntities(level, box, ExceptionPredicate.succeeded(throwableMapper::apply))
                .stream().map(throwableMapper).toList();
    }

    static Vec3 toVec3(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    static Vec3 relativeLocation(BlockHitResult hit, BlockPos pos) {
        return hit.getLocation().subtract(MCUtils.toVec3(pos));
    }

    static boolean contains(VoxelShape shape, Vec3 point) {
        return shape.toAabbs().stream().anyMatch(aabb -> contains(aabb, point));
    }

    /**
     * AABB#containsは上に開いてて使えん
     */
    static boolean contains(AABB aabb, Vec3 point) {
        return aabb.minX <= point.x && point.x <= aabb.maxX
                && aabb.minY <= point.y && point.y <= aabb.maxY
                && aabb.minZ <= point.z && point.z <= aabb.maxZ;
    }
}
