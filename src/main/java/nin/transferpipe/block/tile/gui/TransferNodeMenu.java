package nin.transferpipe.block.tile.gui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.item.Upgrade;
import org.jetbrains.annotations.NotNull;

public abstract class TransferNodeMenu extends BaseMenu {

    private final ContainerData data;

    public static final int upgradesY = 92;
    public int upgradesStart = containerStart;
    public int upgradesEnd = upgradesStart + 5;

    public TransferNodeMenu(IItemHandler upgrades, ContainerData data, MenuType type, int containerId, Inventory inv, ContainerLevelAccess access) {
        super(type, containerId, inv, access);
        this.data = data;
        this.addDataSlots(data);

        for (int ix = 0; ix < upgrades.getSlots(); ++ix) {
            this.addSlot(new Upgrade.Slot(upgrades, ix, 35 + ix * 18, upgradesY));
        }
        containerEnd = upgradesEnd;
    }

    @Override
    public int getOffsetY() {
        return upgradesY;
    }

    @Override
    public Pair<Integer, Integer> getHighPriorityContainerSlots(ItemStack item) {
        return item.getItem() instanceof Upgrade ? Pair.of(upgradesStart, upgradesEnd) : null;
    }

    public boolean isSearching() {
        return data.get(0) == 1;
    }

    public String getSearchPosMsg() {
        return "x: " + data.get(1) + " y: " + data.get(2) + " z: " + data.get(3);
    }


    public static class Item extends TransferNodeMenu {

        //client
        public Item(int containerId, Inventory inv) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv, ContainerLevelAccess.NULL);
        }

        //server
        public Item(IItemHandler slot, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv, ContainerLevelAccess access) {
            super(upgrades, data, TPBlocks.TRANSFER_NODE_ITEM.menu(), containerId, inv, access);
            this.addSlot(new SlotItemHandler(slot, 0, 80, -38 + upgradesY));
            containerEnd++;
        }

        @Override
        public Block getBlock() {
            return TPBlocks.TRANSFER_NODE_ITEM.block();
        }
    }

    public static class Liquid extends TransferNodeMenu {

        private final IItemHandler dummyLiquidItem;

        //client
        public Liquid(int containerId, Inventory inv) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv, ContainerLevelAccess.NULL);
        }

        //server
        public Liquid(IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv, ContainerLevelAccess access) {
            super(upgrades, data, TPBlocks.TRANSFER_NODE_LIQUID.menu(), containerId, inv, access);
            this.dummyLiquidItem = dummyLiquidItem;
            addSlot(new DummyItemSlot(dummyLiquidItem, 0, 114514, 0));
        }

        public static class DummyItemSlot extends SlotItemHandler{

            public DummyItemSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
                super(itemHandler, index, xPosition, yPosition);
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return false;
            }
        }

        public FluidStack getLiquid() {
            var a = dummyLiquidItem.getStackInSlot(0);
            return new FluidHandlerItemStack(a, Integer.MAX_VALUE).getFluid();
        }

        @Override
        public Block getBlock() {
            return TPBlocks.TRANSFER_NODE_LIQUID.block();
        }
    }
}
