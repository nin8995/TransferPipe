package nin.transferpipe.block.pipe;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nin.transferpipe.block.GUIEntityBlock;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.gui.BaseRegulatableScreen;
import nin.transferpipe.network.BasePacket;
import nin.transferpipe.network.TPPackets;

public class RegulatableRationingPipe extends RationingPipe implements GUIEntityBlock<RegulatableRationingPipe.Tile> {

    public RegulatableRationingPipe() {
        super(-1);
    }

    @Override
    public TPBlocks.RegistryGUIEntityBlock<Tile> registryWithGUI() {
        return TPBlocks.REGULATABLE_RATIONING_PIPE;
    }

    @Override
    public BaseBlockMenu menu(Tile tile, int id, Inventory inv) {
        return new Menu(tile.getBlockPos(), tile.ration, id, inv);
    }

    @Override
    public int getItemRation(Level level, BlockPos pos) {
        return getTile(level, pos).item;
    }

    @Override
    public int getLiquidRation(Level level, BlockPos pos) {
        return getTile(level, pos).liquid;
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        var tile = getTile(level, pos);
        return openMenu(level, pos, player, tile, buf -> {
            buf.writeBlockPos(tile.getBlockPos());
            buf.writeInt(tile.item);
            buf.writeInt(tile.liquid);
        });
    }

    public static class Tile extends nin.transferpipe.block.Tile {

        public int item = 64;
        public int liquid = 64 * 250;

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.REGULATABLE_RATIONING_PIPE.tile(), p_155229_, p_155230_);
        }

        public void setRation(int item, int liquid) {
            this.item = item;
            this.liquid = liquid;
            setChanged();
        }

        public static String ITEM = "Item";
        public static String LIQUID = "Liquid";

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);

            tag.putInt(ITEM, item);
            tag.putInt(LIQUID, liquid);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);

            if (tag.contains(ITEM))
                item = tag.getInt(ITEM);
            if (tag.contains(LIQUID))
                liquid = tag.getInt(LIQUID);
        }

        public ContainerData ration = new ContainerData() {
            @Override
            public int get(int p_39284_) {
                return switch (p_39284_) {
                    case 0 -> item;
                    case 1 -> liquid;
                    default -> -1;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0 -> item = value;
                    case 1 -> liquid = value;
                }
                setChanged();
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public static class Menu extends BaseBlockMenu {

        private final ContainerData ration;
        private final BlockPos pos;

        public Menu(int id, Inventory inv, FriendlyByteBuf buf) {
            this(buf.readBlockPos(), readRation(buf), id, inv);
        }

        public static SimpleContainerData readRation(FriendlyByteBuf buf) {
            var clientData = new SimpleContainerData(2);
            clientData.set(0, buf.readInt());
            clientData.set(1, buf.readInt());
            return clientData;
        }

        public Menu(BlockPos pos, ContainerData ration, int id, Inventory inv) {
            super(TPBlocks.REGULATABLE_RATIONING_PIPE, id, inv, "regulatable", 32);
            this.ration = ration;
            this.pos = pos;
            addDataSlots(ration);
        }

        @Override
        public boolean noInventory() {
            return true;
        }

        @Override
        public boolean noTitleText() {
            return true;
        }

        public int getItem() {
            return ration.get(0);
        }

        public int getLiquid() {
            return ration.get(1);
        }
    }

    public static class Screen extends BaseRegulatableScreen<Menu> {

        public Screen(Menu menu, Inventory p_97742_, Component p_97743_) {
            super(menu, p_97742_, p_97743_);
        }

        @Override
        public Pair<Integer, Integer> getRation() {
            return Pair.of(menu.getItem(), menu.getLiquid());
        }

        @Override
        public void onClose() {
            TPPackets.REGULATABLE_RATION_PIPE.accept(menu.pos, toInt(itemRation.getValue()), toInt(liquidRation.getValue()));
            super.onClose();
        }
    }

    public static class Packet extends BasePacket.arg3<BlockPos, Integer, Integer> {

        BlockPos pos;
        int item;
        int liquid;

        @Override
        public FriendlyByteBuf encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeInt(item);
            buf.writeInt(liquid);
            return buf;
        }

        @Override
        public BasePacket decode(FriendlyByteBuf buf) {
            pos = buf.readBlockPos();
            item = buf.readInt();
            liquid = buf.readInt();
            return this;
        }

        @Override
        public BasePacket init(BlockPos pos, Integer integer, Integer integer2) {
            this.pos = pos;
            this.item = integer;
            this.liquid = integer2;
            return this;
        }

        @Override
        public void handleOnServer(ServerPlayer sp) {
            if (sp.getLevel().getBlockEntity(pos) instanceof Tile tile)
                tile.setRation(item, liquid);
        }
    }
}
