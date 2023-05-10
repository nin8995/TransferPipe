package nin.transferpipe.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import nin.transferpipe.gui.PatternSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class HandlePatternClicking {

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    public abstract ItemStack getCarried();

    @Inject(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameTags(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;onSwapCraft(I)V")), remap = false)
    public void resetOrSwap(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_, CallbackInfo ci) {
        Slot slot = slots.get(p_150431_);
        if (slot instanceof PatternSlot pattern)//PatternSlotはmayPlace falseなので実質 } else if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) { だけに挿入される
            if (pattern.isSamePattern(getCarried()))
                pattern.resetPattern();
            else
                pattern.trySetPattern(getCarried());
    }
}
