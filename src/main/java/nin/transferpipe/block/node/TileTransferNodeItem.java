package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.particle.ColorSquare;
import nin.transferpipe.util.ContainerUtils;
import nin.transferpipe.util.HandlerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class TileTransferNodeItem extends TileBaseTransferNode {

    /**
     * 基本情報
     */

    private final ItemStackHandler itemSlot;

    public static final String ITEM_SLOT = "ItemSlot";

    public TileTransferNodeItem(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ITEM.tile(), p_155229_, p_155230_);
        itemSlot = new HandlerUtils.TileItem<>(1, this);
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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, LazyOptional.of(() -> itemSlot));
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
    public void facing(BlockPos pos, Direction dir) {
        if (getItemSlot().getCount() <= getItemSlot().getMaxStackSize())
            if (HandlerUtils.hasItemHandler(level, pos, dir))
                HandlerUtils.forItemHandler(level, pos, dir, this::tryPull);
            else if (ContainerUtils.hasContainer(level, pos))
                ContainerUtils.forContainer(level, pos, dir, this::tryPull);
    }

    public void tryPull(IItemHandler handler) {
        forFirstPullableSlot(handler, slot ->
                receive(handler.extractItem(slot, getPullAmount(handler.getStackInSlot(slot)), false)));
    }

    public void forFirstPullableSlot(IItemHandler handler, IntConsumer func) {
        IntStream.range(0, handler.getSlots())
                .filter(slot -> shouldPull(handler.getStackInSlot(slot)))
                .findFirst().ifPresent(func);
    }

    public void tryPull(Container container, Direction dir) {
        forFirstPullableSlot(container, dir, slot ->
                receive(container.removeItem(slot, getPullAmount(container.getItem(slot)))));
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

    public int getPullAmount(ItemStack in) {
        var pullableAmount = stackMode ? in.getMaxStackSize() : 1;
        var receivableAmount = getItemSlot().isEmpty() ? getItemSlot().getMaxStackSize() : getItemSlot().getMaxStackSize() - getItemSlot().getCount();
        return Math.min(pullableAmount, receivableAmount);
    }

    public void receive(ItemStack item) {
        if (!getItemSlot().isEmpty())
            item.setCount(item.getCount() + getItemSlot().getCount());

        setItemSlot(item);
    }

    @Override
    public void terminal(BlockPos pos, Direction dir) {
        if (HandlerUtils.hasItemHandler(level, pos, dir))
            HandlerUtils.forItemHandler(level, pos, dir, this::tryPush);
        else if (ContainerUtils.hasContainer(level, pos))
            ContainerUtils.forContainer(level, pos, dir, this::tryPush);
    }

    public void tryPush(IItemHandler handler) {
        if (canInsert(handler))
            setItemSlot(insert(handler, false));
    }

    public boolean canInsert(IItemHandler handler) {
        return getItemSlot() != insert(handler, true);
    }

    public ItemStack insert(IItemHandler handler, boolean simulate) {
        var remainder = getItemSlot();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++)
            remainder = handler.insertItem(slot, remainder, simulate);

        return remainder;
    }

    public void tryPush(Container container, Direction dir) {
        if (canInsert(container, dir))
            setItemSlot(insert(container, dir, false));
    }

    public boolean canInsert(Container container, Direction dir) {
        return getItemSlot() != insert(container, dir, true);
    }

    public ItemStack insert(Container container, Direction dir, boolean simulate) {
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
        if (HandlerUtils.hasItemHandler(level, pos, d))
            HandlerUtils.forItemHandler(level, pos, d, handler -> canInsert.set(canInsert(handler)));
        else if (ContainerUtils.hasContainer(level, pos))
            ContainerUtils.forContainer(level, pos, d, (container, dir) -> canInsert.set(canInsert(container, dir)));
        return canInsert.get();
    }

    @Override
    public ColorSquare.Option getParticleOption() {
        return new ColorSquare.Option(1, 0, 0, 1);
    }
}
