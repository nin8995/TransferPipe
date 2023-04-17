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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TileHolderEntity;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.item.RationingUpgradeItem;
import nin.transferpipe.item.SortingUpgrade;
import nin.transferpipe.item.TPItems;
import nin.transferpipe.item.Upgrade;
import nin.transferpipe.particle.ColorSquare;
import nin.transferpipe.particle.TPParticles;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

//搬送する種類に依らない、「ノード」のタイルエンティティとしての機能
public abstract class TileBaseTransferNode extends TileHolderEntity implements TPItems {

    /**
     * 基本情報
     */

    //タイルのフィールドは変更を知らせないといけないので普通privateにしてsetterを介す
    private BlockState pipeState = TPBlocks.TRANSFER_PIPE.get().defaultBlockState();
    private Search search;
    private int cooltime;
    private boolean firstTick = true;//でもこれは内部的な値なのでsetterなしで済ませてる
    private final ItemStackHandler upgrades;

    public static final String PIPE_STATE = "PipeState";
    public static final String SEARCH = "Search";
    public static String COOLTIME = "Cooltime";
    public static final String FIRST_TICK = "FirstTick";
    public static final String UPGRADES = "Upgrades";
    public final BlockPos POS;
    @Nullable
    public Direction FACING;
    public boolean initialized;
    public boolean isSearching;
    public ContainerData searchData = new ContainerData() {
        public int get(int p_58431_) {
            return switch (p_58431_) {
                case 0 -> isSearching ? 1 : 0;
                case 1 -> search.getCurrentPos().getX();
                case 2 -> search.getCurrentPos().getY();
                case 3 -> search.getCurrentPos().getZ();
                default -> 0;
            };
        }

        public void set(int p_58433_, int i) {
            switch (p_58433_) {
                case 0 -> isSearching = i == 1;
                case 1 -> search.setPos(i, search.getCurrentPos().getY(), search.getCurrentPos().getZ());
                case 2 -> search.setPos(search.getCurrentPos().getX(), i, search.getCurrentPos().getZ());
                case 3 -> search.setPos(search.getCurrentPos().getX(), search.getCurrentPos().getY(), i);
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public TileBaseTransferNode(BlockEntityType<? extends TileBaseTransferNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        POS = this.worldPosition;
        if (getBlockState().getBlock() instanceof BlockTransferNode.FacingNode<?> node)
            FACING = node.facing(getBlockState());

        setSearch(new Search(this));
        upgrades = new Upgrade.Handler(6, this);
    }

    public BlockState getPipeState() {
        return pipeState;
    }

    public void setPipeStateAndUpdate(BlockState state) {
        if (pipeState != state) {
            pipeState = state;
            updateTile(state);
            setChanged();//タイルエンティティ更新時の処理
            level.markAndNotifyBlock(getBlockPos(), level.getChunkAt(getBlockPos()), getBlockState(), getBlockState(), 3, 512);//ブロック更新時の処理
        }
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search ss) {
        search = ss;
        setChanged();
    }

    public ItemStack getUpgrade(int slot) {
        return upgrades.getStackInSlot(slot);
    }

    public void setUpgrade(int slot, ItemStack item) {
        upgrades.setStackInSlot(slot, item);
        setChanged();
    }

    public IItemHandler getUpgrades() {
        return upgrades;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        tag.put(SEARCH, search.write());
        tag.putInt(COOLTIME, cooltime);
        tag.putBoolean(FIRST_TICK, firstTick);
        tag.put(UPGRADES, upgrades.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(PIPE_STATE))
            pipeState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(PIPE_STATE));
        if (tag.contains(SEARCH))
            search = search.read(tag.getCompound(SEARCH));
        if (tag.contains(COOLTIME))
            cooltime = tag.getInt(COOLTIME);
        if (tag.contains(FIRST_TICK))
            firstTick = tag.getBoolean(FIRST_TICK);
        if (tag.contains(UPGRADES))
            upgrades.deserializeNBT(tag.getCompound(UPGRADES));
    }

    //見た目を変化させる変更をクライアントに伝えるためのタグ
    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 一般の機能
     */

    public void beforeTick() {
        //インスタンス生成時はlevelがnullでpipeState分らんからここで
        if (firstTick) {
            setPipeStateAndUpdate(PipeUtils.calcInitialState(level, worldPosition, pipeState));
            firstTick = false;//setChangedは上で呼ばれてる
        }

        if (!initialized) {
            calcUpgrades();
            initialized = true;
        }
    }

    @Override
    public final void tick() {
        beforeTick();

        if (redstoneBehavior == RedstoneBehavior.NEVER
                || (redstoneBehavior == RedstoneBehavior.ACTIVE_HIGH && level.getBestNeighborSignal(POS) == 0)
                || (redstoneBehavior == RedstoneBehavior.ACTIVE_LOW && level.getBestNeighborSignal(POS) > 0))
            return;

        super.tick();

        cooltime -= coolRate;
        for (; cooltime <= 0; cooltime += 20) {

            if (FACING != null)//if(canWork(POS, FACED))あってもいいけどなくてもいい←world interaction考えるとない方が楽
                facing(POS.relative(FACING), FACING.getOpposite());
            else
                Direction.stream().forEach(d -> facing(POS.relative(d), d.getOpposite()));

            isSearching = shouldSearch() && !(level.getBlockEntity(search.getNextPos()) == this && !shouldRenderPipe());
            if (isSearching)
                setSearch(search.proceed());
        }

        afterTick();
    }

    public void afterTick() {
    }

    public float coolRate;
    public boolean stackMode;
    public boolean pseudoRoundRobin;
    public boolean depthFirst;
    public boolean breadthFirst;

    public boolean addParticle;
    public RedstoneBehavior redstoneBehavior;

    public int itemRation;
    public int liquidRation;
    public BiPredicate<List<Item>, Item> sortingFunction;

    public void calcUpgrades() {
        coolRate = 2;
        stackMode = false;
        pseudoRoundRobin = false;
        depthFirst = false;
        breadthFirst = false;

        addParticle = false;
        redstoneBehavior = RedstoneBehavior.ACTIVE_LOW;

        itemRation = Integer.MAX_VALUE;
        liquidRation = Integer.MAX_VALUE;
        sortingFunction = (l, i) -> true;

        IntStream.range(0, upgrades.getSlots()).forEach(slot -> {
            var upgrade = upgrades.getStackInSlot(slot);
            if (upgrade.is(SPEED_UPGRADE.get()))
                coolRate += upgrade.getCount();
            else if (upgrade.is(AMPLIFIED_SPEED_UPGRADE.get()))
                coolRate *= Math.pow(1.01, upgrade.getCount());
            else if (upgrade.is(STACK_UPGRADE.get()))
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
                sortingFunction = sorter.filter;

            else if (upgrade.getItem() instanceof Upgrade.BlockItem bi && bi.getBlock() instanceof TransferPipe pipe)
                setPipeStateAndUpdate(PipeUtils.calcInitialState(level, POS, pipe.defaultBlockState()));
        });
    }

    public enum RedstoneBehavior {
        ACTIVE_LOW,
        ACTIVE_HIGH,
        ALWAYS,
        NEVER
    }

    public abstract boolean shouldSearch();

    public abstract void facing(BlockPos pos, Direction dir);

    public abstract void terminal(BlockPos pos, Direction dir);

    public boolean isNormalSearch() {
        return !(breadthFirst || depthFirst);
    }

    //PipeState(特定のパイプの特定の状況)を表示
    // TODO ノード本体が視界外になるだけでPipeStateが表示されなくなる
    // TODO 重い
    public static class Renderer implements BlockEntityRenderer<TileBaseTransferNode> {

        private final BlockRenderDispatcher blockRenderer;

        public Renderer(BlockEntityRendererProvider.Context p_173623_) {
            this.blockRenderer = p_173623_.getBlockRenderDispatcher();
        }

        @Override
        public void render(TileBaseTransferNode be, float p_112308_, PoseStack pose, MultiBufferSource mbs, int p_112311_, int overlay) {
            var level = be.getLevel();

            if (be.shouldRenderPipe() && level != null)//いつlevelがnullになるの
                TPUtils.renderBlockStateWithoutSeed(be.getPipeState(), level, be.getBlockPos(),
                        blockRenderer, pose, mbs, p_112311_);
        }
    }

    public boolean shouldRenderPipe() {
        return !PipeUtils.centerOnly(pipeState);
    }

    public void addSearchParticle(Vec3 pos) {
        if (addParticle)
            TPParticles.addSearch(level, pos, getParticleOption());
    }

    public void addTerminalParticle(Vec3 pos) {
        if (addParticle)
            TPParticles.addTerminal(level, pos, getParticleOption());
    }

    public abstract ColorSquare.Option getParticleOption();

    public abstract boolean canWork(BlockPos pos, Direction d);

    public boolean canWorkMultipleAtTime() {
        return false;
    }

    public void onSearchEnd() {
    }

    public void onProceedPipe(BlockPos pos) {
    }
}
