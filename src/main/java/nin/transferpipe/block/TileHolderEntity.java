package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TileHolderEntity extends NonStaticTickingEntity {

    @Nullable
    public NonStaticTickingEntity holdingTile = null;

    public static String TYPE = "TileType";
    public static String POS = "TilePos";
    public static String STATE = "TileState";
    public static String DATA = "TileData";
    public List<Direction> nonValidDirections = new ArrayList<>();

    public TileHolderEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (holdingTile != null) {
            tag.putString(TYPE, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(holdingTile.getType()).toString());
            tag.put(POS, NbtUtils.writeBlockPos(holdingTile.getBlockPos()));
            tag.put(STATE, NbtUtils.writeBlockState(holdingTile.getBlockState()));
            tag.put(DATA, holdingTile.saveWithFullMetadata());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TYPE)) {
            var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(new ResourceLocation(tag.getString(TYPE)));
            var pos = NbtUtils.readBlockPos(tag.getCompound(POS));
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(STATE));
            holdingTile = (NonStaticTickingEntity) type.create(pos, state);
            holdingTile.load(tag.getCompound(DATA));
        }
    }

    @Override
    public void tick() {
        TPUtils.forNullable(holdingTile, t -> {
            t.setLevel(level);
            t.tick();
            setChanged();
        });
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (nonValidDirections.contains(side))
            return LazyOptional.empty();

        var lo = TPUtils.fromNullable(holdingTile, t -> t.getCapability(cap, side));
        return lo != null && lo.isPresent() ? lo : super.getCapability(cap, side);
    }

    public void checkTile(Block block) {
        if (block instanceof EntityBlock entityBlock && entityBlock.newBlockEntity(worldPosition, block.defaultBlockState()) instanceof NonStaticTickingEntity tile
                && !(holdingTile != null && holdingTile.getClass().equals(tile.getClass()))) {
            holdingTile = tile;
            setChanged();
        }
    }

    @Override
    public void onRemove() {
        super.onRemove();
        TPUtils.forNullable(holdingTile, NonStaticTickingEntity::onRemove);
    }
}
