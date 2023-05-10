package nin.transferpipe.item;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Predicate;

public abstract class BaseLiquidFilter extends UpgradeItem {

    public BaseLiquidFilter(Properties p_41383_) {
        super(p_41383_);
    }

    public abstract Predicate<FluidStack> getFilter(ItemStack filter);
}
