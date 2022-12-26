package nin.transferpipe.block.property;


import net.minecraft.util.StringRepresentable;

public enum ConnectionStates implements StringRepresentable {
    NONE,
    PIPE,
    MACHINE;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}
