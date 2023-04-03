package nin.transferpipe.block;

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
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.status.Search;
import nin.transferpipe.util.CapabilityUtils;
import nin.transferpipe.util.ContainerUtils;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

//搬送する種類に依らない、「ノード」のタイルエンティティとしての機能
public abstract class TransferNodeBlockEntity extends BlockEntity {

    /**
     * 基本情報
     */

    //タイルのフィールドは変更を知らせないといけないので普通privateにしてsetterを介す
    private BlockState pipeState = PipeUtils.defaultState();
    private Search search;
    private int cooltime;
    private boolean initialized;//でもこれと
    private boolean searchInvalidated;//これは内部的な値なのでsetterなしで済ませてる
    private final ItemStackHandler upgrades;

    public static final String PIPE_STATE = "PipeState";
    public static final String SEARCH = "Search";
    public static String COOLTIME = "Cooltime";
    public static final String INITIALIZED = "Initialized";
    public static final String SEARCH_INVALIDATED = "SearchInvalidated";
    public static final String UPGRADES = "Upgrades";
    public final BlockPos POS;
    public final Direction FACING;
    public final BlockPos FACING_POS;
    public final Direction FACED;
    private boolean isSearching;
    public ContainerData data = new ContainerData() {
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

    public TransferNodeBlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        POS = this.worldPosition;
        FACING = getBlockState().getValue(TransferNodeBlock.FACING);
        FACING_POS = worldPosition.relative(FACING);
        FACED = FACING.getOpposite();
        setSearch(new Search(this).reset());
        upgrades = new ItemStackHandler(6);
    }

    /**
     * フィールドの取り扱い
     */

    public BlockState getPipeState() {
        return pipeState;
    }

    public void setPipeStateAndUpdate(BlockState state) {
        if (pipeState != state) {
            pipeState = state;
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

    /**
     * NBT変換
     */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        tag.put(SEARCH, search.write());
        tag.putInt(COOLTIME, cooltime);
        tag.putBoolean(INITIALIZED, initialized);
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
        if (tag.contains(INITIALIZED))
            initialized = tag.getBoolean(INITIALIZED);
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

    //PipeState(特定のパイプの特定の状況)を表示
    // TODO ノード本体が視界外になるだけでPipeStateが表示されなくなる
    // TODO 重い
    public static class Renderer implements BlockEntityRenderer<TransferNodeBlockEntity> {

        private final BlockRenderDispatcher blockRenderer;

        public Renderer(BlockEntityRendererProvider.Context p_173623_) {
            this.blockRenderer = p_173623_.getBlockRenderDispatcher();
        }

        @Override
        public void render(TransferNodeBlockEntity be, float p_112308_, PoseStack pose, MultiBufferSource mbs, int p_112311_, int overlay) {
            var pipeState = be.getPipeState();

            if (be.shouldRenderPipe())
                TPUtils.renderBlockStateWithoutSeed(pipeState, be.getLevel(), be.getBlockPos(),
                        blockRenderer, pose, mbs, overlay);
        }
    }

    public boolean shouldRenderPipe() {
        return !PipeUtils.centerOnly(pipeState);
    }

    public void tick() {
        //インスタンス生成時はlevelがnullでpipeState分らんからここで
        if (!initialized) {
            this.setPipeStateAndUpdate(PipeUtils.recalcConnections(level, worldPosition));
            initialized = true;//setChangedは上で呼ばれてる
        }

        isSearching = shouldSearch() && !(level.getBlockEntity(search.getNextPos()) == this && !shouldRenderPipe());
        decreaseCooltime();

        while (cooltime <= 0) {
            //if(canWork(POS, FACED))あってもいいけどなくてもいい
            facing();
            if (isSearching)
                setSearch(search.proceed());
            cooltime += 10;
        }
    }

    public void decreaseCooltime() {
        cooltime--;
    }

    /**
     * 搬送種毎の機能
     */

    public abstract boolean shouldSearch();

    public abstract void facing();

    public abstract void terminal(BlockPos workplace, Direction dir);

    public abstract boolean canWork(BlockPos pos, Direction d);

    public static class Item extends TransferNodeBlockEntity {

        /**
         * 基本情報
         */

        private final ItemStackHandler itemSlot;

        public static final String ITEM_SLOT = "ItemSlot";

        public Item(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_ITEM.entity(), p_155229_, p_155230_);
            itemSlot = new ItemStackHandler();
        }

        public IItemHandler getItemSlotHandler() {
            return itemSlot;
        }

        public ItemStack getItemSlot() {
            return itemSlot.getStackInSlot(0);
        }

        public void setItemSlot(ItemStack item) {
            itemSlot.setStackInSlot(0, item);
            setChanged();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put(ITEM_SLOT, itemSlot.serializeNBT());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(ITEM_SLOT))
                itemSlot.deserializeNBT(tag.getCompound(ITEM_SLOT));
        }

        /**
         * 機能
         */


        @Override
        public boolean shouldSearch() {
            return !itemSlot.getStackInSlot(0).isEmpty();
        }

        @Override
        public void facing() {
            if (getItemSlot().getCount() <= getItemSlot().getMaxStackSize())
                if (CapabilityUtils.hasItemHandler(level, FACING_POS, FACED))
                    CapabilityUtils.forItemHandler(level, FACING_POS, FACED, this::tryPull);
                else if (ContainerUtils.hasContainer(level, FACING_POS))
                    ContainerUtils.forContainer(level, FACING_POS, FACED, this::tryPull);
        }

        public void tryPull(IItemHandler handler) {
            forFirstPullableSlot(handler, slot ->
                    receive(handler.extractItem(slot, getPullAmount(), false)));
        }

        public void forFirstPullableSlot(IItemHandler handler, IntConsumer func) {
            IntStream.range(0, handler.getSlots())
                    .filter(slot -> shouldPull(handler.getStackInSlot(slot)))
                    .findFirst().ifPresent(func);
        }

        public void tryPull(Container container, Direction dir) {
            forFirstPullableSlot(container, dir, slot ->
                    receive(container.removeItem(slot, getPullAmount())));
        }

        public void forFirstPullableSlot(Container container, Direction dir, IntConsumer func) {
            getSlots(container, dir)
                    .filter(slot -> !(container instanceof WorldlyContainer wc && !wc.canTakeItemThroughFace(slot, wc.getItem(slot), dir)))
                    .filter(slot -> shouldPull(container.getItem(slot)))
                    .findFirst().ifPresent(func);
        }

        //この方角から参照できるスロット番号のstream
        public static IntStream getSlots(Container container, Direction dir) {
            return container instanceof WorldlyContainer wc ? IntStream.of(wc.getSlotsForFace(dir)) : IntStream.range(0, container.getContainerSize());
        }

        public boolean shouldPull(ItemStack item) {
            return shouldAdd(item, getItemSlot());
        }

        public static boolean shouldAdd(ItemStack toAdd, ItemStack toBeAdded) {
            if (toAdd.isEmpty())
                return false;
            if (toBeAdded.isEmpty())
                return true;

            return ItemStack.isSameItemSameTags(toBeAdded, toAdd) && toAdd.getCount() + toBeAdded.getCount() <= toBeAdded.getMaxStackSize();
        }

        public int getPullAmount() {
            return Math.min(1, getItemSlot().getMaxStackSize() - getItemSlot().getCount());
        }

        public void receive(ItemStack item) {
            if (!getItemSlot().isEmpty())
                item.setCount(item.getCount() + getItemSlot().getCount());

            setItemSlot(item);
        }

        @Override
        public void terminal(BlockPos pos, Direction dir) {
            if (CapabilityUtils.hasItemHandler(level, pos, dir))
                CapabilityUtils.forItemHandler(level, pos, dir, this::tryPush);
            else if (ContainerUtils.hasContainer(level, pos))
                ContainerUtils.forContainer(level, pos, dir, this::tryPush);
        }

        public void tryPush(IItemHandler handler) {
            if (canInsert(handler))
                setItemSlot(insertTo(handler, false));
        }

        public boolean canInsert(IItemHandler handler) {
            return getItemSlot() != insertTo(handler, true);
        }

        public ItemStack insertTo(IItemHandler handler, boolean simulate) {
            var remainder = getItemSlot();
            for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++)
                remainder = handler.insertItem(slot, remainder, simulate);

            return remainder;
        }

