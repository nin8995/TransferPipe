package nin.transferpipe.block.node;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.BaseMenu;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.item.Upgrade;
import org.jetbrains.annotations.NotNull;

public abstract class MenuTransferNode extends BaseMenu {

    public final ContainerData searchData;

    public static final int upgradesY = 91 + 30;
    public int upgradesStart = containerStart;
    public int upgradesEnd = upgradesStart + 5;

    public MenuTransferNode(IItemHandler upgrades, ContainerData searchData, TPBlocks.RegistryGUIEntityBlock registry, int containerId, Inventory inv, ContainerLevelAccess access) {
        super(registry, containerId, inv, access, false);
        this.searchData = searchData;
        this.addDataSlots(searchData);

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
        return searchData.get(0) == 1;
    }

    public BlockPos getSearchPos() {
        return new BlockPos(searchData.get(1), searchData.get(2), searchData.get(3));
    }


    public static class Item extends MenuTransferNode {

        //client
        public Item(int containerId, Inventory inv) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv, ContainerLevelAccess.NULL);
        }

        //server
        public Item(IItemHandler slot, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv, ContainerLevelAccess access) {
            super(upgrades, data, TPBlocks.TRANSFER_NODE_ITEM, containerId, inv, access);
            this.addSlot(new SlotItemHandler(slot, 0, 80, -38 + upgradesY));
            containerEnd++;
        }
    }

    public static class Liquid extends MenuTransferNode {

        private final IItemHandler dummyLiquidItem;

        //client
        public Liquid(int containerId, Inventory inv) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv, ContainerLevelAccess.NULL);
        }

        //server
        public Liquid(IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv, ContainerLevelAccess access) {
            super(upgrades, searchData, TPBlocks.TRANSFER_NODE_LIQUID, containerId, inv, access);
            this.dummyLiquidItem = dummyLiquidItem;
            addSlot(new DummyItemSlot(dummyLiquidItem, 0, 114514, 0));
        }

        public static class DummyItemSlot extends SlotItemHandler {

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
    }

    public static class Energy extends MenuTransferNode {

        private final ContainerData energyNodeData;

        public Energy(int containerId, Inventory inv) {
            this(new SimpleContainerData(5), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv, ContainerLevelAccess.NULL);
        }

        public Energy(ContainerData energyNodeData, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv, ContainerLevelAccess access) {
            super(upgrades, data, TPBlocks.TRANSFER_NODE_ENERGY, containerId, inv, access);
            this.energyNodeData = energyNodeData;
            addDataSlots(energyNodeData);
        }

        public int getEnergy() {
            return energyNodeData.get(0);
        }

        public int getExtractables() {
            return energyNodeData.get(1);
        }

        public int getReceivables() {
            return energyNodeData.get(2);
        }

        public int getBoth() {
            return energyNodeData.get(3);
        }

        public int getEnergyReceiverPipes() {
            return energyNodeData.get(4);
        }
    }
}
