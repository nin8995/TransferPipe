package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Tile extends BlockEntity {

    public Tile(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    /**
     * &#064;Nullableの上書き
     */
    public Level level;

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public void setLevel(Level level) {
        super.level = level;
        this.level = level;
    }

    /**
     * 置かれた最初のtickの処理
     */
    public static final String FIRST_TICK = "FirstTick";
    private boolean firstTick = true;

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(FIRST_TICK, firstTick);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(FIRST_TICK))
            firstTick = tag.getBoolean(FIRST_TICK);
    }

    public void onFirstTick() {
    }

    public void tick() {
        if (firstTick) {
            onFirstTick();
            firstTick = false;
            setChanged();
        }
    }

    /**
     * remove時の処理
     */
    public void onRemove() {
    }

    @Override
    public void setRemoved() {
        onRemove();
        super.setRemoved();
    }

    /**
     * 省略
     */
    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    public Block getBlock(BlockPos pos) {
        return getBlockState(pos).getBlock();
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        return level.getBlockEntity(pos);
    }
}
