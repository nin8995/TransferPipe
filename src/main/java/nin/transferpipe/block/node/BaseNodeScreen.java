package nin.transferpipe.block.node;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nin.transferpipe.gui.BaseScreen;
import nin.transferpipe.util.forge.ForgeUtils;

public abstract class BaseNodeScreen<T extends BaseNodeMenu> extends BaseScreen<T> {

    public int customLabelY;

    public BaseNodeScreen(T p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        this.titleLabelY = 7;
    }

    @Override
    protected void renderLabels(PoseStack pose, int p_97809_, int p_97810_) {
        super.renderLabels(pose, p_97809_, p_97810_);
        customLabelY = 27;
        drawCenteredTexts(pose);
    }

    public void drawCenteredTexts(PoseStack pose) {
        if (menu.isSearching()) {
            drawCentered(pose, Component.translatable(getSearchMsg()));
            var pos = menu.getSearchPos();
            drawCentered(pose, Component.translatable("gui.transferpipe.searching_pos", pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    public abstract String getSearchMsg();

    public void drawCentered(PoseStack pose, Component c) {
        drawCentered(pose, c.getString());
    }

    public void drawCentered(PoseStack pose, String str) {
        this.font.draw(pose, str, (imageWidth - font.width(str)) / 2F, customLabelY, 4210752);
        customLabelY += 10;
    }

    public static class Item<T extends BaseNodeMenu.Item> extends BaseNodeScreen<T> {

        public Item(T p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        public String getSearchMsg() {
            return "gui.transferpipe.searching_item";
        }
    }

    public static class Liquid<T extends BaseNodeMenu.Liquid> extends BaseNodeScreen<T> {

        public Liquid(T p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        public String getSearchMsg() {
            return "gui.transferpipe.searching_liquid";
        }

        @Override
        public void drawCenteredTexts(PoseStack pose) {
            var liquid = menu.liquidSlot.getLiquid();
            if (!liquid.isEmpty())
                drawCentered(pose, Component.translatable("gui.transferpipe.liquid_amount",
                        ForgeUtils.toMilliBucket(liquid.getAmount()), liquid.getDisplayName()));
            super.drawCenteredTexts(pose);
        }
    }

    public static class Energy<T extends BaseNodeMenu.Energy> extends BaseNodeScreen<T> {

        public Energy(T p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        public String getSearchMsg() {
            return "gui.transferpipe.searching_energy";
        }

        @Override
        public void drawCenteredTexts(PoseStack pose) {
            customLabelY -= 7;

            var energy = menu.getEnergy();
            if (energy != 0)
                drawCentered(pose, Component.translatable("gui.transferpipe.energy_amount", ForgeUtils.toFE(energy)));

            var extract = menu.getExtract();
            var insert = menu.getInsert();
            var both = menu.getBoth();
            if (!(extract == 0 && insert == 0 && both == 0)) {
                drawCentered(pose, Component.translatable("gui.transferpipe.connection"));
                drawCentered(pose, Component.translatable("gui.transferpipe.connection_amounts", extract, insert, both));
            }

            var energyReceiverPipes = menu.getEnergyReceiverPipes();
            if (energyReceiverPipes != 0)
                drawCentered(pose, Component.translatable("gui.transferpipe.energy_receiver_pipe_amount", energyReceiverPipes));

            super.drawCenteredTexts(pose);
        }
    }
}
