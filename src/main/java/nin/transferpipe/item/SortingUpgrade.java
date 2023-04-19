package nin.transferpipe.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import nin.transferpipe.util.JavaUtils;
import nin.transferpipe.util.TPUtils;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class SortingUpgrade extends FunctionUpgrade {

    public final BiPredicate<List<Item>, Item> filter;

    public SortingUpgrade(BiPredicate<List<Item>, Item> filter, Properties p_41383_) {
        super(p_41383_);
        this.filter = filter;
    }

    public static final BiPredicate<List<Item>, Item> ITEM_SORT = exceptForAirs((items, toPush) -> items.stream()
            .anyMatch(i -> i == toPush));
    public static final BiPredicate<List<Item>, Item> MOD_SORT = exceptForAirs(((items, toPush) -> {
        var modid = BuiltInRegistries.ITEM.getKey(toPush).getNamespace();
        return items.stream().anyMatch(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(modid));
    }));
    public static final BiPredicate<List<Item>, Item> CREATIVE_TAB_SORT = exceptForAirs((items, toPush) -> {
        var tab = TPUtils.getFirstlyContainedTab(toPush);
        return items.stream().anyMatch(i -> TPUtils.getFirstlyContainedTab(i) == tab);
    });
    public static final BiPredicate<List<Item>, Item> TAG_SORT = exceptForAirs((items, toPush) -> items.stream()
            .flatMap(i -> i.builtInRegistryHolder().tags()).collect(Collectors.toSet()).stream()
            .anyMatch(tag -> toPush.builtInRegistryHolder().is(tag)));
    public static final BiPredicate<List<Item>, Item> COMMON_TAG_SORT = exceptForAirs((items, toPush) -> {
        var commonTag = TPUtils.getCommonTag(items);
        return commonTag != null && toPush.builtInRegistryHolder().is(commonTag);
    });
    public static final BiPredicate<List<Item>, Item> CLASS_SORT = exceptForAirs((items, toPush) -> items.stream()
            .anyMatch(i -> i.getClass().isAssignableFrom(toPush.getClass())));
    public static final BiPredicate<List<Item>, Item> COMMON_CLASS_SORT = exceptForAirs((items, toPush) -> {
        var objects = items.stream().map(SortingUpgrade::blockWhenBlockItem).toList();
        var commonSuper = JavaUtils.getCommonSuper(objects);
        var commonInterface = JavaUtils.getCommonInterface(objects);
        var commonClass = commonInterface != null
                ? commonInterface.isAssignableFrom(commonSuper) ? commonSuper : commonInterface
                : commonSuper;
        return commonClass.isAssignableFrom(blockWhenBlockItem(toPush).getClass());
    });

    public static BiPredicate<List<Item>, Item> exceptForAirs(BiPredicate<List<Item>, Item> predicate) {
        return (items, toPush) -> {
            var itemsWithoutAir = TPUtils.reduceAir(items);
            if (itemsWithoutAir.isEmpty())
                return true;

            return predicate.test(itemsWithoutAir, toPush);
        };
    }

    public static Object blockWhenBlockItem(Item item) {
        return item instanceof BlockItem bi ? bi.getBlock() : item;
    }
}
