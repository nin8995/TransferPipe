package nin.transferpipe.util.forge;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public record RegistryEntityBlock<T extends BlockEntity>
        (RegistryObject<Block> roBlock,
         RegistryObject<BlockEntityType<T>> roTile, BlockEntityType.BlockEntitySupplier<T> tileSupplier) {

    public Block block() {
        return roBlock.get();
    }

    public BlockEntityType<T> tile() {
        return roTile.get();
    }
}
