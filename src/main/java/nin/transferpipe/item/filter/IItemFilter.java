package nin.transferpipe.item.filter;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public interface IItemFilter {

    Predicate<ItemStack> getFilter(ItemStack filter);
}
