package nin.transferpipe.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import nin.transferpipe.util.minecraft.BaseItemMenu;
import nin.transferpipe.util.minecraft.BaseMenu;
import nin.transferpipe.util.minecraft.SwapRestricted;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class BanRestrictedSwapping {

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    /**
     * @param swapTo ホットバースロット上なら0~9、オフハンドなら４０。インヴェントリ上での番号であり、スロットの追加順になどなっていないので注意！
     */
    @Inject(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    public void ban(int clicked, int swapTo, ClickType p_150433_, Player player, CallbackInfo ci) {
        if ((Object) this instanceof BaseMenu menu) {
            if (slots.get(clicked) instanceof SwapRestricted
                    || (swapTo == 40 && menu instanceof BaseItemMenu itemMenu && itemMenu.shouldLock() && itemMenu.slot == 40))
                ci.cancel();
        }
    }
}
