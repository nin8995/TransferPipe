package nin.transferpipe;

import net.minecraft.nbt.CompoundTag;

/**
 * Mixin内にstaticフィールドを置けないのでここに置く
 */
public class MixinTemp {

    public static ThreadLocal<CompoundTag> tileData = new ThreadLocal<>();
}
