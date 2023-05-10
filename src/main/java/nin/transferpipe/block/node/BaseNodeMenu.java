package nin.transferpipe.block.node;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.item.upgrade.Upgrade;
import nin.transferpipe.item.upgrade.UpgradeSlot;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import nin.transferpipe.util.minecraft.BaseBlockMenu;
import org.jetbrains.annotations.NotNull;

/**
 * ノードのGUI上同期の基本部分。検索状況、アップグレードスロットの同期。
 */
public abstract class BaseNodeMenu extends BaseBlockMenu {

    public final ContainerData searchData;

    public static final int upgradesY = 91 + 30;
    public int upgradesStart = containerStart;
    public int upgradesEnd = upgradesStart + 5;

    public BaseNodeMenu(IItemHandler upgrades, ContainerData searchData, RegistryGUIEntityBlock<?> registry, int containerId, Inventory inv) {
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


    /**
     * アイテムノードのアイテムスロットの同期
     */
    public static class Item extends BaseNodeMenu {

        public Item(RegistryGUIEntityBlock<?> registry, IItemHandler slot, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(upgrades, searchData, registry, containerId, inv);
            addItemHandler(slot, SlotItemHandler::new, -38 + upgradesY);
        }
    }

    /**
     * 液体ノードの液体スロットの同期
     */
    public static class Liquid extends BaseNodeMenu {

        public LiquidInteractiveSlot liquidSlot;

        //server
        public Liquid(RegistryGUIEntityBlock<?> registry, IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(upgrades, searchData, registry, containerId, inv);
            this.liquidSlot = addItemHandler(dummyLiquidItem, LiquidInteractiveSlot::new, 83).get(0);
        }
    }

    /**
     * エネルギーノードの各種情報と充電スロットの同期
     */
    public static class Energy extends BaseNodeMenu {

        private final ContainerData energyNodeData;

        public Energy(RegistryGUIEntityBlock<?> registry, ContainerData energyNodeData, IItemHandler charge, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
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
