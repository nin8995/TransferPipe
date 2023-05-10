package nin.transferpipe.item.upgrade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.NotNull;

public class UpgradeSlot extends SlotItemHandler {

    public UpgradeSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return super.mayPlace(stack)
                && (stack.getItem() instanceof Upgrade || MCUtils.isAnyOf(stack, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.REDSTONE_TORCH, Items.GUNPOWDER));
    }
}
