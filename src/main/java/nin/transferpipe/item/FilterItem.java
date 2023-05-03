package nin.transferpipe.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.gui.BaseItemMenu;
import nin.transferpipe.gui.BaseScreen;
import nin.transferpipe.gui.PatternSlot;
import nin.transferpipe.util.forge.ObscuredInventory;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.IntStream;

public class FilterItem extends UpgradeItem implements GUIItem {

    public static String INVERTED = "Inverted";
    public static String IGNORE_NBT = "IgnoreNBT";
    public static String IGNORE_DURABILITY = "IgnoreDurability";

    public FilterItem(Properties p_41383_) {
        super(p_41383_);
    }

    public boolean inverted(ItemStack filter) {
        return MCUtils.computeBoolean(filter, INVERTED);
    }

    public boolean ignoreDurability(ItemStack filter) {
        return MCUtils.computeBoolean(filter, IGNORE_DURABILITY);
    }

    public boolean ignoreNBT(ItemStack filter) {
        return MCUtils.computeBoolean(filter, IGNORE_NBT);
    }

    public ObscuredInventory filteringItems(ItemStack filter) {
        return (ObscuredInventory) filter.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
    }

    public Predicate<ItemStack> getFilter(ItemStack filter) {
        return item -> {
            var filtered = IntStream.range(0, filteringItems(filter).getSlots()).anyMatch(i -> {
                var filteringItem = filteringItems(filter).getStackInSlot(i);
                return filteringItem.getItem() instanceof FilterItem f
                       ? f.getFilter(filteringItem).test(item)
                       : ignoreNBT(filter)
                         ? filteringItem.is(item.getItem())
                         : ignoreDurability(filter)
                           ? MCUtils.sameItemSameTagExcept(item, filteringItem, "Damage")
                           : ItemStack.isSameItemSameTags(item, filteringItem);
            });
            return inverted(filter) != filtered;
        };
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ObscuredInventory(9);
    }

    @Override
    public BaseItemMenu menu(ItemStack item, Player player, int slot, int id, Inventory inv) {
        return new FilterItem.Menu(filteringItems(item), slot, id, inv);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return openMenu(level, player, hand);
    }


    public static class Menu extends BaseItemMenu {

        public static int filteringItemsY = 18;

        public Menu(int p_38852_, Inventory inv, FriendlyByteBuf buf) {
            this(new ObscuredInventory(9), buf.readInt(), p_38852_, inv);
        }

        public Menu(ObscuredInventory filteringItems, int slot, int p_38852_, Inventory inv) {
            super(TPItems.ITEM_FILTER, slot, p_38852_, inv, "filter", 131);
            addInventory();
            addItemHandlerSlots(filteringItems, PatternSlot::new, filteringItemsY);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
            var quickMovedSlot = this.slots.get(quickMovedSlotIndex);
            if (quickMovedSlot.hasItem()) {
                if (hotbarStart <= quickMovedSlotIndex && quickMovedSlotIndex <= inventoryEnd) {
                    //一つだけフィルターに登録する
                    var filteringItems = IntStream.rangeClosed(containerStart, containerEnd)
                            .mapToObj(this::getSlot)
                            .filter(Slot::hasItem).toList();
                    var emptySlot = IntStream.rangeClosed(containerStart, containerEnd)
                            .mapToObj(this::getSlot)
                            .filter(s -> !s.hasItem()).findFirst();
                    var item = quickMovedSlot.getItem();
                    if (emptySlot.isPresent() && filteringItems.stream().noneMatch(s -> ItemHandlerHelper.canItemStacksStack(item, s.getItem())))
                        emptySlot.get().safeInsert(item);
                    else {
                        //インベントリ<=>ホットバーで差しあう
                        if (quickMovedSlotIndex <= hotbarEnd)
                            moveItemTo(item, inventoryStart, inventoryEnd, false);
                        else
                            moveItemTo(item, hotbarStart, hotbarEnd, false);
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
