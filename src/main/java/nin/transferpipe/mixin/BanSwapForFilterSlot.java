package nin.transferpipe.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import nin.transferpipe.item.FilterItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractContainerMenu.class)
public class BanSwapForFilterSlot {

    @Inject(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
            cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void ban(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_, CallbackInfo ci, Inventory inventory, Slot slot2) {
        if (slot2 instanceof FilterItem.Menu.FilteringSlot)
            ci.cancel();
    }
}
