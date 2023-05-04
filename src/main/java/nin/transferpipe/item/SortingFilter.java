package nin.transferpipe.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import nin.transferpipe.block.pipe.SortingPipe;
import nin.transferpipe.gui.BaseItemMenu;
import nin.transferpipe.gui.BaseScreen;
import nin.transferpipe.gui.PatternSlot;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.ObscuredInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

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


    public ObscuredInventory sortingItems(ItemStack filter) {
        return (ObscuredInventory) filter.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
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
            var sortingFunc = getSortingFunc(sortingItems(filter).getStackInSlot(0));
            var list = new ArrayList<>(ForgeUtils.toItemList(sortingItems(filter)));
            list.remove(0);
            return sortingFunc == null || sortingFunc.test(list, item.getItem());
        };
    }

    @Override
    public BaseItemMenu menu(ItemStack item, Player player, int slot, int id, Inventory inv) {
        return new Menu(sortingItems(item), slot, id, inv);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return openMenu(level, player, hand);
    }


    public static class Menu extends BaseItemMenu {

        public Menu(int p_38852_, Inventory inv, FriendlyByteBuf buf) {
            this(new ObscuredInventory(10), buf.readInt(), p_38852_, inv);
        }

        public Menu(ObscuredInventory filteringItems, int slot, int p_38852_, Inventory inv) {
            super(TPItems.SORTING_FILTER, slot, p_38852_, inv, "sorting_filter", 143);
            addInventory();
            addItemHandlerSlots(filteringItems, 0, 0, SortingPatternSlot::new, 8);
            addItemHandlerSlots(filteringItems, 1, 9, PatternSlot::new, 30);
        }

        public static class SortingPatternSlot extends PatternSlot {

            public SortingPatternSlot(ObscuredInventory itemHandler, int index, int xPosition, int yPosition) {
                super(itemHandler, index, xPosition, yPosition);
            }

            @Override
            public ItemStack safeInsert(ItemStack item, int p_150658_) {
                if (getSortingFunc(item) != null)
                    set(item.copyWithCount(1));
                return item;
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
            var quickMovedSlot = this.slots.get(quickMovedSlotIndex);
            if (quickMovedSlot.hasItem()) {
                var item = quickMovedSlot.getItem();
                if (hotbarStart <= quickMovedSlotIndex && quickMovedSlotIndex <= inventoryEnd) {
                    if (!slots.get(containerStart).hasItem() && getSortingFunc(item) != null)
                        slots.get(containerStart).safeInsert(item);
                    else {
                        //一つだけフィルターに登録する
                        var filteringItems = IntStream.rangeClosed(containerStart + 1, containerEnd)
                                .mapToObj(this::getSlot)
                                .filter(Slot::hasItem).toList();
                        var emptySlot = IntStream.rangeClosed(containerStart + 1, containerEnd)
                                .mapToObj(this::getSlot)
                                .filter(s -> !s.hasItem()).findFirst();
                        if (emptySlot.isPresent() && filteringItems.stream().noneMatch(s -> s.getItem().is(item.getItem())))
                            emptySlot.get().safeInsert(item);
                        else {
                            //インベントリ<=>ホットバーで差しあう
                            if (quickMovedSlotIndex <= hotbarEnd)
                                moveItemTo(item, inventoryStart, inventoryEnd, false);
                            else
                                moveItemTo(item, hotbarStart, hotbarEnd, false);
                        }
                    }
                } else
                    quickMovedSlot.set(ItemStack.EMPTY);
            }
            return ItemStack.EMPTY;
        }
    }

    public static class Screen extends BaseScreen<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }
}
