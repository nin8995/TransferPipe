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

        flatForEach((pos, dir, v) -> cache.add(pos, dir, marks.contains(pos, dir)));
        cache.forEach((pos, dirsMap) -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            var dirsTag = new CompoundTag();
            dirsMap.forEach((dir, marked) ->
                    dirsTag.putBoolean(String.valueOf(dir.ordinal()), marked));
            subTag.put(DIRS, dirsTag);

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
            var dirsTag = subTag.getCompound(DIRS);
            dirsTag.getAllKeys().forEach(dirOrdinal -> {
                var dir = Direction.values()[Integer.parseInt(dirOrdinal)];
                var marked = dirsTag.getBoolean(dirOrdinal);
                cache.add(pos, dir, marked);
            });
        });
    }
}
