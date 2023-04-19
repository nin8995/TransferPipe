package nin.transferpipe.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import nin.transferpipe.block.TPBlocks;

public abstract class BaseBlockMenu extends BaseMenu {

    private ContainerLevelAccess access;
    private final Block block;

    public BaseBlockMenu(TPBlocks.RegistryGUIEntityBlock registry, int p_38852_, Inventory inv, String bg) {
        super(registry.gui(), p_38852_, inv, bg);
        this.block = registry.block();
    }

    public BaseBlockMenu setAccess(ContainerLevelAccess access) {
        this.access = access;
        return this;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, block);
    }
}
