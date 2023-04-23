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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * その他
     */

    public static <T> T getRandomlyFrom(Collection<T> c, RandomSource rand) {
        if (c.isEmpty())
            return null;

        var list = new ArrayList<>(c);
        return list.get((int) (rand.nextFloat() * list.size()));//0<nextFloat<1のため配列の範囲外エラーは起きない
    }

    public static ResourceLocation modLoc(String id) {
        return new ResourceLocation(TPMod.MODID, id);
    }

    public static String toMilliBucket(int amount) {
        return String.format("%,d", amount) + "mb";
    }

    public static String toFE(int energy) {
        return String.format("%,d", energy) + "FE";
    }

    public static <K, V> void addToSetMap(Map<K, Set<V>> map, K k, @Nullable V v) {
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

    public static <K1, K2, V> void addToMapMap(Map<K1, Map<K2, V>> map, K1 key1, K2 key2, V value) {
        if (map.containsKey(key1))
            map.get(key1).put(key2, value);
        else
            map.put(key1, new HashMap<>(Map.of(key2, value)));
    }

    public static <A, B, C> void removeFromMapMap(Map<A, Map<B, C>> map, Predicate3<A, B, C> shouldRemove) {
        var toRemove = new HashMap<A, Set<B>>();
        map.forEach((a, value) -> value.forEach((b, c) -> {
            if (shouldRemove.test(a, b, c))
                addToSetMap(toRemove, a, b);
        }));
        toRemove.forEach((a, value) -> value.forEach(b -> TPUtils.removeFromMapMap(map, a, b)));
    }

    public interface Predicate3<A, B, C> {

        boolean test(A a, B b, C c);
    }

    public static <K1, K2, V> void removeFromMapMap(Map<K1, Map<K2, V>> map, K1 key1, K2 key2) {
        map.get(key1).remove(key2);
        if (map.get(key1).isEmpty())
            map.remove(key1);
    }

    public static <A, B> void removeFromMap(Map<A, B> map, BiPredicate<A, B> shouldRemove, Consumer<A> terminalFunc) {
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

    public static String POS = "Pos";
    public static String DIRS = "Dirs";

    @SafeVarargs
    public static CompoundTag writePosDirsSetMap(Map<BlockPos, Set<Direction>>... setMaps) {
        var tag = new CompoundTag();
        AtomicInteger i = new AtomicInteger(0);
        for (Map<BlockPos, Set<Direction>> setMap : setMaps)
            putPosDirsFromSetMapTo(setMap, tag, i);

        return tag;
    }

    @SafeVarargs
    public static <T> CompoundTag writePosDirsMapMap(BiConsumer<CompoundTag, T> writer, Map<BlockPos, Map<Direction, T>>... mapMaps) {
        var tag = new CompoundTag();
        AtomicInteger i = new AtomicInteger(0);
        for (Map<BlockPos, Map<Direction, T>> mapMap : mapMaps)
            putPosDirsFromMapMapTo(mapMap, writer, tag, i);

        return tag;
    }

    public static void putPosDirsFromSetMapTo(Map<BlockPos, Set<Direction>> setMap, CompoundTag tag, AtomicInteger i) {
        setMap.forEach((pos, dirs) -> putPosDirsTo(tag, pos, dirs, i));
    }

    public static <T> void putPosDirsFromMapMapTo(Map<BlockPos, Map<Direction, T>> mapMap, BiConsumer<CompoundTag, T> writer, CompoundTag tag, AtomicInteger i) {
        mapMap.forEach((pos, dirsMap) -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            dirsMap.forEach((dir, value) -> {
                var tTag = new CompoundTag();
                writer.accept(tTag, value);
                subTag.put(dir.toString(), tTag);
            });

            tag.put(String.valueOf(i), subTag);
            i.getAndIncrement();
        });
    }

    public static void putPosDirsTo(CompoundTag tag, BlockPos pos, Set<Direction> dirs, AtomicInteger i) {
        var subTag = new CompoundTag();
        subTag.put(POS, NbtUtils.writeBlockPos(pos));
        subTag.putIntArray(DIRS, dirs.stream().map(Enum::ordinal).toList());

        tag.put(String.valueOf(i), subTag);
        i.getAndIncrement();
    }

    public static void readPosDirs(CompoundTag tag, BiConsumer<BlockPos, Set<Direction>> readFunc) {
        tag.getAllKeys().forEach(i -> {
            var entryTag = tag.getCompound(i);
            var pos = NbtUtils.readBlockPos(entryTag.getCompound(POS));
            var dirs = Arrays.stream(entryTag.getIntArray(DIRS)).mapToObj(j -> Direction.values()[j]).collect(Collectors.toSet());
            readFunc.accept(pos, dirs);
        });
    }

    public static <T> void readPosDirsMap(CompoundTag tag, Function<CompoundTag, T> translateFunc, BiConsumer<BlockPos, Map<Direction, T>> readFunc) {
        tag.getAllKeys().forEach(i -> {
            var subTag = tag.getCompound(i);
            var pos = NbtUtils.readBlockPos(subTag.getCompound(POS));
            var dirsMap = new HashMap<Direction, T>();
            Direction.stream().filter(d -> subTag.contains(d.toString())).forEach(d ->
                    dirsMap.put(d, translateFunc.apply(subTag.getCompound(d.toString()))));

            readFunc.accept(pos, dirsMap);
        });
    }

    public static <T> CompoundTag writePosMap(Map<BlockPos, T> map, BiConsumer<CompoundTag, T> writer) {
        var tag = new CompoundTag();
        AtomicInteger i = new AtomicInteger(0);

        map.forEach((pos, t) -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            writer.accept(subTag, t);
            tag.put(i.toString(), subTag);
            i.getAndIncrement();
        });

        return tag;
    }

    public static <T> void readPosMap(CompoundTag tag, Function<CompoundTag, T> translateFunc, BiConsumer<BlockPos, T> readFunc) {
        tag.getAllKeys().forEach(i -> {
            var subTag = tag.getCompound(i);
            var pos = NbtUtils.readBlockPos(subTag.getCompound(POS));
            var t = translateFunc.apply(subTag);
            readFunc.accept(pos, t);
        });
    }

    public static BlockEntity getTile(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileHolderEntity tileHolder ? tileHolder.holdingTile : level.getBlockEntity(pos);
    }

    public static BufferedImage getImage(ResourceLocation loc) {
        return getImage(Minecraft.getInstance().getResourceManager().getResource(loc).get());
    }

    public static BufferedImage getImage(Resource resource) {
        try {
            return ImageIO.read(new ByteArrayInputStream(resource.open().readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int computeInt(ItemStack item, String key, int initialValue) {
        return compute(item.getOrCreateTag(), key, CompoundTag::getInt, CompoundTag::putInt, initialValue);
    }

    public static boolean computeBoolean(ItemStack item, String key) {
        return compute(item.getOrCreateTag(), key, CompoundTag::getBoolean, CompoundTag::putBoolean, false);
    }

    public static CompoundTag computeTag(ItemStack item, String key) {
        return compute(item.getOrCreateTag(), key, CompoundTag::getCompound, CompoundTag::put, new CompoundTag());
    }

    public static <T> T compute(CompoundTag tag, String key, BiFunction<CompoundTag, String, T> getter, Consumer3<CompoundTag, String, T> putter, T initialValue) {
        if (!tag.contains(key))
            putter.accept(tag, key, initialValue);

        return getter.apply(tag, key);
    }

    public interface Consumer3<A, B, C> {

        void accept(A a, B b, C c);
    }

    @Nullable
    public static CreativeModeTab getFirstlyContainedTab(Item checked) {
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
    public static CreativeModeTab getFirstlyContainedTab(Item checked, List<CreativeModeTab> tabs) {
        for (CreativeModeTab tab : tabs)
            if (tab.getDisplayItems().stream().anyMatch(itemStack -> itemStack.is(checked)))
                return tab;
        return null;
    }

    public static boolean isAnyOf(ItemStack item, Item... items) {
        return Arrays.stream(items).anyMatch(item::is);
    }

    public static boolean sameItemSameTagExcept(ItemStack item, ItemStack filteringItem, String tagKey) {
        var noDamageItem = item.copy();
        removeTag(noDamageItem, tagKey);
        var noDamageFilteringItem = filteringItem.copy();
        removeTag(noDamageFilteringItem, tagKey);

        return ItemStack.isSameItemSameTags(noDamageItem, noDamageFilteringItem);
    }

    public static void removeTag(ItemStack item, String key) {
        if (item.hasTag() && item.getTag().contains(key))
            item.getTag().remove(key);
    }

    @Nullable
    public static TagKey<Item> getCommonTag(List<Item> item) {
        return item.stream()
                .map(i -> i.builtInRegistryHolder().tags()).map(Stream::toList)
                .min(Comparator.comparingLong(List::size)).get().stream()
                .filter(tag -> item.stream().allMatch(i -> i.builtInRegistryHolder().is(tag)))
                .findFirst().orElse(null);
    }

    public static List<Item> reduceAir(List<Item> items) {
        return items.stream().filter(i -> i != Items.AIR).toList();
    }

    public static Set<TagKey<Item>> getAvailableTags(List<Item> items) {
        return items.stream()
                .flatMap(i -> i.builtInRegistryHolder().tags())
                .collect(Collectors.toSet());
    }

    public static ItemStack copyWithScale(ItemStack item, int scale) {
        return item.copyWithCount(item.getCount() * scale);
    }

    public static FluidStack copyWithAddition(FluidStack fluid, int addition) {
        return copyWithAmount(fluid, fluid.getAmount() + addition);
    }

    public static FluidStack copyWithAmount(FluidStack fluid, int amount) {
        var copy = fluid.copy();
        copy.setAmount(amount);
        return copy;
    }
}
