package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface TickingGUIEntityBlock<T extends BlockEntity> extends TickingEntityBlock<T> {

    @Override
    default TPBlocks.RegistryEntityBlock<T> registry() {
        return new TPBlocks.RegistryEntityBlock<>(registryWithGUI().roBlock(), registryWithGUI().roTile(), registryWithGUI().tileSupplier());
    }

    TPBlocks.RegistryGUIEntityBlock<T> registryWithGUI();

    MenuProvider menu(Level level, BlockPos pos, T be);
}
