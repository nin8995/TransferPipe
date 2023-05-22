package nin.transferpipe.item.filter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.item.TPItems;
import nin.transferpipe.item.upgrade.UpgradeItem;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.ObscuredInventory;
import nin.transferpipe.util.minecraft.BaseItemMenu;
import nin.transferpipe.util.minecraft.BaseScreen;
import nin.transferpipe.util.minecraft.GUIItem;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ItemFilter extends UpgradeItem implements IItemFilter, GUIItem {

    public static String INVERTED = "Inverted";
    public static String IGNORE_NBT = "IgnoreNBT";
    public static String IGNORE_DURABILITY = "IgnoreDurability";

    public ItemFilter(Properties p_41383_) {
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

    public IItemHandler patterns(ItemStack filter) {
        return filter.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
    }

    public boolean isEmpty(ItemStack itemFilter) {
        return ForgeUtils.isEmpty(patterns(itemFilter));
    }

    @Override
    public Predicate<ItemStack> getFilter(ItemStack filter) {
        return item -> {
            var filtered = ForgeUtils.stream(patterns(filter)).filter(i -> !i.isEmpty()).anyMatch(pattern ->
                    pattern.getItem() instanceof IItemFilter f && !ForgeUtils.isEmpty(patterns(pattern))
                    ? f.getFilter(pattern).test(item)
                    : ignoreNBT(filter)
                      ? item.is(pattern.getItem())
                      : ignoreDurability(filter)
                        ? MCUtils.sameExcept(item, pattern, "Damage")
                        : MCUtils.same(item, pattern));
            return inverted(filter) != filtered;
        };
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ObscuredInventory(9);
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
            this(new ItemStackHandler(9), buf.readInt(), p_38852_, inv);
        }

        public Menu(IItemHandler dummyItems, int slot, int p_38852_, Inventory inv) {
            super(TPItems.ITEM_FILTER, slot, p_38852_, inv, "item_filter", 131);
            addInventory();
            addPatterns(addItemHandler(dummyItems, ItemFilterPattern::new, 18));
        }
    }

    public static class Screen extends BaseScreen<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }
}
