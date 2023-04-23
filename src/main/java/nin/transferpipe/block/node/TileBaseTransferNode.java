package nin.transferpipe.block.node;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TileHolderEntity;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.item.*;
import nin.transferpipe.particle.TPParticles;
import nin.transferpipe.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public abstract class TileBaseTransferNode extends TileHolderEntity implements Searcher, TPItems {

    /**
     * 初期化処理
     */
    public final BlockPos POS;
    @Nullable
    public Direction FACING;
    public BlockPos FACING_POS;
    public SearchInstance search;
    public final ItemStackHandler upgrades;
    public BlockState pipeState = TPBlocks.TRANSFER_PIPE.get().defaultBlockState();

    public TileBaseTransferNode(BlockEntityType<? extends TileBaseTransferNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        POS = this.worldPosition;
        updateFacing();
        search = new SearchInstance(this);
        upgrades = new UpgradeHandler(6, this);
    }

    @Override
    public void setBlockState(@NotNull BlockState p_155251_) {
        super.setBlockState(p_155251_);
        updateFacing();
    }

    public void updateFacing() {
        if (getBlockState().getBlock() instanceof BlockTransferNode.FacingNode<?> node) {
            FACING = node.facing(getBlockState());
            FACING_POS = POS.relative(FACING);
            onUpdateFacing();
        }
    }

    public void onUpdateFacing() {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        calcUpgrades();
        search.onLoad(level);
    }

    @Override
    public void onFirstTick() {
        super.onFirstTick();
        setPipeStateAndUpdate(PipeUtils.calcInitialState(level, POS, pipeState));
    }

    public void setPipeStateAndUpdate(BlockState state) {
        pipeState = state;
        updateTile(state);
        setChanged();//タイルエンティティ更新時の処理
        level.markAndNotifyBlock(getBlockPos(), level.getChunkAt(getBlockPos()), getBlockState(), getBlockState(), 3, 512);//ブロック更新時の処理
    }

    /**
     * アップグレード
     */
    public float coolRate;
    public float worldInteraction;
    public boolean stackMode;
    public boolean pseudoRoundRobin;
    public boolean depthFirst;
    public boolean breadthFirst;
    public boolean addParticle;
    public RedstoneBehavior redstoneBehavior;
    public int itemRation;
    public int liquidRation;
    public BiPredicate<List<Item>, Item> sortingFunc;
    public Predicate<ItemStack> filteringFunc;

    public void calcUpgrades() {
        coolRate = 2;
        worldInteraction = 0;
        stackMode = false;
        pseudoRoundRobin = false;
        depthFirst = false;
        breadthFirst = false;
        addParticle = false;
        redstoneBehavior = RedstoneBehavior.ACTIVE_LOW;
        itemRation = Integer.MAX_VALUE;
        liquidRation = Integer.MAX_VALUE;
        sortingFunc = (l, i) -> true;
        filteringFunc = i -> true;

        var pipeRemoved = new AtomicBoolean(pipeState.getBlock() != TPBlocks.TRANSFER_PIPE.get());

        IntStream.range(0, upgrades.getSlots()).forEach(slot -> {
            var upgrade = upgrades.getStackInSlot(slot);
            if (upgrade.is(SPEED_UPGRADE.get()))
                coolRate += upgrade.getCount();
            else if (upgrade.is(WORLD_INTERACTION_UPGRADE.get()))
                worldInteraction += upgrade.getCount();
            else if (upgrade.is(OVERCLOCK_UPGRADE.get())) {
                coolRate *= Math.pow(1.01, upgrade.getCount());
                worldInteraction *= Math.pow(1.01, upgrade.getCount());
            } else if (upgrade.is(STACK_UPGRADE.get()))
                stackMode = true;
            else if (upgrade.is(PSEUDO_ROUND_ROBIN_UPGRADE.get()))
                pseudoRoundRobin = true;
            else if (upgrade.is(DEPTH_FIRST_SEARCH_UPGRADE.get()))
                depthFirst = true;
            else if (upgrade.is(BREADTH_FIRST_SEARCH_UPGRADE.get()))
                breadthFirst = true;
            else if (upgrade.is(Items.GLOWSTONE_DUST))
                addParticle = true;
            else if (upgrade.is(Items.REDSTONE))
                redstoneBehavior = RedstoneBehavior.ALWAYS;
            else if (upgrade.is(Items.REDSTONE_TORCH))
                redstoneBehavior = RedstoneBehavior.ACTIVE_HIGH;
            else if (upgrade.is(Items.GUNPOWDER))
                redstoneBehavior = RedstoneBehavior.NEVER;
            else if (upgrade.getItem() instanceof RationingUpgradeItem rationing) {
                itemRation = rationing.getItemRation(upgrade);
                liquidRation = rationing.getLiquidRation(upgrade);
            } else if (upgrade.getItem() instanceof SortingUpgrade sorter)
                sortingFunc = sorter.filter;
            else if (upgrade.getItem() instanceof FilterItem filter)
                filteringFunc = filter.getFilter(upgrade);

            else if (upgrade.getItem() instanceof UpgradeBlockItem bi && bi.getBlock() instanceof TransferPipe pipe) {
                if (pipe != pipeState.getBlock())
                    setPipeStateAndUpdate(PipeUtils.calcInitialState(level, POS, pipe.defaultBlockState()));
                else
                    pipeRemoved.set(false);
            }
        });

        if (pipeRemoved.get())
            setPipeStateAndUpdate(PipeUtils.calcInitialState(level, POS, TPBlocks.TRANSFER_PIPE.get().defaultBlockState()));
    }

    /**
     * 毎tick処理
     */
    public int cooltime;
    public boolean isSearching;
    public ContainerData searchData = new ContainerData() {
        public int get(int p_58431_) {
            return switch (p_58431_) {
                case 0 -> isSearching ? 1 : 0;
                case 1 -> search.searchingPos.getX();
                case 2 -> search.searchingPos.getY();
                case 3 -> search.searchingPos.getZ();
                default -> 0;
            };
        }

        public void set(int p_58433_, int i) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    @Override
    public final void tick() {
        super.tick();
        if (redstoneBehavior == RedstoneBehavior.NEVER
                || (redstoneBehavior == RedstoneBehavior.ACTIVE_HIGH && level.getBestNeighborSignal(POS) == 0)
                || (redstoneBehavior == RedstoneBehavior.ACTIVE_LOW && level.getBestNeighborSignal(POS) > 0))
            return;

        bodyTick();
    }

    public void bodyTick() {
        cooltime -= coolRate;
        for (; cooltime <= 0; cooltime += 20) {
            isSearching = shouldSearch() && !(level.getBlockEntity(search.getNextPos()) == this && !shouldRenderPipe());
            if (isSearching)
                search.proceed();

            if (FACING != null)
                facing(FACING_POS, FACING.getOpposite());
            else
                Direction.stream().forEach(d -> facing(POS.relative(d), d.getOpposite()));
        }
    }

    public abstract boolean shouldSearch();

    public abstract void facing(BlockPos pos, Direction dir);

    /**
     * 検索
     */
    @Override
    public BlockPos initialPos() {
        return POS;
    }

    @Override
    public Direction initialNonValidDir() {
        return FACING;
    }

    @Override
    public boolean isDest(BlockPos pos, Direction dir, BlockPos relativePos, Direction workDir) {
        return PipeUtils.currentConnection(level, pos, dir) == Connection.MACHINE
                && canWork(relativePos, workDir);
    }

    public abstract boolean canWork(BlockPos pos, Direction d);

    @Override
    public void onFind(BlockPos pos, Direction dir) {
        work(pos, dir);
        if (addParticle)
            addBlockParticle(pos);
    }

    public abstract void work(BlockPos pos, Direction dir);

    @Override
    public boolean canProceed(BlockPos pos, Direction dir, BlockPos relativePos, Direction workDir) {
        return PipeUtils.canProceedPipe(level, pos, dir, this);
    }

    @Override
    public boolean findToEnd() {
        return !pseudoRoundRobin;
    }

    public boolean isFullSearch() {
        return breadthFirst || depthFirst;
    }

    @Override
    public BlockPos pickNext(OrderedSetMap<BlockPos, Direction> queue) {
        return breadthFirst ? queue.getFirstKey() : Searcher.super.pickNext(queue);
    }

    @Override
    public void onSearchProceed(BlockPos pos) {
        setChanged();
        if (addParticle)
            addPipeParticle(pos);
    }

    /**
     * PipeStateの表示
     */
    public static class Renderer implements BlockEntityRenderer<TileBaseTransferNode> {
        private final BlockRenderDispatcher blockRenderer;

        public Renderer(BlockEntityRendererProvider.Context p_173623_) {
            this.blockRenderer = p_173623_.getBlockRenderDispatcher();
        }

        // TODO ノード本体が視界外になるだけでPipeStateが表示されなくなる
        // TODO 重い
        @Override
        public void render(TileBaseTransferNode be, float p_112308_, @NotNull PoseStack pose, @NotNull MultiBufferSource mbs, int p_112311_, int overlay) {
            var level = be.getLevel();

            if (be.shouldRenderPipe() && level != null)//いつlevelがnullになるの
                TPUtils.renderBlockStateWithoutSeed(be.pipeState, level, be.getBlockPos(),
                        blockRenderer, pose, mbs, p_112311_);
        }
    }

    public boolean shouldRenderPipe() {
        return !PipeUtils.centerOnly(pipeState);
    }

    /**
     * パーティクル
     */
    public void addPipeParticle(BlockPos pos) {
        TPParticles.addPipeCorners(level, pos.getCenter(), getColor());
    }

    public static float gold = (float) ((1 + Math.sqrt(5)) / 2);

    public void addBlockParticle(BlockPos pos) {
        var option = TPParticles.defaultOption(getColor());
        option.size *= gold;
        TPParticles.addBlockCorners(level, pos.getCenter(), option);
    }

    public void addEdges(Vec3 pos, float boxRadius) {
        var option = TPParticles.defaultOption(getColor());
        option.size *= gold;
        TPParticles.addBoxEdges(level, pos, boxRadius, 2, option);
    }

    public abstract Vector3f getColor();

    /**
     * NBT
     */
    public static final String SEARCH = "Search";
    public static final String UPGRADES = "Upgrades";
    public static final String PIPE_STATE = "PipeState";
    public static final String COOLTIME = "Cooltime";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(SEARCH, search.serializeNBT());
        tag.put(UPGRADES, upgrades.serializeNBT());
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        tag.putInt(COOLTIME, cooltime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(SEARCH))
            search.deserializeNBT(tag.getCompound(SEARCH));
        if (tag.contains(UPGRADES))
            upgrades.deserializeNBT(tag.getCompound(UPGRADES));
        if (tag.contains(PIPE_STATE))
            pipeState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(PIPE_STATE));
        if (tag.contains(COOLTIME))
            cooltime = tag.getInt(COOLTIME);
    }

    //見た目を変化させる変更をクライアントに伝えるためのタグ
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
