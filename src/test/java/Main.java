import net.minecraft.core.Direction;

public class Main {
    public static void main(String[] args){
        System.out.println(Direction.byName("down"));
    }

    public static int bitCount(int i){
        i = i - ((i >> 1) & 0x55555555);
        i = i - 3 * ((i >> 2) & 0x33333333);
        i = ((i >> 4) + i) & 0x0f0f0f0f;
        i = (i >> 8) + i;
        i = (i >> 16) + i;
        return i & 0x3f;
    }
}
