package nin.transferpipe.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nin.transferpipe.gui.BaseItemMenu;
import nin.transferpipe.gui.BaseRegulatableScreen;
import nin.transferpipe.network.BasePacket;
import nin.transferpipe.network.TPPackets;
import nin.transferpipe.util.minecraft.MCUtils;

public class RegulatableRationingUpgrade extends RationingUpgrade implements GUIItem {

    public RegulatableRationingUpgrade(Properties p_41383_) {
        super(-1, p_41383_);
    }

    public static String ITEM = "Item";
    public static String LIQUID = "Liquid";

    @Override
    public int getItemRation(ItemStack item) {
        return MCUtils.computeInt(item, ITEM, 64);
    }

    @Override
    public int getLiquidRation(ItemStack item) {
        return MCUtils.computeInt(item, LIQUID, 16000);
    }

    public static void setRation(ItemStack upgrade, int item, int liquid) {
        upgrade.getOrCreateTag().putInt(ITEM, item);
        upgrade.getOrCreateTag().putInt(LIQUID, liquid);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return openMenu(level, player, hand);
    }

    @Override
    public BaseItemMenu menu(ItemStack item, Player player, int slot, int id, Inventory inv) {
        return new Menu(slot, id, inv);
    }

    public static class Menu extends BaseItemMenu {

        private final Slot upgradeInInventory;

        public Menu(int p_38852_, Inventory inv, FriendlyByteBuf buf) {
            this(buf.readInt(), p_38852_, inv);
        }

        public Menu(int slot, int p_38852_, Inventory inv) {
            super(TPItems.REGULATABLE_RATIONING_UPGRADE, slot, p_38852_, inv, "regulatable", 32);
            this.upgradeInInventory = new Slot(inv, slot, 114514, 1919810);
            addSlot(upgradeInInventory);
        }

        @Override
        public boolean noInventory() {
            return true;
        }

        @Override
        public boolean noTitleText() {
            return true;
        }
    }

    public static class Screen extends BaseRegulatableScreen<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        int prevItem;
        int prevLiquid;

        @Override
        public Pair<Integer, Integer> getRation() {
            var upgrade = menu.upgradeInInventory.getItem();
            if (upgrade.getItem() instanceof RationingUpgrade rationing) {
                prevItem = rationing.getItemRation(upgrade);
                prevLiquid = rationing.getLiquidRation(upgrade);
                return Pair.of(prevItem, prevLiquid);
            }
            return Pair.of(0, 0);
        }

        @Override
        public void onClose() {
            var item = toInt(itemRation.getValue());
            var liquid = toInt(liquidRation.getValue());
            if (prevItem != item || prevLiquid != liquid)
                TPPackets.REGULATE_RATION_UPGRADE.accept(menu.slot, item, liquid);
            super.onClose();
        }
    }

    public static class Packet extends BasePacket.arg3<Integer, Integer, Integer> {

        private int slot;
        private int item;
        private int liquid;

        @Override
        public FriendlyByteBuf encode(FriendlyByteBuf buf) {
            buf.writeInt(slot);
            buf.writeInt(item);
            buf.writeInt(liquid);
            return buf;
        }

        @Override
        public BasePacket decode(FriendlyByteBuf buf) {
            slot = buf.readInt();
            item = buf.readInt();
            liquid = buf.readInt();
            return this;
        }

        @Override
        public BasePacket init(Integer integer, Integer integer2, Integer integer3) {
            slot = integer;
            item = integer2;
            liquid = integer3;
            return this;
        }

        @Override
        public void handleOnServer(ServerPlayer sp) {
            var upgrade = sp.getInventory().getItem(slot);
            if (upgrade.getItem() instanceof RegulatableRationingUpgrade) {
                setRation(upgrade, item, liquid);//ここでもタグ作る必要
                sp.getInventory().setItem(slot, upgrade);
            }
        }
    }
}
