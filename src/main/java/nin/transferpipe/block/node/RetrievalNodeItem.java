package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.util.forge.ForgeUtils;
import org.joml.Vector3f;

public class RetrievalNodeItem extends BaseBlockNode.Facing<RetrievalNodeItem.Tile> {

    @Override
    public TPBlocks.RegistryGUIEntityBlock<RetrievalNodeItem.Tile> registryWithGUI() {
        return TPBlocks.RETRIEVAL_NODE_ITEM;
    }

    @Override
    public BaseBlockMenu menu(Tile tile, int id, Inventory inv) {
        return new Menu(tile.itemSlot, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseMenuNode.Item {

        //client
        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        //server
        public Menu(IItemHandler slot, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(TPBlocks.RETRIEVAL_NODE_ITEM, slot, upgrades, searchData, containerId, inv);
        }
    }

    public static class Screen extends BaseScreenNode<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        public String getSearchMsg() {
            return "gui.transferpipe.searching_item";
        }
    }

    public static class Tile extends BaseTileNodeItem {

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.RETRIEVAL_NODE_ITEM.tile(), p_155229_, p_155230_);
        }

        @Override
        public boolean shouldSearch() {
            return itemSlot.hasFreeSpace();
        }

        @Override
        public Vector3f getColor() {
            return new Vector3f(0, 1, 0);
        }

        @Override
        public void calcUpgrades() {
            super.calcUpgrades();
            pseudoRoundRobin = true;
            stickingSearch = true;
        }

        @Override
        public void facing(BlockPos pos, Direction dir) {
            if (itemSlot.hasItem())
                ForgeUtils.forItemHandler(level, pos, dir, this::tryInsert);
        }

        @Override
        public boolean canWork(BlockPos pos, Direction d) {
            return ForgeUtils.getItemHandler(level, pos, d).filter(this::canExtract).isPresent();
        }

        @Override
        public void work(BlockPos pos, Direction dir) {
            ForgeUtils.forItemHandler(level, pos, dir, this::tryExtract);
        }
    }
}
