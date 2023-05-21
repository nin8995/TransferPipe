package nin.transferpipe.util.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

public interface GUITile {

    BaseBlockMenu menu(int id, Inventory inv);

    InteractionResult openMenu(Player player);

    default InteractionResult openMenu(Level level, BlockPos pos, Player player, BlockEntity tile) {
        return openMenu(level, pos, player, buf -> buf.writeBlockPos(tile.getBlockPos()));
    }

    default InteractionResult openMenu(Level level, BlockPos pos, Player player, Consumer<FriendlyByteBuf> writer) {
        try {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (i, inv, pl) -> menu(i, inv).setAccess(ContainerLevelAccess.create(level, pos)).setBlock(level.getBlockState(pos).getBlock()),
                        level.getBlockState(pos).getBlock().getName()), writer);

            return InteractionResult.sidedSuccess(level.isClientSide);
        } catch (Exception e) {
            return InteractionResult.PASS;
        }
    }
}
