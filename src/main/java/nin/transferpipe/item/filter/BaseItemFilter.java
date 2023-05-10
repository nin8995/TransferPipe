package nin.transferpipe.item.filter;

import net.minecraft.world.item.ItemStack;
import nin.transferpipe.item.upgrade.UpgradeItem;

import java.util.function.Predicate;

public abstract class BaseItemFilter extends UpgradeItem {

    public BaseItemFilter(Properties p_41383_) {
        super(p_41383_);
    }

    public abstract Predicate<ItemStack> getFilter(ItemStack filter);
}
