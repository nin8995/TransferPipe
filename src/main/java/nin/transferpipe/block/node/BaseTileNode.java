package nin.transferpipe.block.node;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Function3;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.Pipe;
import nin.transferpipe.item.TPItems;
import nin.transferpipe.item.filter.IItemFilter;
import nin.transferpipe.item.filter.ILiquidFilter;
import nin.transferpipe.item.upgrade.RationingUpgrade;
import nin.transferpipe.item.upgrade.SortingUpgrade;
import nin.transferpipe.item.upgrade.UpgradeBlockItem;
import nin.transferpipe.item.upgrade.UpgradeHandler;
import nin.transferpipe.particle.TPParticles;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.java.UtilSetMap;
import nin.transferpipe.util.minecraft.GUITile;
import nin.transferpipe.util.minecraft.MCUtils;
import nin.transferpipe.util.minecraft.TileHolder;
import nin.transferpipe.util.transferpipe.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ノードの基本部分。アップグレード、毎tick処理、探索、レンダー。
 */
public abstract class BaseTileNode<T> extends TileHolder implements GUITile, Searcher, TPItems {

    /**
     * 初期化処理
     */
    public SearchInstance searchManager;
    public UpgradeHandler upgrades;
    public BlockState pipeState = TPBlocks.TRANSFER_PIPE.get().defaultBlockState();

    public BlockPos pos;
    @Nullable
    public Direction facing;
    public BlockPos facingPos;
    public Function3<Level, BlockPos, Direction, LazyOptional<T>> blockHandlerLOSup;
    public Function<Entity, T> entityHandlerSup;

    public BaseTileNode(BlockEntityType<? extends BaseTileNode> p_155228_, BlockPos p_155229_, BlockState p_155230_,
                        Function3<Level, BlockPos, Direction, LazyOptional<T>> blockHandlerLOSup,
                        Function<Entity, T> entityHandlerSup) {
        super(p_155228_, p_155229_, p_155230_);
        pos = this.worldPosition;
        updateFacing();
        searchManager = new SearchInstance(this);
        upgrades = new UpgradeHandler(6, this);
        this.blockHandlerLOSup = blockHandlerLOSup;
        this.entityHandlerSup = entityHandlerSup;
    }

    @Override
    public void setBlockState(@NotNull BlockState p_155251_) {
        super.setBlockState(p_155251_);
        updateFacing();
    }

    public void updateFacing() {
        if (getBlockState().getBlock() instanceof BaseNodeBlock.Facing<?> node) {
            facing = node.facing(getBlockState());
            facingPos = pos.relative(facing);
            onUpdateFacing();
        }
    }

    public void onUpdateFacing() {
    }

    public void setPipeStateAndUpdate(BlockState state) {
        pipeState = state;
        updateTile(state);
        setChanged();//タイルエンティティ更新時の処理
        level.markAndNotifyBlock(getBlockPos(), level.getChunkAt(getBlockPos()), getBlockState(), getBlockState(), 3, 512);//ブロック更新時の処理
    }

    @Override
    public InteractionResult openMenu(Player player) {
        return openMenu(level, pos, player, this);
    }

    /**
     * アップグレード
     */
    public float coolRate;
    public float capacityRate;
    public float worldInteraction;
    public boolean stackMode;
    public boolean pseudoRoundRobin;
    public boolean depthFirst;
    public boolean breadthFirst;
    public boolean searchMemory;
    public boolean stickingSearch;
    public boolean addParticle;
    public RedstoneBehavior redstoneBehavior;
    public int itemRation;
    public int liquidRation;
    public BiPredicate<List<Item>, Item> sortingFunc;
    public Predicate<ItemStack> itemFilter;
    public Predicate<FluidStack> liquidFilter;

