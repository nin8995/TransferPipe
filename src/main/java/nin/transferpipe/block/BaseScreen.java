package nin.transferpipe.block;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import nin.transferpipe.BaseMenu;
import nin.transferpipe.util.TPUtils;

public class BaseScreen<T extends BaseMenu> extends AbstractContainerScreen<T> {

    private final ResourceLocation bg;
    private final int bgWidth;
    private final int bgHeight;

    public BaseScreen(T menu, Inventory p_97742_, Component p_97743_) {
        super(menu, p_97742_, p_97743_);
        this.bg = TPUtils.modLoc("textures/gui/container/" + menu.bg + ".png");
        var image = TPUtils.getImage(this.bg);
        this.bgWidth = image.getWidth();
        this.bgHeight = image.getHeight();
        imageWidth = bgWidth;
        imageHeight = bgHeight;

        titleLabelX = menu.disableTitleText ? 114514 : 7;
        titleLabelY = 5;
        inventoryLabelX = menu.disableInventoryText ? 1919810 : 7;
        inventoryLabelY = getMenu().getOffsetY() - 11;
    }

    @Override
    public void render(PoseStack p_97795_, int p_97796_, int p_97797_, float p_97798_) {
        renderBackground(p_97795_);
        super.render(p_97795_, p_97796_, p_97797_, p_97798_);
        renderTooltip(p_97795_, p_97796_, p_97797_);
    }

    @Override
    protected void renderBg(PoseStack pose, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.setShaderTexture(0, bg);
        blit(pose, (width - bgWidth) / 2, (height - bgHeight) / 2, 0, 0, bgWidth, bgHeight, bgWidth, bgHeight);
    }
}
