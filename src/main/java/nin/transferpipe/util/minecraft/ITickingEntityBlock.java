package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.util.forge.RegistryEntityBlock;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.Nullable;

public interface ITickingEntityBlock<T extends BaseTile> extends EntityBlock {

    @Override
    default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == getType() ? (Level l, BlockPos p, BlockState bs, T t) -> {
            if (!level.isClientSide && t instanceof BaseTile be)
                be.tick();
        } : null;
    }

    @Nullable
    @Override
    default BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return registry().tileSupplier().create(p_153215_, p_153216_);
    }

    default BlockEntityType<T> getType() {
        return registry().tile();
    }

    RegistryEntityBlock<T> registry();

    default T getOuterTile(Level level, BlockPos pos) {
        return (T) TPUtils.getOuterTile(level, pos);
    }

    default T getInnerTile(Level level, BlockPos pos) {
        return (T) TPUtils.getInnerTile(level, pos);
    }
}
