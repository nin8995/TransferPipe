package nin.transferpipe.block.node;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import nin.transferpipe.util.forge.LiquidItemSlot;
import org.jetbrains.annotations.NotNull;

/**
 * Transfer Node (Liquid) のための液体表示スロット
 */
//TODO
public class LiquidInteractiveSlot extends LiquidItemSlot {

    public LiquidInteractiveSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
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
