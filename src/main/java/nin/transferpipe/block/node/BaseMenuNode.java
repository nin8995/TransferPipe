package nin.transferpipe.block.node;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.item.Upgrade;
import nin.transferpipe.item.UpgradeSlot;
import org.jetbrains.annotations.NotNull;

public abstract class BaseMenuNode extends BaseBlockMenu {

    public final ContainerData searchData;

    public static final int upgradesY = 91 + 30;
    public int upgradesStart = containerStart;
    public int upgradesEnd = upgradesStart + 5;

    public BaseMenuNode(IItemHandler upgrades, ContainerData searchData, TPBlocks.RegistryGUIEntityBlock<?> registry, int containerId, Inventory inv) {
        super(registry, containerId, inv, "transfer_node", 225);
        this.searchData = searchData;
        this.addDataSlots(searchData);
        addInventory();
        addItemHandlerSlots(upgrades, UpgradeSlot::new, upgradesY);
    }

    @Override
    public boolean noInventoryText() {
        return true;
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


    public static class Item extends BaseMenuNode {

        public Item(TPBlocks.RegistryGUIEntityBlock<?> registry, IItemHandler slot, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv) {
            super(upgrades, data, registry, containerId, inv);
            addItemHandlerSlots(slot, SlotItemHandler::new, -38 + upgradesY);
        }
    }

    public static class Liquid extends BaseMenuNode {

        private final IItemHandler dummyLiquidItem;

        //server
        public Liquid(TPBlocks.RegistryGUIEntityBlock<?> registry, IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(upgrades, searchData, registry, containerId, inv);
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

    public static class Energy extends BaseMenuNode {

        private final ContainerData energyNodeData;

        public Energy(TPBlocks.RegistryGUIEntityBlock<?> registry, ContainerData energyNodeData, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv) {
            super(upgrades, data, registry, containerId, inv);
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
