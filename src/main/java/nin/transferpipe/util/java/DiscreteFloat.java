package nin.transferpipe.util.java;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class DiscreteFloat implements INBTSerializable<CompoundTag> {

    public int step;

    public float baseValue;
    public float valPerStep;
    public int maxStep;

    public DiscreteFloat(float baseValue, float valPerStep, int maxStep) {
        this.baseValue = baseValue;
        this.valPerStep = valPerStep;
        this.maxStep = maxStep;
    }

    public void proceed(boolean reverse) {
        if (!reverse && step >= maxStep)
            step = 0;
        if (reverse && step <= 0)
            step = maxStep;
        else
            step += reverse ? -1 : 1;
    }

    public float value() {
        return baseValue + valPerStep * step;
    }

    public static String STEP = "Step";

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putInt(STEP, step);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(STEP))
            step = tag.getInt(STEP);
    }
}
