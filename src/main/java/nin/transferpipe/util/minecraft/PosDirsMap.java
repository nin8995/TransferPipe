package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.common.util.INBTSerializable;
import nin.transferpipe.util.java.Consumer3;
import nin.transferpipe.util.java.FlagMapMap;

import java.util.concurrent.atomic.AtomicInteger;

public class PosDirsMap<V> extends FlagMapMap<BlockPos, Direction, V> implements INBTSerializable<CompoundTag> {

    public PosDirsMap() {
        super();
    }

    public PosDirsMap(Consumer3<BlockPos, Direction, V> removeFunc) {
        super(removeFunc);
    }

    /**
     * NBT
     */
    public static String POS = "Pos";
    public static String DIRS = "Dirs";

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        var i = new AtomicInteger(0);

        forEach((pos, dirsMap) -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            var dirTag = new CompoundTag();
            if (cache.containsKey(pos))
                cache.get(pos).forEach((dir, marked) ->
                        dirTag.putBoolean(String.valueOf(dir.ordinal()), marked));
            dirsMap.keySet().forEach(dir ->
                    dirTag.putBoolean(String.valueOf(dir.ordinal()), marks.contains(pos, dir)));

            subTag.put(DIRS, dirTag);

            tag.put(String.valueOf(i), subTag);
            i.getAndIncrement();
        });


        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        tag.getAllKeys().forEach(i -> {
            var subTag = tag.getCompound(i);
            var pos = NbtUtils.readBlockPos(subTag.getCompound(POS));
            var dirsTag = subTag.getCompound(DIRS);
            dirsTag.getAllKeys().forEach(dirOrdinal -> {
                var dir = Direction.values()[Integer.parseInt(dirOrdinal)];
                var marked = dirsTag.getBoolean(dirOrdinal);
                cache.add(pos, dir, marked);
            });
        });
    }
}
