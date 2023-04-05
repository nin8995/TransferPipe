package nin.transferpipe.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.TileItemHandler;
import nin.transferpipe.block.TransferNodeBlockEntity;
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

    class ItemHandler extends TileItemHandler<TransferNodeBlockEntity>{

        public ItemHandler(int size, TransferNodeBlockEntity be) {
            super(size, be);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            be.calcUpgrades();
        }
    }
}