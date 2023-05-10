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
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public abstract class BaseTileNodeItem extends BaseTileNode {

    /**
     * 初期化
     */
    public final TileItemSlot<BaseTileNodeItem> itemSlot;

    public BaseTileNodeItem(BlockEntityType<? extends BaseTileNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
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
        var toExtract = itemSlot.isEmpty()
                        ? ForgeUtils.findFirst(inv, itemFilter)
                        : itemSlot.getItem();
        return toExtract != null && canExtract(inv, toExtract, false);
    }

    public boolean canExtract(IItemHandler inv, ItemStack toExtract, boolean byWorldInteraction) {
        var extractionSpeed = getExtractionSpeed(toExtract, byWorldInteraction);

        return IntStream.range(0, inv.getSlots())
                .filter(slot -> MCUtils.same(inv.getStackInSlot(slot), toExtract))
                .anyMatch(slot -> {
                    var item = inv.getStackInSlot(slot);
                    var extraction = Math.min(getExtractableAmount(item, byWorldInteraction), extractionSpeed);
                    return extraction > 0;
                });
    }

    public void tryExtract(IItemHandler inv) {
        var toExtract = itemSlot.isEmpty()
                        ? ForgeUtils.findFirst(inv, itemFilter)
                        : itemSlot.getItem();
        if (toExtract != null)
            tryExtract(inv, toExtract, getExtractionSpeed(toExtract, false), false);
    }

    public int tryExtract(IItemHandler inv, ItemStack toExtract, int remainingExtractionPower, boolean byWorldInteraction) {
        for (int slot : IntStream.range(0, inv.getSlots())
                .filter(slot -> MCUtils.same(inv.getStackInSlot(slot), toExtract))
                .toArray()) {

            var item = inv.getStackInSlot(slot);
            var extraction = Math.min(getExtractableAmount(item, byWorldInteraction), remainingExtractionPower);
            if (extraction <= 0)
                break;
            remainingExtractionPower -= extraction;
            itemSlot.receive(inv.extractItem(slot, extraction, false));
        }
        return remainingExtractionPower;
    }

    public boolean shouldReceive(ItemStack item) {
        return itemSlot.canStack(item) && itemFilter.test(item);
    }

    public int getExtractableAmount(ItemStack toExtract, boolean byWorldInteraction) {
        return Math.min(getExtractionSpeed(toExtract, byWorldInteraction), getReceivableAmount(toExtract, byWorldInteraction));
    }

    public int getExtractionSpeed(ItemStack toExtract, boolean byWorldInteraction) {
        var extractionSpeed = stackMode ? (int) (toExtract.getMaxStackSize() * capacityRate) : 1;
        if (byWorldInteraction)
            extractionSpeed = Math.max(extractionSpeed, wi());

        return extractionSpeed;
    }

    public int getReceivableAmount(ItemStack toReceive, boolean byWorldInteraction) {
        var freeSpace = itemSlot.getFreeSpace();
        if (byWorldInteraction)
            freeSpace = Math.max(freeSpace, itemSlot.getFreeSpace(wi()));

        return Math.min(toReceive.getCount(), freeSpace);
    }

    /**
     * アイテム搬入
     */
    public boolean canInsert(IItemHandler inv) {
        return itemSlot.getItem() != insert(inv, true);
    }

    public void tryInsert(IItemHandler inv) {
        if (canInsert(inv))
            itemSlot.setItem(insert(inv, false));
    }

    public ItemStack insert(IItemHandler inv, boolean simulate) {
        var self = itemSlot.getItem();
        var itemToInsert = getInsertableItem(inv, self);
        if (itemToInsert.isEmpty())
            return self;//failed

        var remainder = ItemHandlerHelper.insertItemStacked(inv, itemToInsert, simulate);
        if (itemToInsert == remainder)
            return self;//failed

        var filteredAmount = self.getCount() - itemToInsert.getCount();
        return self.copyWithCount(filteredAmount + remainder.getCount());//succeeded and return remainder
    }

    public ItemStack getInsertableItem(IItemHandler inv, ItemStack self) {
        if (self.isEmpty())
            return ItemStack.EMPTY;

        //test sort
        if (!sortingFunc.test(ForgeUtils.toItemList(inv), self.getItem()))
            return ItemStack.EMPTY;

        //consider ration
        var ration = itemRation - ForgeUtils.countItem(inv, self);
        return self.copyWithCount(Math.min(ration, self.getCount()));
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
