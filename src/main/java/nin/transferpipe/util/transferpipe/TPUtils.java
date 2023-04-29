package nin.transferpipe.util.transferpipe;

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
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.fluids.FluidStack;
import nin.transferpipe.TPMod;
import nin.transferpipe.block.TileHolderEntity;
import nin.transferpipe.mixin.AtlasAccessor;
import nin.transferpipe.util.java.Consumer3;
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

public interface TPUtils {

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

    static void renderLiquid(FluidStack liquid, PoseStack pose, int x, int y, int size) {
        var color = IClientFluidTypeExtensions.of(liquid.getFluid()).getTintColor();

        renderWithColor(color, () -> forStillFluidSprite(liquid, sprite -> blit(sprite, pose, x, y, size)));
    }

    static void renderWithColor(int argb, Runnable renderer) {
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

    static void forStillFluidSprite(FluidStack fluidStack, Consumer<TextureAtlasSprite> func) {
        var fluid = fluidStack.getFluid();
        var renderProperties = IClientFluidTypeExtensions.of(fluid);
        var fluidStill = renderProperties.getStillTexture(fluidStack);

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

        TPUtils.blit(textureLoc, pose, x, y, blitSize, textureWidth, textureHeight, imageStartX, imageStartY, imageSize);
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
     * その他
     */

    static <T> T getRandomlyFrom(Collection<T> c, RandomSource rand) {
        if (c.isEmpty())
            return null;

        var list = new ArrayList<>(c);
        return list.get((int) (rand.nextFloat() * list.size()));//0<nextFloat<1のため配列の範囲外エラーは起きない
    }

    static ResourceLocation modLoc(String id) {
        return new ResourceLocation(TPMod.MODID, id);
    }

    static String toMilliBucket(int amount) {
        return String.format("%,d", amount) + "mb";
    }

    static String toFE(int energy) {
        return String.format("%,d", energy) + "FE";
    }

    static <K, V> void addToSetMap(Map<K, Set<V>> map, K k, @Nullable V v) {
        if (map.containsKey(k)) {
            if (v != null)
                map.get(k).add(v);
        } else {
            var valueSet = new HashSet<V>();
            if (v != null)
                valueSet.add(v);
            map.put(k, valueSet);
        }
    }

    static <A, B> void removeFromMap(Map<A, B> map, BiPredicate<A, B> shouldRemove, Consumer<A> terminalFunc) {
        var toRemove = new HashSet<A>();
        map.forEach((a, b) -> {
            if (shouldRemove.test(a, b))
                toRemove.add(a);
        });
        toRemove.forEach(pos -> {
            terminalFunc.accept(pos);
            map.remove(pos);
        });
    }

    static BlockEntity getTile(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileHolderEntity tileHolder ? tileHolder.holdingTile : level.getBlockEntity(pos);
    }

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

    static boolean isAnyOf(ItemStack item, Item... items) {
        return Arrays.stream(items).anyMatch(item::is);
    }

    /**
     * Tag
     */
    static boolean sameItemSameTagExcept(ItemStack item, ItemStack filteringItem, String tagKey) {
        var noDamageItem = item.copy();
        removeTag(noDamageItem, tagKey);
        var noDamageFilteringItem = filteringItem.copy();
        removeTag(noDamageFilteringItem, tagKey);

        return ItemStack.isSameItemSameTags(noDamageItem, noDamageFilteringItem);
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

    static List<Item> reduceAir(List<Item> items) {
        return items.stream().filter(i -> i != Items.AIR).toList();
    }

    /**
     * ItemStack
     */
    static ItemStack copyWithSub(ItemStack item, ItemStack sub) {
        return item.copyWithCount(item.getCount() - sub.getCount());
    }

    static ItemStack copyWithSub(ItemStack item, int sub) {
        return item.copyWithCount(item.getCount() - sub);
    }

    static ItemStack copyWithScale(ItemStack item, int scale) {
        return item.copyWithCount(item.getCount() * scale);
    }

    /**
     * FluidStack
     */
    static FluidStack copyWithAddition(FluidStack fluid, int addition) {
        return copyWithAmount(fluid, fluid.getAmount() + addition);
    }

    static FluidStack copyWithAmount(FluidStack fluid, int amount) {
        var copy = fluid.copy();
        copy.setAmount(amount);
        return copy;
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

    static List<ItemStack> scaleItems(List<ItemStack> items, int scale) {
        return items.stream().map(i -> TPUtils.copyWithScale(i, scale)).toList();
    }
}
