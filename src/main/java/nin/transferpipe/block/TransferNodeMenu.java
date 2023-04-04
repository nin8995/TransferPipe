package nin.transferpipe.block;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.item.Upgrade;

import java.util.stream.IntStream;

public abstract class TransferNodeMenu extends QuickMoveMenu {

    private final ContainerLevelAccess access;

    private static final int upgradesY = 92;
    private final ContainerData data;
    private int upgradesStart;
    private int upgradesEnd;

    protected TransferNodeMenu(Object slot, IItemHandler upgrades, ContainerData data, MenuType type, int containerId, Inventory inv, ContainerLevelAccess access) {
        super(type, containerId);
        this.access = access;
        this.data = data;

        this.addDataSlots(data);

        containerStart = 0;
        containerEnd = 5;
        upgradesStart = containerStart;
        upgradesEnd = containerEnd;
        if (slot instanceof IItemHandler itemSlot) {
            this.addSlot(new SlotItemHandler(itemSlot, 0, 80, -38 + upgradesY));
            containerEnd++;
            upgradesStart++;
            upgradesEnd++;
        }
        for (int ix = 0; ix < upgrades.getSlots(); ++ix) {
            this.addSlot(new Upgrade.Slot(upgrades, ix, 35 + ix * 18, upgradesY));
        }

        inventoryStart = containerEnd + 1;
        inventoryEnd = inventoryStart + 26;
        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inv, k + l * 9 + 9, 8 + k * 18, l * 18 + 22 + upgradesY));
            }
        }

        hotbarStart = inventoryEnd + 1;
        hotbarEnd = hotbarStart + 8;
        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(inv, i1, 8 + i1 * 18, 80 + upgradesY));
        }
    }

    @Override
    public Pair<Integer, Integer> getHighPriorityContainerSlots(ItemStack item) {
        return item.getItem() instanceof Upgrade ? Pair.of(containerEnd == 6 ? containerStart + 1 : containerStart, containerEnd) : null;
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
