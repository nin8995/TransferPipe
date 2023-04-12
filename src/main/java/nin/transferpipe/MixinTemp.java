package nin.transferpipe;

import net.minecraft.nbt.CompoundTag;

public class MixinTemp {

    public static ThreadLocal<CompoundTag> tileData = new ThreadLocal<>();
}
