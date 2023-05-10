package nin.transferpipe.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.pipe.SortingPipe;
import nin.transferpipe.gui.BaseItemMenu;
import nin.transferpipe.gui.BaseScreen;
import nin.transferpipe.gui.ItemFilterPattern;
import nin.transferpipe.gui.PatternMenu;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.ObscuredInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SortingFilter extends BaseItemFilter implements GUIItem {

    public SortingFilter(Properties p_41383_) {
        super(p_41383_);
    }

    /**
     * @0 -> Sorting Pipe/Upgrade
     * @1 ~ 9 -> sortingFuncに渡すList
     */
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ObscuredInventory(10);
    }


    public IItemHandler patterns(ItemStack filter) {
        return filter.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
    }

    @Nullable
    public static BiPredicate<List<Item>, Item> getSortingFunc(ItemStack item) {
        return item.getItem() instanceof SortingUpgrade sorter
               ? sorter.sortingFunc
               : item.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SortingPipe sorter
                 ? sorter.sortingFunc
                 : null;
    }

    @Override
    public Predicate<ItemStack> getFilter(ItemStack filter) {
        return item -> {
            var sortingFunc = getSortingFunc(patterns(filter).getStackInSlot(0));
            var list = new ArrayList<>(ForgeUtils.toItemList(patterns(filter)));
            list.remove(0);
            return sortingFunc == null || sortingFunc.test(list, item.getItem());
        };
    }

    @Override
    public BaseItemMenu menu(ItemStack item, Player player, int slot, int id, Inventory inv) {
        return new Menu(patterns(item), slot, id, inv);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return openMenu(level, player, hand);
    }


    public static class Menu extends PatternMenu {

        public Menu(int p_38852_, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(10), buf.readInt(), p_38852_, inv);
        }

        public Menu(IItemHandler dummyItems, int slot, int p_38852_, Inventory inv) {
            super(TPItems.SORTING_FILTER, slot, p_38852_, inv, "sorting_filter", 143);
            addInventory();
            addPatterns(addItemHandler(dummyItems, 0, 0, SorterPattern::new, 8));
            addPatterns(addItemHandler(dummyItems, 1, 9, ItemFilterPattern::new, 30));
        }

        public static class SorterPattern extends ItemFilterPattern {

            public SorterPattern(IItemHandler inv, int index, int xPosition, int yPosition) {
                super(inv, index, xPosition, yPosition);
            }

            @Override
            public boolean shouldSet(ItemStack item) {
                return super.shouldSet(item) && getSortingFunc(item) != null;
            }
        }
    }

    public static class Screen extends BaseScreen<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }
}
