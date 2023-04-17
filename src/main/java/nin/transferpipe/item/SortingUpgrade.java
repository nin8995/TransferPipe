package nin.transferpipe.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import nin.transferpipe.util.TPUtils;

import java.util.List;
import java.util.function.BiPredicate;

public class SortingUpgrade extends Upgrade.Function {

    public final BiPredicate<List<Item>, Item> filter;

    public static final BiPredicate<List<Item>, Item> ITEM_SORTING_FUNCTION = (items, toPush) -> items.stream().allMatch(i ->
            i == toPush || i == Items.AIR);
    public static final BiPredicate<List<Item>, Item> MOD_SORTING_FUNCTION = (items, toPush) -> {
        var modid = BuiltInRegistries.ITEM.getKey(toPush).getNamespace();
        return items.stream().allMatch(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(modid) || i == Items.AIR);
    };
    public static final BiPredicate<List<Item>, Item> TAB_SORTING_FUNCTION = (items, toPush) -> {
        var tab = TPUtils.getFirstlyContainedTab(toPush);
        return items.stream().allMatch(i -> TPUtils.getFirstlyContainedTab(i) == tab || i == Items.AIR);
    };

    public SortingUpgrade(BiPredicate<List<Item>, Item> filter, Properties p_41383_) {
        super(p_41383_);
        this.filter = filter;
    }
}
