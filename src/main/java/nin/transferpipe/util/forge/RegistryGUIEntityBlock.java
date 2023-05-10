package nin.transferpipe.util.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public record RegistryGUIEntityBlock<T extends BlockEntity>
        (RegistryObject<Block> roBlock,
         RegistryObject<BlockEntityType<T>> roTile, BlockEntityType.BlockEntitySupplier<T> tileSupplier,
         RegistryObject<MenuType<?>> roMenu, MenuScreens.ScreenConstructor<?, ?> screen) {

    public Block block() {
        return roBlock.get();
    }

    public BlockEntityType<T> tile() {
        return roTile.get();
    }

    public MenuType<?> menu() {
        return roMenu.get();
    }

    public RegistryGUI gui() {
        return new RegistryGUI(roMenu, screen);
    }
}
