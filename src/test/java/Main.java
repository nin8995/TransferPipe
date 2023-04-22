import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nin.transferpipe.block.node.TileTransferNodeItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        System.out.println(TileTransferNodeItem.relativeInventoryPositions.get(Direction.UP));
    }

/*
    var map = new HashMap<Integer, Integer>();
        map.put(1,1);
        map.put(2,2);
        map.forEach((i1, i2) -> map.put(i1, ++i2));
        System.out.println(map);*/
/*
    var map = new LinkedHashMap<String, Integer>();
        map.put("ha", 1);
        map.put("hea", 2);
        map.put("hae", 3);

    var key = map.keySet().iterator().next();
        System.out.println(map.get(key));
        map.remove(key);

    key = map.keySet().iterator().next();
        System.out.println(map.get(key));
        map.remove(key);

    key = map.keySet().iterator().next();
        System.out.println(map.get(key));
        map.remove(key);

        if(map.keySet().iterator().hasNext()) {
        key = map.keySet().iterator().next();
        System.out.println(map.get(key));
        map.remove(key);
    }*/

    /*Player player;
        int slotIndex;
        Slot  slot;

        ItemStack item = this.quickMoveStack(player, slotIndex);
        while(!item.isEmpty() && ItemStack.isSame(slot.getItem(), item))
            item = this.quickMoveStack(player, slotIndex);*/

    public static int bitCount(int i) {
        i = i - ((i >> 1) & 0x55555555);
        i = i - 3 * ((i >> 2) & 0x33333333);
        i = ((i >> 4) + i) & 0x0f0f0f0f;
        i = (i >> 8) + i;
        i = (i >> 16) + i;
        return i & 0x3f;
    }
}
