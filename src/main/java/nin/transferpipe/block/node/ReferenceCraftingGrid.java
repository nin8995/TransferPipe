package nin.transferpipe.block.node;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.minecraft.DummyMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Transfer Node (Item) の自動クラフト用の、インベントリを参照するクラフトグリッド
 */
public class ReferenceCraftingGrid extends CraftingContainer {

    public Map<Integer, Pair<IItemHandler, Integer>> inventoryReferences = new HashMap<>();
    public List<BlockPos> itemPositions = new ArrayList<>();
    public TransferNodeItem.Tile node;

    public ReferenceCraftingGrid(TransferNodeItem.Tile referencer) {
        super(new DummyMenu(), 3, 3);
        this.node = referencer;
    }

    public void setItem(int gridNumber, IItemHandler inventory, int inventorySlot, BlockPos pos) {
        setItem(gridNumber, inventory.getStackInSlot(inventorySlot));
        inventoryReferences.put(gridNumber, Pair.of(inventory, inventorySlot));
        itemPositions.add(pos);
    }

    public int getMinCount() {
        return inventoryReferences.values().stream()
                .map(pair -> pair.getFirst().getStackInSlot(pair.getSecond()).getCount())
                .min(Integer::compareTo)
                .orElse(0);
    }

    public void consume(int assembleAmount, List<ItemStack> remainders) {
        inventoryReferences.forEach((gridNumber, p) -> {
            var inventory = p.getFirst();
            var slot = p.getSecond();
            inventory.extractItem(slot, assembleAmount, false);
            var remainder = remainders.get(gridNumber);
            for (int i = inventory.getSlots() - 1; i >= 0; i--)
                remainder = inventory.insertItem(i, remainder, false);
            if (!remainder.isEmpty())
                Containers.dropContents(node.getLevel(), node.getBlockPos(), NonNullList.of(ItemStack.EMPTY, remainder));
        });
    }

    public void clear() {
        IntStream.range(0, getContainerSize()).forEach(i -> setItem(i, ItemStack.EMPTY));
        inventoryReferences.clear();
        itemPositions.clear();
    }

    public boolean hasInvalidInventories() {
        return inventoryReferences.entrySet().stream().anyMatch(e -> {
            var gridNumber = e.getKey();
            var inventory = e.getValue().getFirst();
            return ForgeUtils.getItemHandler(node.getLevel(), node.inventoryPozzes.get(gridNumber), node.facing).map(inv -> inv != inventory).orElse(true);
        });
    }
}
