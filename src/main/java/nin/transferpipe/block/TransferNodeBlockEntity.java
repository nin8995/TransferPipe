package nin.transferpipe.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.block.status.Search;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    private boolean initialized;//でもこれは内部的な値なのでsetterなしで済ませてる

    public static final String PIPE_STATE = "PipeState";
    public static final String SEARCH = "Search";
    public static String COOLTIME = "Cooltime";
    public static final String INITIALIZED = "Initialized";
    public final Direction FACING;
    public final BlockPos FACING_POS;
    public final Direction FACED;

    public TransferNodeBlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        FACING = getBlockState().getValue(TransferNodeBlock.FACING);
        FACING_POS = worldPosition.relative(FACING);
        FACED = FACING.getOpposite();
        resetSearchStatus();
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

    public Search getSearchStatus() {
        return search;
    }

    public void setSearchStatus(Search ss) {
        search = ss;
        setChanged();
    }

    public void resetSearchStatus() {
        setSearchStatus(new Search(this, this.worldPosition, FACING));
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

    //pipeState(特定のパイプの特定の状況)を表示
    public static class Renderer implements BlockEntityRenderer<TransferNodeBlockEntity> {

        private final BlockRenderDispatcher blockRenderer;

        public Renderer(BlockEntityRendererProvider.Context p_173623_) {
            this.blockRenderer = p_173623_.getBlockRenderDispatcher();
        }

        @Override
        public void render(TransferNodeBlockEntity be, float p_112308_, PoseStack pose, MultiBufferSource mbs, int p_112311_, int p_112312_) {
            var pipeState = be.getPipeState();

            if (be.shouldRenderPipe())
                TPUtils.renderBlockState(pipeState, be.getLevel(), be.getBlockPos(),
                        blockRenderer, pose, mbs.getBuffer(RenderType.cutout()));
        }
    }

    public boolean shouldRenderPipe() {
        return !PipeUtils.centerOnly(pipeState);
    }

    public void tick() {
        //インスタンス生成時はlevelがnullでpipeState分らんからここで
        if (!initialized) {
            this.setPipeStateAndUpdate(PipeUtils.recalcConnections(level, worldPosition));
            initialized = true;
            setChanged();
        }

        decreaseCooltime();

        while (cooltime <= 0) {
            facing(FACING_POS);
            if (shouldSearch())
                search.next();
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

    public abstract void facing(BlockPos workplace);

    public abstract void terminal(BlockPos workplace, Direction dir);

    public static class Item extends TransferNodeBlockEntity implements Container {

        /**
         * 基本情報
         */

        private ItemStack itemSlot = ItemStack.EMPTY;

        public static final String ITEM_SLOT = "ItemSlot";

        public Item(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_ITEM.type(), p_155229_, p_155230_);
        }

        public ItemStack getItemSlot() {
            return itemSlot;
        }

        public void setItemSlot(ItemStack itemSlot) {
            this.itemSlot = itemSlot;
            setChanged();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put(ITEM_SLOT, itemSlot.save(new CompoundTag()));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(ITEM_SLOT))
                itemSlot = ItemStack.of(tag.getCompound(ITEM_SLOT));
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return itemSlot.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? itemSlot : null;
        }

        @Override
        public ItemStack removeItem(int slot, int i) {
            var item = ContainerHelper.removeItem(List.of(itemSlot), slot, i);
            if (!item.isEmpty())
                setChanged();

            return item;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(List.of(itemSlot), slot);
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            if (slot == 0)
                setItemSlot(item);
        }

        @Override
        public boolean stillValid(Player p_18946_) {
            return true;
        }

        @Override
        public void clearContent() {
            setItemSlot(ItemStack.EMPTY);
        }

        /**
         * 機能
         */


        @Override
        public boolean shouldSearch() {
            return !itemSlot.isEmpty();
        }

        @Override
        public void facing(BlockPos workplace) {
            if (PipeUtils.isWorkPlace(level, workplace)) {
                var container = HopperBlockEntity.getContainerAt(level, workplace);
                getSlots(container, FACED).anyMatch(slot -> tryPull(container, slot));
            }
        }

        //この方角から参照できるスロット番号のstream
        public static IntStream getSlots(Container p_59340_, Direction p_59341_) {
            return p_59340_ instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer) p_59340_).getSlotsForFace(p_59341_)) : IntStream.range(0, p_59340_.getContainerSize());
        }

        public boolean tryPull(Container container, int slot) {
            if (canPull(container, slot)) {
                var suckedItem = container.removeItem(slot, getSuckAmount());
                if (!suckedItem.isEmpty()) {

                    if (itemSlot.isEmpty())
                        itemSlot = suckedItem;
                    else
                        itemSlot.setCount(itemSlot.getCount() + suckedItem.getCount());
                    setChanged();

                    return true;
                }
            }

            return false;
        }

        public boolean canPull(Container container, int slot) {
            var item = container.getItem(slot);
            return canReceive(item)
                    && container.canTakeItem(container, slot, item)
                    && !(container instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, item, FACED));
        }

        public boolean canReceive(ItemStack item) {
            return itemSlot.isEmpty() ||
                    (itemSlot.getCount() <= itemSlot.getMaxStackSize() && ItemStack.isSameItemSameTags(itemSlot, item));
        }

        public int getSuckAmount() {
            return Math.min(1, itemSlot.getMaxStackSize() - itemSlot.getCount());
        }

        @Override
        public void terminal(BlockPos workplace, Direction dir) {
            if (PipeUtils.isWorkPlace(level, workplace)) {
                var container = HopperBlockEntity.getContainerAt(level, workplace);
                push(container, dir);
            }
        }

        public void push(Container container, Direction dir) {
            var newSlot = HopperBlockEntity.addItem(this, container, itemSlot, dir);
            if (!newSlot.equals(itemSlot))
                setItemSlot(newSlot);
        }
    }
}
