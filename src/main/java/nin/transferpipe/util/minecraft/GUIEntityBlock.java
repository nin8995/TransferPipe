package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nin.transferpipe.util.forge.RegistryEntityBlock;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;

public interface GUIEntityBlock<T extends Tile & GUITile> extends TickingEntityBlock<T> {

    @Override
    default RegistryEntityBlock<T> registry() {
        return new RegistryEntityBlock<>(registryWithGUI().roBlock(), registryWithGUI().roTile(), registryWithGUI().tileSupplier());
    }

    RegistryGUIEntityBlock<T> registryWithGUI();

    default InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        return getOuterTile(level, pos).openMenu(player);
    }
}
