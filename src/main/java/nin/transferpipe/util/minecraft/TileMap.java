package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import nin.transferpipe.util.java.LoadResult;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.function.BiConsumer;

public class TileMap<V extends BaseTile> extends PosMap<V> {

    private final Class<V> beClass;

    public TileMap(Class<V> beClass) {
        this(beClass, (pos, v) -> {
        });
    }

    public TileMap(Class<V> beClass, BiConsumer<BlockPos, V> removeFunc) {
        super(removeFunc);
        this.beClass = beClass;
    }

    public void tryLoadCache(Level level, BiConsumer<BlockPos, V> loadFunc) {
        tryLoadCache(pos ->
                level.isLoaded(pos)
                ? beClass.isAssignableFrom(TPUtils.getInnerTile(level, pos).getClass())
                  ? LoadResult.a((V) TPUtils.getInnerTile(level, pos))
                  : LoadResult.na()
                : LoadResult.nl(), loadFunc);
    }
}