    public void calcUpgrades() {
        coolRate = 2;
        capacityRate = 1;
        worldInteraction = 0;
        stackMode = false;
        pseudoRoundRobin = false;
        depthFirst = false;
        breadthFirst = false;
        searchMemory = false;
        stickingSearch = false;
        addParticle = false;
        redstoneBehavior = RedstoneBehavior.ACTIVE_LOW;
        itemRation = Integer.MAX_VALUE;
        liquidRation = Integer.MAX_VALUE;
        sortingFunc = (is, i) -> true;
        itemFilter = i -> true;
        liquidFilter = l -> true;
        var pipeUpgrade = new AtomicReference<>(TPBlocks.TRANSFER_PIPE.get());

        upgrades.forEachItem(upgrade -> {
            if (upgrade.is(SPEED_UPGRADE.get()))
                coolRate += upgrade.getCount();
            else if (upgrade.is(CAPACITY_UPGRADE.get()))
                capacityRate += upgrade.getCount();
            else if (upgrade.is(WORLD_INTERACTION_UPGRADE.get()))
                worldInteraction += upgrade.getCount();
            else if (upgrade.is(OVERCLOCK_UPGRADE.get())) {
                var multiplier = Math.pow(1.01, upgrade.getCount());
                coolRate *= multiplier;
                capacityRate *= multiplier;
                worldInteraction *= multiplier;
            } else if (upgrade.is(STACK_UPGRADE.get()))
                stackMode = true;
            else if (upgrade.is(PSEUDO_ROUND_ROBIN_UPGRADE.get()))
                pseudoRoundRobin = true;
            else if (upgrade.is(DEPTH_FIRST_SEARCH_UPGRADE.get()))
                depthFirst = true;
            else if (upgrade.is(BREADTH_FIRST_SEARCH_UPGRADE.get()))
                breadthFirst = true;
            else if (upgrade.is(SEARCH_MEMORY_UPGRADE.get()))
                searchMemory = true;
            else if (upgrade.is(STICKING_SEARCH_UPGRADE.get()))
                stickingSearch = true;
            else if (upgrade.is(Items.GLOWSTONE_DUST))
                addParticle = true;
            else if (upgrade.is(Items.REDSTONE))
                redstoneBehavior = RedstoneBehavior.ALWAYS;
            else if (upgrade.is(Items.REDSTONE_TORCH))
                redstoneBehavior = RedstoneBehavior.ACTIVE_HIGH;
            else if (upgrade.is(Items.GUNPOWDER))
                redstoneBehavior = RedstoneBehavior.NEVER;
            else if (upgrade.getItem() instanceof RationingUpgrade rationing) {
                itemRation = rationing.getItemRation(upgrade);
                liquidRation = rationing.getLiquidRation(upgrade);
            } else if (upgrade.getItem() instanceof SortingUpgrade sorter)
                sortingFunc = sorter.sortingFunc;
            else if (upgrade.getItem() instanceof IItemFilter filter)
                itemFilter = filter.getFilter(upgrade);
            else if (upgrade.getItem() instanceof ILiquidFilter filter)
                liquidFilter = filter.getFilter(upgrade);
            if (upgrade.getItem() instanceof UpgradeBlockItem bi && bi.getBlock() instanceof Pipe pipe)
                pipeUpgrade.set(pipe);
        });

        if (pipeUpgrade.get() != pipeState.getBlock()) {
            setPipeStateAndUpdate(PipeInstance.precalcState(level, pos, TPUtils.withFlow(pipeUpgrade.get().defaultBlockState(), pipeState), facing));
        }
    }

    public int wi() {
        return (int) worldInteraction;
    }

    public void tryEntityInteraction(BlockPos pos, Direction boxDir, Function<List<T>, Boolean> func) {
        var boxSize = getBoxSize();
        var boxCenter = MCUtils.relative(pos, boxDir, boxSize / 2);
        tryEntityInteraction(boxSize, boxCenter, func);
    }

    public double getBoxSize() {
        return 1 + 2 * JavaUtils.log(2, worldInteraction);
    }

    public void tryEntityInteraction(double boxSize, Vec3 boxCenter, Function<List<T>, Boolean> func) {
        var box = AABB.ofSize(boxCenter, boxSize, boxSize, boxSize);
        var entities = MCUtils.getMappableMappedEntities(level, box, entityHandlerSup);
        if (func.apply(entities) && addParticle)
            addEdges(boxCenter, (float) boxSize / 2);
    }

    public void tryEntityInteraction(BlockPos pos, Function<List<T>, Boolean> func) {
        tryEntityInteraction(getBoxSize(), pos.getCenter(), func);
    }

    /**
     * 毎tick処理
     */
    public int cooltime;
    public boolean isSearching;

    @Override
    public final void tick() {
        super.tick();
        if (!redstoneBehavior.isActive(level, pos))
            return;

        beforeTick();
        cooltime -= coolRate;
        for (; cooltime <= 0; cooltime += 20) {
            isSearching = shouldSearch() && JavaUtils.extraCondition(getBlockEntity(searchManager.getNextPos()) == this, shouldRenderPipe());
            if (isSearching)
                searchManager.proceed();

            if (facing != null)
                facing(facingPos, facing.getOpposite());
            else
                Direction.stream().forEach(d -> facing(pos.relative(d), d.getOpposite()));
        }
        afterTick();
    }

    public void beforeTick() {
    }

    public void afterTick() {
    }

    public abstract boolean shouldSearch();

    public boolean byWorldInteraction;

