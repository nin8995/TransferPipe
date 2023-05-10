package nin.transferpipe.util.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.util.minecraft.SwapRestricted;
import org.jetbrains.annotations.NotNull;

public class LiquidItemSlot extends SlotItemHandler implements SwapRestricted {

    public LiquidItemSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    public FluidStack getLiquid() {
        return ForgeUtils.getFluid(getItem());
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(ForgeUtils.hasFluid(stack) ? ForgeUtils.getFluidItem(stack) : stack.copy());//コピーせずに置いたら不具、てかItemStackは参照なんだからコピーしないとってことか
    }
}
