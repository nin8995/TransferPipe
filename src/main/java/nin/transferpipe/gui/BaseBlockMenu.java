package nin.transferpipe.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import nin.transferpipe.block.TPBlocks;
import org.jetbrains.annotations.NotNull;

public abstract class BaseBlockMenu extends BaseMenu {

    private ContainerLevelAccess access;
    private final Block block;

    public BaseBlockMenu(TPBlocks.RegistryGUIEntityBlock<?> registry, int id, Inventory inv, String bg, int bgHeight) {
        super(registry.gui(), id, inv, bg, bgHeight);
        this.block = registry.block();
    }

    public BaseBlockMenu setAccess(ContainerLevelAccess access) {
        this.access = access;
        return this;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return AbstractContainerMenu.stillValid(access, player, block);
    }
}
