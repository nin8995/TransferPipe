package nin.transferpipe.util.transferpipe;

public enum RedstoneBehavior {
    ACTIVE_LOW,
    ACTIVE_HIGH,
    ALWAYS,
    NEVER;

    public boolean isActive(int signal) {
        return switch (this) {
            case ACTIVE_LOW -> signal == 0;
            case ACTIVE_HIGH -> signal != 0;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }
}
