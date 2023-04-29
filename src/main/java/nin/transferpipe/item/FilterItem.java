package nin.transferpipe.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.gui.BaseItemMenu;
import nin.transferpipe.gui.BaseScreen;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
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
        return TPUtils.computeBoolean(filter, INVERTED);
    }

    public boolean ignoreDurability(ItemStack filter) {
        return TPUtils.computeBoolean(filter, IGNORE_DURABILITY);
    }

    public boolean ignoreNBT(ItemStack filter) {
        return TPUtils.computeBoolean(filter, IGNORE_NBT);
    }

    public FilterInventory filteringItems(ItemStack filter) {
        return (FilterInventory) filter.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
    }

    public Predicate<ItemStack> getFilter(ItemStack filter) {
        return item -> {
            var filtered = IntStream.range(0, filteringItems(filter).getSlots()).anyMatch(i -> {
                var filteringItem = filteringItems(filter).getStackInSlot(i);
                return filteringItem.getItem() instanceof FilterItem f ? f.getFilter(filteringItem).test(item)
                                                                       : ignoreNBT(filter) ? filteringItem.is(item.getItem())
                                                                                           : ignoreDurability(filter) ? TPUtils.sameItemSameTagExcept(item, filteringItem, "Damage")
                                                                                                                      : ItemStack.isSameItemSameTags(item, filteringItem);
            });
            return inverted(filter) != filtered;
        };
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FilterInventory(9);
    }

    @Override
    public BaseItemMenu menu(ItemStack item, Player player, int id, Inventory inv) {
        return new FilterItem.Menu(filteringItems(item), id, inv);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return openMenu(level, player, hand);
    }

    public static class FilterInventory extends ItemStackHandler implements ICapabilityProvider {

        public FilterInventory(int size) {
            super(size);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, LazyOptional.of(() -> this));
        }
    }

    public static class Menu extends BaseItemMenu {

        public static int filteringItemsY = 18;

        public Menu(int p_38852_, Inventory inv) {
            this(new FilterInventory(9), p_38852_, inv);
        }

        public Menu(FilterInventory filteringItems, int p_38852_, Inventory inv) {
            super(TPItems.ITEM_FILTER, p_38852_, inv, "filter");
            addItemHandlerSlots(filteringItems, FilteringSlot::new, filteringItemsY);
        }

        @Override
        public int getOffsetY() {
            return filteringItemsY + 31;
        }

        @Override
        public boolean shouldLock(Slot info, int index) {
            return info.container instanceof Inventory inv && inv.selected == index;
        }

        public static class FilteringSlot extends SlotItemHandler {

            public FilteringSlot(FilterInventory itemHandler, int index, int xPosition, int yPosition) {
                super(itemHandler, index, xPosition, yPosition);
            }

            @Override
            public ItemStack safeInsert(ItemStack p_150660_, int p_150658_) {
                set(p_150660_.copyWithCount(1));
                return p_150660_;
            }
/*
            @Override
            public ItemStack safeTake(int p_150648_, int p_150649_, Player p_150650_) {
                set(ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }*/

            @Override
            public Optional<ItemStack> tryRemove(int p_150642_, int p_150643_, Player p_150644_) {
                set(ItemStack.EMPTY);
                return Optional.empty();
            }

            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            public boolean mayPickup(Player player) {
                return true;
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
            if (inventoryStart <= quickMovedSlotIndex && quickMovedSlotIndex <= hotbarEnd)
                IntStream.rangeClosed(containerStart, containerEnd)
                        .mapToObj(this::getSlot)
                        .filter(s -> !s.hasItem())
                        .findFirst().ifPresent(slot -> slot.safeInsert(getSlot(quickMovedSlotIndex).getItem()));
            else
                getSlot(quickMovedSlotIndex).set(ItemStack.EMPTY);

            return ItemStack.EMPTY;
        }
    }

    public static class Screen extends BaseScreen<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }
}