    public void facing(BlockPos pos, Direction dir) {
        if (canFacingWork()) {
            var handlerLO = blockHandlerLOSup.apply(level, pos, dir);
            if (handlerLO.isPresent())
                handlerLO.ifPresent(inv -> facingWork(pos, dir, inv));
            else if (worldInteraction > 0) {
                byWorldInteraction = true;
                tryWorldInteraction(pos, dir);
                byWorldInteraction = false;
            }
        }
    }

    public abstract boolean canFacingWork();

    public abstract void facingWork(BlockPos pos, Direction dir, T inv);

    public abstract void tryWorldInteraction(BlockPos pos, Direction dir);

    /**
     * 検索
     */
    @Override
    public BlockPos initialPos() {
        return pos;
    }

    @Override
    public Direction initialNonValidDir() {
        return facing;
    }

    @Override
    public boolean isDest(BlockPos pos, Direction dir, BlockPos relativePos, Direction workDir) {
        return TPUtils.currentConnection(level, pos, dir) == Connection.MACHINE
                && blockHandlerLOSup.apply(level, relativePos, workDir).filter(this::canWork).isPresent();
    }

    public abstract boolean canWork(T inv);

    @Override
    public void onFind(BlockPos pos, Direction dir) {
        blockHandlerLOSup.apply(level, pos, dir).ifPresent(inv -> work(pos, dir, inv));

        if (addParticle)
            addBlockParticle(pos);
    }

    public abstract void work(BlockPos pos, Direction dir, T inv);

    @Override
    public boolean canProceed(BlockPos pos, Direction dir, BlockPos relativePos, Direction workDir) {
        return PipeInstance.canProceed(level, pos, dir, this);
    }

    @Override
    public boolean findToEnd() {
        return !pseudoRoundRobin;
    }

    @Override
    public boolean isFullSearch() {
        return breadthFirst || depthFirst;
    }

    @Override
    public BlockPos pickNext(UtilSetMap<BlockPos, Direction> queue) {
        return breadthFirst ? queue.getFirstKey() : Searcher.super.pickNext(queue);
    }

    @Override
    public boolean useMemory() {
        return searchMemory;
    }

    @Override
    public boolean stickingSearch() {
        return stickingSearch;
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
    public static class Renderer implements BlockEntityRenderer<BaseTileNode> {
        private final BlockRenderDispatcher blockRenderer;

        public Renderer(BlockEntityRendererProvider.Context p_173623_) {
            this.blockRenderer = p_173623_.getBlockRenderDispatcher();
        }

        // TODO ノード本体が視界外になるだけでPipeStateが表示されなくなる
        // ↑ BlockPropertyのdynamicShape trueにしたら治った
        // TODO 重い
        @Override
        public void render(BaseTileNode be, float p_112308_, @NotNull PoseStack pose, @NotNull MultiBufferSource mbs, int p_112311_, int overlay) {
            var level = be.getLevel();

            if (be.shouldRenderPipe() && level != null)//いつlevelがnullになるの
                MCUtils.renderBlockStateWithoutSeed(be.pipeState, level, be.getBlockPos(),
                        blockRenderer, pose, mbs, p_112311_);
        }
    }

    public boolean shouldRenderPipe() {
        return !TPUtils.centerOnly(pipeState);
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
     * 読む必要ない
     */
    public static final String SEARCH_MANAGER = "SearchManager";
    public static final String UPGRADES = "Upgrades";
    public static final String PIPE_STATE = "PipeState";
    public static final String COOLTIME = "Cooltime";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(SEARCH_MANAGER, searchManager.serializeNBT());
        tag.put(UPGRADES, upgrades.serializeNBT());
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        tag.putInt(COOLTIME, cooltime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(SEARCH_MANAGER))
            searchManager.deserializeNBT(tag.getCompound(SEARCH_MANAGER));
        if (tag.contains(UPGRADES))
            upgrades.deserializeNBT(tag.getCompound(UPGRADES));
        if (tag.contains(PIPE_STATE))
            pipeState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(PIPE_STATE));
        if (tag.contains(COOLTIME))
            cooltime = tag.getInt(COOLTIME);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        calcUpgrades();
        searchManager.onLoad(level);
    }

    @Override
    public void onFirstTick() {
        super.onFirstTick();
        setPipeStateAndUpdate(PipeInstance.precalcState(level, pos, pipeState, facing));
        searchManager.reset();
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

    public ContainerData searchData = new ContainerData() {
        public int get(int p_58431_) {
            return switch (p_58431_) {
                case 0 -> isSearching ? 1 : 0;
                case 1 -> searchManager.searchingPos.getX();
                case 2 -> searchManager.searchingPos.getY();
                case 3 -> searchManager.searchingPos.getZ();
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
}
