package nin.transferpipe.util.forge;

import com.mojang.datafixers.util.Function3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import nin.transferpipe.util.java.Consumer3;
import nin.transferpipe.util.java.LoadResult;
import nin.transferpipe.util.minecraft.PosDirsMap;

import java.util.List;

public class LazyOptionalMap<V> extends PosDirsMap<LazyOptional<V>> {

    private final Function3<Level, BlockPos, Direction, LazyOptional<V>> loGetter;

    public LazyOptionalMap(Function3<Level, BlockPos, Direction, LazyOptional<V>> loGetter) {
        this(loGetter, (pos, dir, v) -> {
        });
    }

    public LazyOptionalMap(Function3<Level, BlockPos, Direction, LazyOptional<V>> loGetter, Consumer3<BlockPos, Direction, V> removeFunc) {
        super((pos, dir, lo) -> lo.ifPresent(v -> removeFunc.accept(pos, dir, v)));
        this.loGetter = loGetter;
    }

    public void tryLoadCache(Level level) {
        tryLoadCache((pos, dir) ->
                level.isLoaded(pos)
                ? loGetter.apply(level, pos, dir).isPresent()
                  ? LoadResult.a(loGetter.apply(level, pos, dir))
                  : LoadResult.na()
                : LoadResult.nl());
    }

    public List<V> forceGetValues() {
        return super.getValues(loEnergy -> loEnergy.resolve().get());
    }
}
