package nin.transferpipe.item;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public abstract class BaseItemFilter extends UpgradeItem {

    public BaseItemFilter(Properties p_41383_) {
        super(p_41383_);
    }

    public abstract Predicate<ItemStack> getFilter(ItemStack filter);
}
