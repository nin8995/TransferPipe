package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.common.util.INBTSerializable;
import nin.transferpipe.util.java.UtilSetMap;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class PosDirsSet extends UtilSetMap<BlockPos, Direction> implements INBTSerializable<CompoundTag> {

    public PosDirsSet() {
        super();
    }

    public PosDirsSet(BiConsumer<BlockPos, Direction> removeFunc) {
        super(removeFunc);
    }

    /**
     * NBT
     */
    public static final String POS = "Pos";
    public static final String DIRS = "Dirs";

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        AtomicInteger i = new AtomicInteger(0);

        forEach((pos, dirs) -> {
            var subTag = new CompoundTag();
            subTag.put(POS, NbtUtils.writeBlockPos(pos));
            subTag.putIntArray(DIRS, dirs.stream().map(Enum::ordinal).toList());

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
            var dirs = Arrays.stream(subTag.getIntArray(DIRS))
                    .mapToObj(ordinal -> Direction.values()[ordinal])
                    .collect(Collectors.toSet());
            addAll(pos, dirs);
        });
    }
}
