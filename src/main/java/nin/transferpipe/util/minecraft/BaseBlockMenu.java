package nin.transferpipe.util.minecraft;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import org.jetbrains.annotations.NotNull;

public abstract class BaseBlockMenu extends BaseMenu {

    public ContainerLevelAccess access;
    public Block block;

    public BaseBlockMenu(RegistryGUIEntityBlock<?> registry, int id, Inventory inv, String bg, int bgHeight) {
        super(registry.gui(), id, inv, bg, bgHeight);
    }

    public BaseBlockMenu setAccess(ContainerLevelAccess access) {
        this.access = access;
        return this;
    }

    public BaseBlockMenu setBlock(Block block) {
        this.block = block;
        return this;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return AbstractContainerMenu.stillValid(access, player, block);
    }
}
