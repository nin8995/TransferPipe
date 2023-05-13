package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.TileItemSlot;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.java.OptionalStream;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * アイテムノードのアイテム搬入出部分
 */
public abstract class BaseTileNodeItem extends BaseTileNode<IItemHandler> {

    /**
     * 初期化
     */
    public final TileItemSlot<BaseTileNodeItem> itemSlot;

    public BaseTileNodeItem(BlockEntityType<? extends BaseTileNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_, ForgeUtils::getItemHandler, ForgeUtils::getItemHandler);
        itemSlot = new TileItemSlot<>(this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.ITEM_HANDLER, itemSlot, cap, side);
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        itemSlot.capacityRate = capacityRate;
    }

    /**
     * アイテム搬出
     */
    public boolean canExtract(IItemHandler inv) {
        return toExtract(inv).isPresent();
    }

    public boolean tryExtract(IItemHandler inv) {
        return JavaUtils.isPresentAndThen(toExtract(inv), it -> extract(inv, it, getExtractionPower(it)));
    }

    public boolean canExtract(List<IItemHandler> invs) {
        return toExtract(invs).isPresent();
    }

    public boolean tryExtract(List<IItemHandler> invs) {
        return JavaUtils.isPresentAndThen(toExtract(invs), item -> extract(invs, item, getExtractionPower(item)));
    }

    public boolean shouldReceive(ItemStack item) {
        return itemSlot.canStack(item) && itemFilter.test(item);
    }

    public int getExtractableCount(ItemStack item) {
        return Math.min(getExtractionPower(item), getReceivableCount(item));
    }

    public int getExtractionPower(ItemStack item) {
        if (!shouldReceive(item))
            return 0;

        var extractionPower = stackMode ? (int) (item.getMaxStackSize() * capacityRate) : 1;
        if (byWorldInteraction)
            extractionPower = Math.max(extractionPower, wi());

        return extractionPower;
    }

    public int getReceivableCount(ItemStack item) {
        return Math.min(getFreeSpace(item), item.getCount());
    }

    public int getFreeSpace(ItemStack item) {
        if (!shouldReceive(item))
            return 0;

        var maxStackSize = (int) (item.getMaxStackSize() * capacityRate);
        if (byWorldInteraction)
            maxStackSize = Math.max(maxStackSize, wi());

        return itemSlot.getFreeSpace(maxStackSize);
    }

    private Optional<ItemStack> toExtract(IItemHandler inv) {
        return Optional.ofNullable(
                itemSlot.hasItem()
                ? canExtract(inv, itemSlot.getItem())
                  ? itemSlot.getItem()
                  : null
                : ForgeUtils.findFirstItem(inv, this::shouldReceive));
    }

    private boolean canExtract(IItemHandler inv, ItemStack item) {
        return getExtractableCount(item) > 0 && ForgeUtils.contains(inv, item);
    }

    private int extract(IItemHandler inv, ItemStack item, int extractionPower) {
        return JavaUtils.decrementRecursion(
                ForgeUtils.containingSlots(inv, item),
                extractionPower,
                (slot, i) -> Math.min(getExtractableCount(inv.getStackInSlot(slot)), i),
                (slot, i) -> itemSlot.insert(inv.extractItem(slot, i, false)));
    }

    private Optional<ItemStack> toExtract(List<IItemHandler> invs) {
        return OptionalStream.of(invs, this::toExtract).findFirst();
    }

    private int extract(List<IItemHandler> invs, ItemStack item, int extractionPower) {
        return JavaUtils.recursion(
                invs,
                extractionPower,
                (inv, i) -> extract(inv, item, i));
    }

    /**
     * アイテム搬入
     */
    public boolean canInsert(IItemHandler inv) {
        return toInsert(inv).isPresent();
    }

    public boolean tryInsert(IItemHandler inv) {
        return JavaUtils.isPresentAndThen(toInsert(inv), item -> insert(inv, item));
    }

    public boolean tryInsert(List<IItemHandler> invs) {
        invs = JavaUtils.filter(invs, this::canInsert);
        if (!invs.isEmpty()) {

            JavaUtils.forEach(invs, itemSlot::isEmpty, this::tryInsert);
            return true;
        }
        return false;
    }

    private void insert(IItemHandler inv, ItemStack item) {
        if (!MCUtils.same(item, itemSlot.getItem()) || item.getCount() > itemSlot.getCount())
            return;

        var remainder = ItemHandlerHelper.insertItemStacked(inv, item, false);
        itemSlot.extract(MCUtils.copyWithSub(item, remainder));
    }

    private Optional<ItemStack> toInsert(IItemHandler inv) {
        var self = itemSlot.getItem();
        if (self.isEmpty())
            return Optional.empty();

        //sort
        if (!sortingFunc.test(ForgeUtils.toItemList(inv), self.getItem()))
            return Optional.empty();

        //ration
        var ration = itemRation - ForgeUtils.countItem(inv, self);
        if (ration <= 0)
            return Optional.empty();

        //insertable amount
        var toInsert = self.copyWithCount(Math.min(ration, self.getCount()));
        var remainder = ItemHandlerHelper.insertItemStacked(inv, toInsert, true);
        toInsert = MCUtils.copyWithSub(toInsert, remainder);
        if (toInsert.isEmpty())
            return Optional.empty();

        return Optional.of(toInsert);
    }

    /**
     * NBT
     */
    public static final String ITEM_SLOT = "ItemSlot";

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
}
