import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nin.transferpipe.block.node.TileTransferNodeItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class Main {

    public static void main(String[] args) {
        var a = IntStream.of(10).boxed().toList();
        ;
        System.out.println(a.stream().map(i -> i*19).toList());
        System.out.println(a);
    }/*

    public void tryPull(Container container, Direction dir) {
        forFirstPullableSlot(container, dir, slot ->
                receive(container.removeItem(slot, getPullAmount(container.getItem(slot), false))));
    }

    public void forFirstPullableSlot(Container container, Direction dir, IntConsumer func) {
        getSlots(container, dir)
                .filter(slot -> !(container instanceof WorldlyContainer wc && !wc.canTakeItemThroughFace(slot, wc.getItem(slot), dir)))
                .filter(slot -> shouldPull(container.getItem(slot)))
                .findFirst().ifPresent(func);
    }

    //この方角から参照できるスロット番号のstream(WorldlyContainerは方角毎のItemHandlerを持たず、ただ内部コンテナを持っていて、その方角から参照できるコンテナ上のスロット番号を返す。)
    public static IntStream getSlots(Container container, Direction dir) {
        return container instanceof WorldlyContainer wc ? IntStream.of(wc.getSlotsForFace(dir)) : IntStream.range(0, container.getContainerSize());
    }*/
/*

    public void tryPush(Container container, Direction dir) {
        if (canInsert(container, dir))
            setItemSlot(insert(container, dir, false));
    }

    public boolean canInsert(Container container, Direction dir) {
        return getItemSlot() != insert(container, dir, true);
    }

    public ItemStack insert(Container container, Direction dir, boolean simulate) {
        var remainder = getPushableItem(container);
        var ration = remainder.getCount();

        for (int slot : getSlots(container, dir)
                .filter(slot -> container.canPlaceItem(slot, remainder)
                        && !(container instanceof WorldlyContainer wc && !wc.canPlaceItemThroughFace(slot, remainder, dir)))
                .toArray()) {
            var item = container.getItem(slot).copy();

            if (shouldAdd(remainder, item)) {
                int addableAmount = item.isEmpty() ? remainder.getMaxStackSize() : item.getMaxStackSize() - item.getCount();
                int addedAmount = Math.min(addableAmount, remainder.getCount());

                if (item.isEmpty()) {
                    var ageea = remainder.copy();
                    ageea.setCount(addedAmount);
                    item = ageea;
                } else {
                    item.setCount(item.getCount() + addedAmount);
                }

                if (!simulate)
                    container.setItem(slot, item);

                remainder.setCount(remainder.getCount() - addedAmount);
            }

            if (remainder.isEmpty())
                break;
        }

        //同じならcopy前のインスタンスを返す（IItemHandler.insertItemと同じ仕様。ItemStack.equalsが多田野参照評価なため、同値性を求める文脈で渡しておいて同一性と同値性を一致させておくが吉）
        if (remainder.getCount() == ration)
            return getItemSlot();
        else {
            var notRationedItem = getItemSlot().copy();
            var notRationedAmount = getItemSlot().getCount() - ration;
            notRationedItem.setCount(remainder.getCount() + notRationedAmount);
            return notRationedItem;
        }
    }*/
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
