package nin.transferpipe.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.item.Upgrade;

public abstract class TransferNodeMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;

    private static final int upgradesY = 92;
    private final ContainerData data;
    private final int containerStart = 0;
    private int containerEnd = containerStart + 5;
    private final int inventoryStart;
    private final int inventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    protected TransferNodeMenu(Object slot, IItemHandler upgrades, ContainerData data, MenuType type, int containerId, Inventory inv, ContainerLevelAccess access) {
        super(type, containerId);
        this.access = access;
        this.data = data;

        this.addDataSlots(data);

        if (slot instanceof IItemHandler itemSlot) {
            this.addSlot(new SlotItemHandler(itemSlot, 0, 80, -38 + upgradesY));
            containerEnd++;
        }

        for (int ix = 0; ix < upgrades.getSlots(); ++ix) {
            this.addSlot(new Upgrade.Slot(upgrades, ix, 35 + ix * 18, upgradesY));
        }

        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inv, k + l * 9 + 9, 8 + k * 18, l * 18 + 22 + upgradesY));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(inv, i1, 8 + i1 * 18, 80 + upgradesY));
        }

        inventoryStart = containerEnd + 1;
        inventoryEnd = inventoryStart + 26;
        hotbarStart = inventoryEnd + 1;
        hotbarEnd = hotbarStart + 8;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        var quickMovedStack = ItemStack.EMPTY;
        var quickMovedSlot = this.slots.get(quickMovedSlotIndex);
        if (quickMovedSlot.hasItem()) {
            ItemStack rawStack = quickMovedSlot.getItem();
            quickMovedStack = rawStack.copy();

            if (quickMovedSlotIndex <= containerEnd) {
                if (!moveItemTo(rawStack, inventoryStart, hotbarEnd, true))
                    return ItemStack.EMPTY;
            } else if (quickMovedSlotIndex <= hotbarEnd) {
                if (!moveItemTo(rawStack, containerStart, containerEnd, false)) {
                    if (quickMovedSlotIndex <= inventoryEnd) {
                        if (!moveItemTo(rawStack, hotbarStart, hotbarEnd, false))
                            return ItemStack.EMPTY;
                    } else if (!moveItemTo(rawStack, inventoryStart, inventoryEnd, false))
                        return ItemStack.EMPTY;
                }
            }

            if (rawStack.isEmpty())
                quickMovedSlot.set(ItemStack.EMPTY);
            else
                quickMovedSlot.setChanged();

            if (rawStack.getCount() == quickMovedStack.getCount())
                return ItemStack.EMPTY;

            quickMovedSlot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    public boolean moveItemTo(ItemStack item, int minSlot, int maxSlot, boolean fillMaxToMin) {
        return moveItemStackTo(item, minSlot, maxSlot + 1, fillMaxToMin);
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, getBlock());
    }

    public boolean isSearching() {
        return data.get(0) == 1;
    }

    public String getSearchPosMsg() {
        return "x: " + data.get(1) + " y: " + data.get(2) + " z: " + data.get(3);
    }

    public abstract Block getBlock();

    public static class Item extends TransferNodeMenu {

        //client
        protected Item(int containerId, Inventory inv) {
            this(new ItemStackHandler(1), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv, ContainerLevelAccess.NULL);
        }

        //server
        public Item(IItemHandler slot, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv, ContainerLevelAccess access) {
            super(slot, upgrades, data, TPBlocks.TRANSFER_NODE_ITEM.menu(), containerId, inv, access);
        }

        @Override
        public Block getBlock() {
            return TPBlocks.TRANSFER_NODE_ITEM.block();
        }
    }
}
