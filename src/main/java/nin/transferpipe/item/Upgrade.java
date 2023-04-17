package nin.transferpipe.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.util.HandlerUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;

public interface Upgrade {

    class BlockItem extends net.minecraft.world.item.BlockItem implements Upgrade {

        public BlockItem(net.minecraft.world.level.block.Block p_40565_, Properties p_40566_) {
            super(p_40565_, p_40566_);
        }
    }

    class UItem extends Item implements Upgrade {

        public UItem(Properties p_41383_) {
            super(p_41383_);
        }
    }

    class Function extends UItem {

        public Function(Properties p_41383_) {
            super(p_41383_);
        }
    }

    class Slot extends SlotItemHandler {

        public Slot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return (stack.getItem() instanceof Upgrade || TPUtils.isAnyOf(stack, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.REDSTONE_TORCH, Items.GUNPOWDER)) && super.mayPlace(stack);
        }
    }

    class Handler extends HandlerUtils.TileItem<TileBaseTransferNode> {

        public Handler(int size, TileBaseTransferNode be) {
            super(size, be);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            be.calcUpgrades();
        }
    }
}
