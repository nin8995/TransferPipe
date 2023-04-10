package nin.transferpipe.block.node;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import nin.transferpipe.util.TPUtils;

public abstract class ScreenTransferNode<T extends MenuTransferNode> extends AbstractContainerScreen<T> {

    private static final ResourceLocation BG = TPUtils.modLoc("textures/gui/container/transfer_node.png");

    public int customLabelY;

    public ScreenTransferNode(T p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        this.inventoryLabelY = 114514;
        this.titleLabelY = -23;
    }

    @Override
    public void render(PoseStack p_97795_, int p_97796_, int p_97797_, float p_97798_) {
        renderBackground(p_97795_);
        super.render(p_97795_, p_97796_, p_97797_, p_97798_);
        renderTooltip(p_97795_, p_97796_, p_97797_);
    }

    @Override
    protected void renderBg(PoseStack pose, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.setShaderTexture(0, BG);
        blit(pose, this.leftPos, (this.height - 224) / 2, 0, 0, 176, 225);
    }

    @Override
    protected void renderLabels(PoseStack pose, int p_97809_, int p_97810_) {
        super.renderLabels(pose, p_97809_, p_97810_);
        customLabelY = -3;
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

    public static class Item extends ScreenTransferNode<MenuTransferNode.Item> {

        public Item(MenuTransferNode.Item p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        public String getSearchMsg() {
            return "gui.transferpipe.searching_item";
        }
    }

    public static class Liquid extends ScreenTransferNode<MenuTransferNode.Liquid> {

        public Liquid(MenuTransferNode.Liquid p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

        @Override
        protected void renderLabels(PoseStack pose, int p_97809_, int p_97810_) {
            super.renderLabels(pose, p_97809_, p_97810_);
            var liquid = menu.getLiquid();

            if (!liquid.isEmpty())
                TPUtils.renderLiquid(liquid, pose, 80, -38 + MenuTransferNode.upgradesY, 16);

        }

        @Override
        public String getSearchMsg() {
            return "gui.transferpipe.searching_liquid";
        }

        @Override
        public void drawCenteredTexts(PoseStack pose) {
            var liquid = menu.getLiquid();
            if (!liquid.isEmpty())
                drawCentered(pose, Component.translatable("gui.transferpipe.liquid_amount",
                        TPUtils.toMilliBucket(liquid.getAmount()), liquid.getDisplayName()));
            super.drawCenteredTexts(pose);
        }
    }

    public static class Energy extends ScreenTransferNode<MenuTransferNode.Energy> {

        public Energy(MenuTransferNode.Energy p_97741_, Inventory p_97742_, Component p_97743_) {
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
                drawCentered(pose, Component.translatable("gui.transferpipe.energy_amount", TPUtils.toFE(energy)));

            var extractables = menu.getExtractables();
            var receivables = menu.getReceivables();
            var both = menu.getBoth();
            if (!(extractables == 0 && receivables == 0 && both == 0)) {
                drawCentered(pose, Component.translatable("gui.transferpipe.connection"));
                drawCentered(pose, Component.translatable("gui.transferpipe.connection_amounts", extractables, receivables, both));
            }

            var energyReceiverPipes = menu.getEnergyReceiverPipes();
            if (energyReceiverPipes != 0)
                drawCentered(pose, Component.translatable("gui.transferpipe.energy_receiver_pipe_amount", energyReceiverPipes));

            super.drawCenteredTexts(pose);
        }
    }
}
