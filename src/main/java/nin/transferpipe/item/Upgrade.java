package nin.transferpipe.item;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.tile.TileTransferNode;
import nin.transferpipe.util.HandlerUtils;
import org.jetbrains.annotations.NotNull;

public interface Upgrade {

    class BlockItem extends net.minecraft.world.item.BlockItem implements Upgrade {

        public BlockItem(net.minecraft.world.level.block.Block p_40565_, Properties p_40566_) {
            super(p_40565_, p_40566_);
        }
    }

    class Item extends net.minecraft.world.item.Item implements Upgrade {

        public Item(Properties p_41383_) {
            super(p_41383_);
        }
    }

    class Slot extends SlotItemHandler {

        public Slot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof Upgrade && super.mayPlace(stack);
        }
    }

    class Handler extends HandlerUtils.TileItem<TileTransferNode> {

        public Handler(int size, TileTransferNode be) {
            super(size, be);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            be.calcUpgrades();
        }
    }
}
