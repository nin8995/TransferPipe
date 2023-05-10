package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.BaseBlockMenu;
import nin.transferpipe.util.minecraft.MCUtils;
import org.joml.Vector3f;

public class RetrievalNodeItem extends BaseNodeBlock.Facing<RetrievalNodeItem.Tile> {

    @Override
    public RegistryGUIEntityBlock<Tile> registryWithGUI() {
        return TPBlocks.RETRIEVAL_NODE_ITEM;
    }

    @Override
    public BaseBlockMenu menu(Tile tile, int id, Inventory inv) {
        return new Menu(tile.itemSlot, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseNodeMenu.Item {

        //client
        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        //server
        public Menu(IItemHandler slot, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(TPBlocks.RETRIEVAL_NODE_ITEM, slot, upgrades, searchData, containerId, inv);
        }
    }

    public static class Screen extends BaseNodeScreen<Menu> {

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
                if (ForgeUtils.hasItemHandler(level, pos, dir))
                    ForgeUtils.forItemHandler(level, pos, dir, this::tryInsert);
                else if (worldInteraction > 0) {
                    if (getBlock(pos) == Blocks.AIR)
                        tryInsert(pos, dir.getOpposite());
                }
        }

        public void tryInsert(BlockPos pos, Direction boxDir) {
            var boxSize = 1 + 2 * JavaUtils.log(2, worldInteraction);
            var boxCenter = MCUtils.relative(pos, boxDir, boxSize / 2);
            var box = AABB.ofSize(boxCenter, boxSize, boxSize, boxSize);
            var invEntities = JavaUtils.filter(MCUtils.getMappableMappedEntities(level, box, ForgeUtils::getItemHandler), this::canInsert);
            if (!invEntities.isEmpty()) {
                for (IItemHandler inv : invEntities) {
                    if (itemSlot.isEmpty())
                        break;
                    tryInsert(inv);
                }

                if (addParticle)
                    addEdges(boxCenter, (float) boxSize / 2);
            }
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
