package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.Tile;
import nin.transferpipe.util.java.LoadResult;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.function.BiConsumer;

public class TileMap<V extends Tile> extends PosMap<V> {

    private final Class<V> beClass;

    public TileMap(Class<V> beClass) {
        this(beClass, (pos, v) -> {
        });
    }

    public TileMap(Class<V> beClass, BiConsumer<BlockPos, V> removeFunc) {
        super(removeFunc);
        this.beClass = beClass;
    }

    public void tryLoadCache(Level level) {
        tryLoadCache(pos ->
                level.isLoaded(pos)
                ? TPUtils.getTile(level, pos).getClass().isInstance(beClass)
                  ? LoadResult.a((V) TPUtils.getTile(level, pos))
                  : LoadResult.na()
                : LoadResult.nl());
    }
}