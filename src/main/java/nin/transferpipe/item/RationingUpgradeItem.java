package nin.transferpipe.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nin.transferpipe.gui.BaseItemMenu;
import nin.transferpipe.gui.BaseScreen;
import nin.transferpipe.network.TPPackets;
import nin.transferpipe.util.TPUtils;

public class RationingUpgradeItem extends FunctionUpgrade {

    private final int ration;

    public static String ITEM = "Item";
    public static String LIQUID = "Liquid";

    public RationingUpgradeItem(int ration, Properties p_41383_) {
        super(p_41383_);
        this.ration = ration;
    }

    public int getItemRation(ItemStack item) {
        return ration;
    }

    public int getLiquidRation(ItemStack item) {
        return ration * 250;
    }

    public static class Regulatable extends RationingUpgradeItem implements GUIItem {

        public Regulatable(Properties p_41383_) {
            super(-1, p_41383_);
        }

        @Override
        public int getItemRation(ItemStack item) {
            return TPUtils.computeInt(item, ITEM, 64);
        }

        @Override
        public int getLiquidRation(ItemStack item) {
            return TPUtils.computeInt(item, LIQUID, 16000);
        }

        public void setRations(ItemStack upgrade, int item, int liquid) {
            upgrade.getOrCreateTag().putInt(ITEM, item);
            upgrade.getOrCreateTag().putInt(LIQUID, liquid);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return openMenu(level, player, hand);
        }

        @Override
        public BaseItemMenu menu(ItemStack item, Player player, int id, Inventory inv) {
            return new Menu(id, inv);
        }
    }

    public static class Menu extends BaseItemMenu {

        private final Slot upgradeInInventory;

        public Menu(int p_38852_, Inventory inv) {
            super(TPItems.REGULATABLE_RATIONING_UPGRADE, p_38852_, inv, "regulatable");
            noDefaultTexts();

            this.upgradeInInventory = new Slot(inv, inv.selected, 114514, 1919810);
            addSlot(upgradeInInventory);
        }

        @Override
        public boolean noInventory() {
            return true;
        }
    }

    public static class Screen extends BaseScreen<Menu> {

        private EditBox itemRation;
        private EditBox liquidRation;

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        protected void init() {
            super.init();
            var upgrade = menu.upgradeInInventory.getItem();
            if (upgrade.getItem() instanceof RationingUpgradeItem rationing) {

                itemRation = newEditBox(70, 10, -35 - 20);
                itemRation.setFilter(this::checkStr);
                itemRation.setValue(String.valueOf(rationing.getItemRation(upgrade)));//もしタグがない場合、クライアントでtag作ってるだけ、サーバーはまだタグ無し
                addRenderableWidget(itemRation);

                liquidRation = newEditBox(70, 10, 35 + 5);
                liquidRation.setFilter(this::checkStr);
                liquidRation.setValue(String.valueOf(rationing.getLiquidRation(upgrade)));
                addRenderableWidget(liquidRation);
            }
        }

        public EditBox newEditBox(int width, int height, int xOffset) {
            return new EditBox(font, (this.width - width) / 2 + xOffset, (this.height - height) / 2, width, height, Component.empty());
        }

        @Override
        protected void renderLabels(PoseStack pose, int p_97809_, int p_97810_) {
            super.renderLabels(pose, p_97809_, p_97810_);

            draw(pose, "items", -20 + 2);
            draw(pose, "mb", 70 + 5 + 2);
        }

        public void draw(PoseStack pose, String str, int xOffset) {
            this.font.draw(pose, str, (imageWidth) / 2F + xOffset, (this.imageHeight - font.lineHeight) / 2F, 4210752);
        }

        @Override
        protected void containerTick() {
            super.containerTick();

            itemRation.tick();
        }

        @Override
        public void onClose() {
            TPPackets.REGULATE_RATION.accept(toInt(itemRation.getValue()), toInt(liquidRation.getValue()));
            super.onClose();
        }

        public int toInt(String str) {
            return str.isEmpty() ? 0 : Integer.parseInt(str);
        }

        public boolean checkStr(String str) {
            try {
                toInt(str);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
