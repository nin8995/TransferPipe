package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.common.util.INBTSerializable;
import nin.transferpipe.util.java.FlagMap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class PosMap<V> extends FlagMap<BlockPos, V> implements INBTSerializable<CompoundTag> {

    public PosMap() {
        super();
    }

    public PosMap(BiConsumer<BlockPos, V> removeFunc) {
        super(removeFunc);
    }

    /**
     * NBT
     */
    public static String POS = "Pos";
    public static String MARKED = "Marked";

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        var i = new AtomicInteger(0);

        cache.forEach((pos, marked) -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            subTag.putBoolean(MARKED, marked);

            tag.put(i.toString(), subTag);
            i.getAndIncrement();
        });
        keySet().forEach(pos -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            subTag.putBoolean(MARKED, marks.contains(pos));

            tag.put(i.toString(), subTag);
            i.getAndIncrement();
        });

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        tag.getAllKeys().forEach(i -> {
            var subTag = tag.getCompound(i);
            var pos = NbtUtils.readBlockPos(subTag.getCompound(POS));
            var marked = subTag.getBoolean(MARKED);
            cache.put(pos, marked);
        });
    }
}
