package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public interface TickingGUIBlock<T extends NonStaticTickingEntity> extends TickingEntityBlock<T> {

    @Override
    default TPBlocks.RegistryEntityBlock<T> registry() {
        return new TPBlocks.RegistryEntityBlock<>(registryWithGUI().roBlock(), registryWithGUI().roTile(), registryWithGUI().tileSupplier());
    }

    TPBlocks.RegistryGUIEntityBlock<T> registryWithGUI();

    BaseBlockMenu menu(T be, int id, Inventory inv);

    default InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        try {
            var be = (T) level.getBlockEntity(pos);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (i, inv, pl) -> menu(be, i, inv).setAccess(be),
                        level.getBlockState(pos).getBlock().getName()));

            return InteractionResult.sidedSuccess(level.isClientSide);
        } catch (Exception e) {
            return InteractionResult.PASS;
        }
    }
}