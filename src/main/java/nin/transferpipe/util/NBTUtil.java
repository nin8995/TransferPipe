package nin.transferpipe.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class NBTUtil {

    //HolderLookupのいらないNbtUtils.readBlockState
    public static BlockState readBlockState(Block b, CompoundTag tag) {
        if (!tag.contains("Name", 8)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            var bs = b.defaultBlockState();
            if (tag.contains("Properties", 10)) {
                var ps = tag.getCompound("Properties");
                for (String s : ps.getAllKeys())
                    bs = valueSetter(bs, b.getStateDefinition().getProperty(s), ps.getString(s));
            }
            return bs;
        }
    }

    public static <T extends Comparable<T>> BlockState valueSetter(BlockState bs, Property<T> p, String v) {
        return bs.setValue(p, p.getValue(v).get());
    }

    public static CompoundTag writeBlockState(BlockState bs) {
        return NbtUtils.writeBlockState(bs);
    }
}
