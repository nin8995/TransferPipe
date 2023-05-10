package nin.transferpipe.util.minecraft;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public interface GUIItem {

    default InteractionResultHolder<ItemStack> openMenu(Level level, Player player, InteractionHand hand) {
        var item = player.getItemInHand(hand);
        var slot = hand == InteractionHand.OFF_HAND
                   ? Inventory.SLOT_OFFHAND
                   : player.getInventory().selected;
        try {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (i, inv, pl) -> menu(item, player, slot, i, inv),
                        item.getItem().getName(item)), buf -> buf.writeInt(slot));

            return InteractionResultHolder.sidedSuccess(item, level.isClientSide);
        } catch (Exception e) {
            return InteractionResultHolder.pass(item);
        }
    }

    BaseItemMenu menu(ItemStack item, Player player, int slot, int id, Inventory inv);
}
