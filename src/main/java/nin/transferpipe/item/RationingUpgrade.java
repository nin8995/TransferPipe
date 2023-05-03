package nin.transferpipe.item;

import net.minecraft.world.item.ItemStack;

public class RationingUpgrade extends FunctionUpgrade {

    private final int ration;

    public RationingUpgrade(int ration, Properties p_41383_) {
        super(p_41383_);
        this.ration = ration;
    }

    public int getItemRation(ItemStack item) {
        return ration;
    }

    public int getLiquidRation(ItemStack item) {
        return ration * 250;
    }
}
