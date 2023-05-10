package nin.transferpipe.item.filter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.LiquidItemSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LiquidFilterPattern extends LiquidItemSlot implements PatternSlot {

    public LiquidFilterPattern(IItemHandler inv, int index, int xPosition, int yPosition) {
        super(inv, index, xPosition, yPosition);
    }

    @Override
    public boolean shouldSet(ItemStack item) {
        return ForgeUtils.hasFluidHandler(item) || item.getItem() instanceof BaseLiquidFilter;
    }

    @Override
    public void setPattern(ItemStack item) {
        set(item);
    }

    @Override
    public void resetPattern() {
        set(ItemStack.EMPTY);
    }

    @Override
    public boolean isSamePattern(ItemStack item) {
        return ForgeUtils.hasFluid(item) ? ForgeUtils.getFluid(item).isFluidEqual(getLiquid())
                                         : ItemHandlerHelper.canItemStacksStack(item, getItem());
    }

    @Override
    public boolean hasPattern() {
        return hasItem();
    }

    @Override
    public ItemStack safeInsert(ItemStack item, int p_150658_) {
        trySetPattern(item);
        return item;
    }

    @Override
    public Optional<ItemStack> tryRemove(int p_150642_, int p_150643_, Player p_150644_) {
        resetPattern();
        return Optional.empty();
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return true;
    }
}