        public void tryPush(Container container, Direction dir) {
            if (canInsert(container, dir))
                setItemSlot(insertTo(container, dir, false));
        }

        public boolean canInsert(Container container, Direction dir) {
            return getItemSlot() != insertTo(container, dir, true);
        }

        public ItemStack insertTo(Container container, Direction dir, boolean simulate) {
            var remainder = getItemSlot().copy();

            for (int slot : getSlots(container, dir)
                    .filter(slot -> container.canPlaceItem(slot, remainder)
                            && !(container instanceof WorldlyContainer wc && !wc.canPlaceItemThroughFace(slot, remainder, dir)))
                    .toArray()) {
                var item = container.getItem(slot).copy();

                if (shouldAdd(remainder, item)) {
                    int addableAmount = item.isEmpty() ? remainder.getMaxStackSize() : item.getMaxStackSize() - item.getCount();
                    int addedAmount = Math.min(addableAmount, remainder.getCount());

                    if (item.isEmpty()) {
                        var ageea = remainder.copy();
                        ageea.setCount(addedAmount);
                        item = ageea;
                    } else {
                        item.setCount(item.getCount() + addedAmount);
                    }

                    if (!simulate)
                        container.setItem(slot, item);

                    remainder.setCount(remainder.getCount() - addedAmount);
                }

                if (remainder.isEmpty())
                    break;
            }

            //同じならcopy前のインスタンスを返す（IItemHandler.insertItemと同じ仕様。ItemStack.equalsが多田野参照評価なため、同値性を求める文脈で渡しておいて同一性と同値性を一致させておくが吉）
            return remainder.getCount() == getItemSlot().getCount() ? getItemSlot() : remainder;
        }

        @Override
        public boolean canWork(BlockPos pos, Direction d) {
            var canInsert = new AtomicBoolean(false);
            if (CapabilityUtils.hasItemHandler(level, pos, d))
                CapabilityUtils.forItemHandler(level, pos, d, handler -> canInsert.set(canInsert(handler)));
            else if (ContainerUtils.hasContainer(level, pos))
                ContainerUtils.forContainer(level, pos, d, (container, dir) -> canInsert.set(canInsert(container, dir)));
            return canInsert.get();
        }
    }
}
