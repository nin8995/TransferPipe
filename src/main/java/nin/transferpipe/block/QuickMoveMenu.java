package nin.transferpipe.block;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class QuickMoveMenu extends AbstractContainerMenu {

    public int containerStart;
    public int containerEnd;
    public int inventoryStart;
    public int inventoryEnd;
    public int hotbarStart;
    public int hotbarEnd;

    protected QuickMoveMenu(@Nullable MenuType<?> p_38851_, int p_38852_) {
        super(p_38851_, p_38852_);
    }

    //返り値がEmptyかスロットのものと違う種類になるまで呼ばれ続ける
    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        var quickMovedSlot = this.slots.get(quickMovedSlotIndex);
        if (quickMovedSlot.hasItem()) {
            var item = quickMovedSlot.getItem();

            if (quickMovedSlotIndex <= containerEnd) {//コンテナスロットなら
                if(!moveItemTo(item, inventoryStart, hotbarEnd, true))//インベントリに差して
                    return ItemStack.EMPTY;//差せなければ終わり
            } else if (quickMovedSlotIndex <= hotbarEnd) {//インベントリスロットなら
                //まず優先度の高いコンテナスロットに差してから
                var highPriorities = getHighPriorityContainerSlots(item);
                var slotChanged = false;
                if (highPriorities != null)
                    slotChanged = moveItemTo(item, highPriorities.getFirst(), highPriorities.getSecond(), false);

                //普通にコンテナに差す
                if (!moveItemTo(item, containerStart, containerEnd, false)) {
                    //コンテナに空きなければインベントリ<=>ホットバーで差しあう
                    if (quickMovedSlotIndex <= inventoryEnd) {
                        if (!moveItemTo(item, hotbarStart, hotbarEnd, false) && !slotChanged)
                            return ItemStack.EMPTY;//それでも空きがなくて、高優先度スロットでも変化が無ければ
                    } else if (!moveItemTo(item, inventoryStart, inventoryEnd, false) && !slotChanged)
                        return ItemStack.EMPTY;//終わり
                }
            }

            //スロットが変わった
            quickMovedSlot.onTake(player, item);//ここに入れるitemは種類だけでいい。何個削られたかはそれぞれのslotが勝手にremoveCountを記録している。
            //return item;//まだ残りがあってスロットにも空きがあるからitemを、、、って、そんな状況ないんですけどー..
        }

        return ItemStack.EMPTY;//一回の動作で終わらす
    }

    //少しでも挿入できたか
    public boolean moveItemTo(ItemStack item, int minSlot, int maxSlot, boolean fillMaxToMin) {
        return moveItemStackTo(item, minSlot, maxSlot + 1, fillMaxToMin);
    }

    public abstract Pair<Integer, Integer> getHighPriorityContainerSlots(ItemStack item);
}
