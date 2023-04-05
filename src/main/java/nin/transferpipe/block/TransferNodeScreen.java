package nin.transferpipe.block;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import nin.transferpipe.util.TPUtils;

public abstract class TransferNodeScreen<T extends TransferNodeMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation BG = TPUtils.modLoc("textures/gui/container/transfer_node.png");

    private int customLabelY = 34;

    public TransferNodeScreen(T p_97741_, Inventory p_97742_, Component p_97743_) {
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
        if (menu.isSearching()) {
            drawCentered(pose, Component.translatable("gui.transferpipe.searching").getString());
            drawCentered(pose, menu.getSearchPosMsg());
        }
    }

    public void drawCentered(PoseStack pose, String str) {
        this.font.draw(pose, str, (imageWidth - font.width(str)) / 2F, customLabelY, 4210752);
        customLabelY += 10;
    }

    public static class Item extends TransferNodeScreen<TransferNodeMenu.Item> {

        public Item(TransferNodeMenu.Item p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }

    }
}
