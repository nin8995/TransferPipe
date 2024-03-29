package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public abstract class TileHolder extends BaseTile {

    @Nullable
    public BaseTile holdingTile = null;

    public static String POS = "TilePos";
    public static String STATE = "TileState";
    public static String DATA = "TileData";

    public TileHolder(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (holdingTile != null) {
            tag.put(POS, NbtUtils.writeBlockPos(holdingTile.getBlockPos()));
            tag.put(STATE, NbtUtils.writeBlockState(holdingTile.getBlockState()));
            tag.put(DATA, holdingTile.saveWithFullMetadata());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(POS)) {
            var pos = NbtUtils.readBlockPos(tag.getCompound(POS));
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(STATE));
            holdingTile = (BaseTile) BlockEntity.loadStatic(pos, state, tag.getCompound(DATA));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (holdingTile != null) {
            if (holdingTile.getLevel() == null)
                holdingTile.setLevel(level);
            holdingTile.tick();
        }
    }

    public <T, C> LazyOptional<T> orSuper(Capability<C> childCap, C childCapInstance, Capability<T> cap, Direction side) {
        return childCap == cap ? LazyOptional.of(() -> childCapInstance).cast()
                               : holdingTile != null ? holdingTile.getCapability(cap, side)
                                                     : super.getCapability(cap, side);
    }

    public void updateTile(BlockState state) {
        if (holdingTile != null && holdingTile.getBlockState().getBlock() == state.getBlock())
            holdingTile.setBlockState(state);
        else if (state.getBlock() instanceof EntityBlock entityBlock && entityBlock.newBlockEntity(worldPosition, state) instanceof BaseTile tile)
            holdingTile = tile;
        else
            holdingTile = null;

        setChanged();
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (holdingTile != null)
            holdingTile.onRemove();
    }

    public boolean hasHoldingTileMenu() {
        return holdingTile instanceof GUITile;
    }

    public InteractionResult openHoldingTileMenu(Player player) {
        if (holdingTile instanceof GUITile guiTile)
            return guiTile.openMenu(player);
        return InteractionResult.PASS;
    }
}
