package nin.transferpipe.item.filter;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Predicate;

public interface ILiquidFilter {

    Predicate<FluidStack> getFilter(ItemStack filter);
}
