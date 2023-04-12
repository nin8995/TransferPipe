package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class NonStaticTickerEntity extends BlockEntity {

    public NonStaticTickerEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    public abstract void tick();

    public void onRemove(){
    }

    @Override
    public void setRemoved() {
        onRemove();
        super.setRemoved();
    }
}