package nin.transferpipe.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import nin.transferpipe.BaseMenu;

public abstract class BaseBlockMenu extends BaseMenu {

    private ContainerLevelAccess access;
    private final Block block;

    protected BaseBlockMenu(TPBlocks.RegistryGUIEntityBlock registry, int p_38852_, Inventory inv, String bg, boolean noItemSlots) {
        super(registry.gui(), p_38852_, inv, bg, noItemSlots);
        this.block = registry.block();
    }

    public BaseBlockMenu setAccess(NonStaticTickingEntity be) {
        access = be.containerAccess();
        return this;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, block);
    }
}
