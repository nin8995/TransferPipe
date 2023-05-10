package nin.transferpipe.block.node;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.gui.LiquidInteractiveSlot;
import nin.transferpipe.item.Upgrade;
import nin.transferpipe.item.UpgradeSlot;
import nin.transferpipe.util.forge.ForgeUtils;
import org.jetbrains.annotations.NotNull;

public abstract class BaseNodeMenu extends BaseBlockMenu {

    public final ContainerData searchData;

    public static final int upgradesY = 91 + 30;
    public int upgradesStart = containerStart;
    public int upgradesEnd = upgradesStart + 5;

    public BaseNodeMenu(IItemHandler upgrades, ContainerData searchData, TPBlocks.RegistryGUIEntityBlock<?> registry, int containerId, Inventory inv) {
        super(registry, containerId, inv, "transfer_node", 225);
        this.searchData = searchData;
        this.addDataSlots(searchData);
        addInventory();
        addItemHandler(upgrades, UpgradeSlot::new, upgradesY);
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


    public static class Item extends BaseNodeMenu {

        public Item(TPBlocks.RegistryGUIEntityBlock<?> registry, IItemHandler slot, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(upgrades, searchData, registry, containerId, inv);
            addItemHandler(slot, SlotItemHandler::new, -38 + upgradesY);
        }
    }

    public static class Liquid extends BaseNodeMenu {

        public LiquidInteractiveSlot liquidSlot;

        //server
        public Liquid(TPBlocks.RegistryGUIEntityBlock<?> registry, IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(upgrades, searchData, registry, containerId, inv);
            this.liquidSlot = addItemHandler(dummyLiquidItem, LiquidInteractiveSlot::new, 83).get(0);
        }
    }

    public static class Energy extends BaseNodeMenu {

        private final ContainerData energyNodeData;

        public Energy(TPBlocks.RegistryGUIEntityBlock<?> registry, ContainerData energyNodeData, IItemHandler charge, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(upgrades, searchData, registry, containerId, inv);
            this.energyNodeData = energyNodeData;
            addDataSlots(energyNodeData);
            addItemHandler(charge, ChargeSlot::new, -38 + upgradesY);
        }

        @Override
        public Pair<Integer, Integer> getHighPriorityContainerSlots(ItemStack item) {
            return ForgeUtils.hasEnergyStorage(item) ? Pair.of(containerEnd, containerEnd) : super.getHighPriorityContainerSlots(item);
        }

        public static class ChargeSlot extends SlotItemHandler {

            public ChargeSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
                super(itemHandler, index, xPosition, yPosition);
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return super.mayPlace(stack) && ForgeUtils.hasEnergyStorage(stack);
            }
        }

        public int getEnergy() {
            return energyNodeData.get(0);
        }

        public int getExtract() {
            return energyNodeData.get(1);
        }

        public int getInsert() {
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
