package nin.transferpipe.item.upgrade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nin.transferpipe.util.java.ExceptionPredicate;
import nin.transferpipe.util.minecraft.BaseMenu;
import nin.transferpipe.util.minecraft.BaseScreen;

public abstract class BaseRegulatableScreen<T extends BaseMenu> extends BaseScreen<T> {

    public EditBox itemRation;
    public EditBox liquidRation;

    public BaseRegulatableScreen(T p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
    }

    public EditBox newEditBox(int width, int height, int xOffset) {
        return new EditBox(font, (this.width - width) / 2 + xOffset, (this.height - height) / 2, width, height, Component.empty());
    }

    @Override
    protected void init() {
        super.init();
        itemRation = newEditBox(70, 10, -35 - 20);
        itemRation.setFilter(this::checkStr);
        itemRation.setValue(String.valueOf(getRation().getFirst()));//もしタグがない場合、クライアントでtag作ってるだけ、サーバーはまだタグ無し
        addRenderableWidget(itemRation);

        liquidRation = newEditBox(70, 10, 35 + 8);
        liquidRation.setFilter(this::checkStr);
        liquidRation.setValue(String.valueOf(getRation().getSecond()));
        addRenderableWidget(liquidRation);
    }

    public abstract Pair<Integer, Integer> getRation();

    @Override
    protected void renderLabels(PoseStack pose, int p_97809_, int p_97810_) {
        super.renderLabels(pose, p_97809_, p_97810_);

        draw(pose, "items", -20 + 2);
        draw(pose, "mb", 70 + 8 + 2);
    }

    public void draw(PoseStack pose, String str, int xOffset) {
        this.font.draw(pose, str, (imageWidth) / 2F + xOffset, (this.imageHeight - font.lineHeight) / 2F, 4210752);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        itemRation.tick();
        liquidRation.tick();
    }

    public int toInt(String str) {
        return str.isEmpty() ? 0 : Integer.parseInt(str);
    }

    public boolean checkStr(String str) {
        return ExceptionPredicate.succeeded(() -> toInt(str));
    }
}
