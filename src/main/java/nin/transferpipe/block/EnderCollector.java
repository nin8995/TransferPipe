package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.item.filter.IItemFilter;
import nin.transferpipe.network.CurveParticlePacket;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.RegistryEntityBlock;
import nin.transferpipe.util.forge.TileItemSlot;
import nin.transferpipe.util.java.DiscreteFloat;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.java.UtilSetMap;
import nin.transferpipe.util.minecraft.BaseTile;
import nin.transferpipe.util.minecraft.ITickingEntityBlock;
import nin.transferpipe.util.minecraft.LightingBlock;
import nin.transferpipe.util.minecraft.MCUtils;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EnderCollector extends LightingBlock implements ITickingEntityBlock<EnderCollector.Tile> {

    public static DirectionProperty FACING = BlockStateProperties.FACING;

    public EnderCollector() {
        super(BlockBehaviour.Properties.of(Material.STONE));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public RegistryEntityBlock<Tile> registry() {
        return TPBlocks.ENDER_COLLECTOR;
    }

    public static Map<Direction, VoxelShape> shapes = MCUtils.getRotatedShapes(Stream.of(
            Block.box(10, 6, 10, 15, 10, 14),
            Block.box(4, 4, 0, 12, 12, 4),
            Block.box(5, 5, 4, 11, 11, 6),
            Block.box(6, 6, 6, 10, 10, 16),
            Block.box(4, 12, 0, 12, 15, 2),
            Block.box(6, 10, 10, 10, 15, 14),
            Block.box(1, 4, 0, 4, 12, 2),
            Block.box(1, 6, 10, 6, 10, 14),
            Block.box(4, 1, 0, 12, 4, 2),
            Block.box(6, 1, 10, 10, 6, 14),
            Block.box(12, 4, 0, 15, 12, 2)
    ).reduce(Shapes::or).get());

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return shapes.get(p_60555_.getValue(FACING));
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        var item = player.getItemInHand(hand);
        var tile = getOuterTile(level, pos);

        if (item.getItem() instanceof IItemFilter filter && !tile.filterSlot.hasItem()) {
            tile.setFilter(item.copyWithCount(1), filter.getFilter(item));
            item.shrink(1);
        } else if (TPUtils.isWrench(item) && tile.filterSlot.hasItem())
            tile.filterSlot.drop();
        else
            tile.adjustRange(player.isCrouching());

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static class Tile extends BaseTile {

        /**
         * 準備
         */
        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.ENDER_COLLECTOR.tile(), p_155229_, p_155230_);
            filterSlot = new TileItemSlot<>(this);
            calcAABB();
        }

        public DiscreteFloat range = new DiscreteFloat(0.5F, 0.5F, 15);
        public AABB aabb;

        public void adjustRange(boolean shift) {
            range.proceed(shift);
            calcAABB();
            setChanged();
        }

        public void calcAABB() {
            var size = 1 + range.value() * 2;
            aabb = AABB.ofSize(worldPosition.getCenter(), size, size, size);
        }

        public TileItemSlot<Tile> filterSlot;
        public Predicate<ItemStack> filteringFunc = i -> true;

        public void setFilter(ItemStack filterItem, Predicate<ItemStack> filteringFunc) {
            filterSlot.setItem(filterItem);
            this.filteringFunc = filteringFunc;
            setChanged();
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        /**
         * テレポートアイテムコレクター
         */
        public boolean canTeleport(ItemEntity drop) {
            return aabb.contains(drop.position()) && filteringFunc.test(drop.getItem());
        }

        public void teleport(ItemEntity drop) {
            ForgeUtils.forItemHandler(level, worldPosition.relative(facing()), facing().getOpposite(), inv -> {
                var remainder = ItemHandlerHelper.insertItemStacked(inv, drop.getItem(), false);
                drop.setItem(remainder);
                if (level instanceof ServerLevel sl)
                    new CurveParticlePacket().init(drop, MCUtils.getDirectionalCenter(worldPosition, facing().getOpposite()), Vec3.atLowerCornerOf(facing().getOpposite().getNormal()))
                            .toClients(sl.players());
            });
        }

        /**
         * NBT
         */
        public static String RANGE = "Range";
        public static String FILTER_SLOT = "FilterItem";

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put(RANGE, range.serializeNBT());
            tag.put(FILTER_SLOT, filterSlot.serializeNBT());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(RANGE)) {
                range.deserializeNBT(tag.getCompound(RANGE));
                calcAABB();
            }
            if (tag.contains(FILTER_SLOT)) {
                filterSlot.deserializeNBT(tag.getCompound(FILTER_SLOT));
                if (filterSlot.getItem().getItem() instanceof IItemFilter filter)
                    filteringFunc = filter.getFilter(filterSlot.getItem());
            }
        }

        /**
         * ドロップが出てきた時点で吸う
         */
        @Override
        public void onLoad() {
            super.onLoad();
            enderCollectors.addValue(level, this);
        }

        @Override
        public void onRemove() {
            super.onRemove();
            enderCollectors.removeValue(level, this);
        }
    }

    public static final UtilSetMap<Level, Tile> enderCollectors = new UtilSetMap<>();

    @SubscribeEvent
    public static void teleportDropItemOnJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof ItemEntity drop)
            JavaUtils.forEachRandomly(
                    enderCollectors.get(e.getLevel()).stream().filter(ec -> ec.canTeleport(drop)).toList(),
                    e.getLevel().random,
                    tile -> drop.isRemoved(),
                    tile -> tile.teleport(drop));
    }

    static {
        MinecraftForge.EVENT_BUS.register(EnderCollector.class);
    }
}
