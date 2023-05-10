package nin.transferpipe.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import nin.transferpipe.gui.LiquidItemSlot;
import nin.transferpipe.util.minecraft.MCUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public class RenderLiquidSlot extends Screen {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    protected RenderLiquidSlot(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    public void renderLiquid(PoseStack p_97800_, Slot slot, CallbackInfo ci) {
        if (slot instanceof LiquidItemSlot liquidItem) {
            var liquid = liquidItem.getLiquid();
            if (!liquid.isEmpty()) {
                MCUtils.renderLiquid(liquid, p_97800_, slot.x, slot.y, 16);
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V"), cancellable = true)
    public void hideLiquidItemTooltip(PoseStack p_97791_, int p_97792_, int p_97793_, CallbackInfo ci) {
        if (hoveredSlot instanceof LiquidItemSlot liquidItem && !liquidItem.getLiquid().isEmpty()) {
            renderTooltip(p_97791_, liquidItem.getLiquid().getDisplayName(), p_97792_, p_97793_);
            ci.cancel();
        }
    }
}
