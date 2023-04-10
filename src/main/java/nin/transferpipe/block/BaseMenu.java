package nin.transferpipe.block;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;

    public int inventoryStart = 0;
    public int inventoryEnd = inventoryStart + 26;
    public int hotbarStart = inventoryEnd + 1;
    public int hotbarEnd = hotbarStart + 8;
    public int containerStart = hotbarEnd + 1;
    public int containerEnd;

    protected BaseMenu(@Nullable MenuType<?> p_38851_, int p_38852_, Inventory inv, ContainerLevelAccess access) {
        super(p_38851_, p_38852_);
        this.access = access;

        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inv, k + l * 9 + 9, 8 + k * 18, l * 18 + 22 + getOffsetY()));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(inv, i1, 8 + i1 * 18, 80 + getOffsetY()));
        }
    }

    public abstract int getOffsetY();

    //返り値がEmptyかスロットのものと違う種類になるまで呼ばれ続ける
    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        var quickMovedSlot = this.slots.get(quickMovedSlotIndex);
        if (quickMovedSlot.hasItem()) {
            var item = quickMovedSlot.getItem();

            if (containerStart <= quickMovedSlotIndex && quickMovedSlotIndex <= containerEnd) {//コンテナスロットなら
                if (!moveItemTo(item/*SlotItemHandlerから取られたアイテムは本来加工してはいけないが*/, inventoryStart, hotbarEnd, true))//インベントリに差して
                    return ItemStack.EMPTY;//差せなければ終わり
                else if (quickMovedSlot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() instanceof ItemStackHandler handler)
                    handler.setStackInSlot(0, handler.getStackInSlot(0));//適当に何もしない更新をかけることで、このmodにおいては大丈夫
            } else if (inventoryStart <= quickMovedSlotIndex && quickMovedSlotIndex <= hotbarEnd) {//インベントリスロットなら
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
            } else {//範囲外なら
                return ItemStack.EMPTY;//終わり
            }

            //スロットが変わった
            quickMovedSlot.setChanged();
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

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, getBlock());
    }

    public abstract Block getBlock();

}
