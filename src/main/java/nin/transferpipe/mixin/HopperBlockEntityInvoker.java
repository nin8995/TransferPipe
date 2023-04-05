package nin.transferpipe.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityInvoker {

    //なぜかNoClassDefFoundErrorで使えんかった
    @Invoker("canPlaceItemInContainer")
    public static boolean invokeCanPlaceItemInContainer(Container p_59335_, ItemStack p_59336_, int p_59337_, @Nullable Direction p_59338_) {
        throw new AssertionError();
    }
}
