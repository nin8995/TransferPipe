package nin.transferpipe.gui;

import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class BaseMenu extends AbstractContainerMenu {

    public Inventory inv;

    //screen info
    public String bg;
    public int bgHeight;

    public BaseMenu(RegistryGUI registry, int id, Inventory inv, String bg, int bgHeight) {
        super(registry.menu(), id);
        this.bg = bg;
        this.inv = inv;
        this.bgHeight = bgHeight;
    }

    public boolean noInventory() {
        return false;
    }

    public boolean noInventoryText() {
        return noInventory();
    }

    public boolean noTitleText() {
        return false;
    }

    public void addInventory() {
        //hotbar
        addInventorySlots(0, inv, Slot::new, 58);

        //inventory
        IntStream.rangeClosed(0, 2).forEach(i ->
                addInventorySlots(9 * (i + 1), inv, Slot::new, i * 18));
    }

    public <T extends Container> void addInventorySlots(int start, T inv, Function4<T, Integer, Integer, Integer, Slot> slotConstructor, int y) {
        addCenteredSlots(start, start + 8, (index, x) -> slotConstructor.apply(inv, index, x, y + getOffsetY()));
    }

    public <T extends IItemHandler> void addItemHandlerSlots(T handler, Function4<T, Integer, Integer, Integer, Slot> slotConstructor, int y) {
        addItemHandlerSlots(handler, 0, handler.getSlots() - 1, slotConstructor, y);
    }

    public <T extends IItemHandler> void addItemHandlerSlots(T handler, int start, int end, Function4<T, Integer, Integer, Integer, Slot> slotConstructor, int y) {
        addCenteredSlots(start, end, (index, x) -> slotConstructor.apply(handler, index, x, y));
    }

    public <T extends Container> void addContainerSlots(T container, Function4<T, Integer, Integer, Integer, Slot> slotConstructor, int y) {
        addCenteredSlots(0, container.getContainerSize() - 1, (index, x) -> slotConstructor.apply(container, index, x, y));
    }

    //slot info
    public int hotbarStart = 0;
    public int hotbarEnd = hotbarStart + 8;
    public int inventoryStart = hotbarEnd + 1;
    public int inventoryEnd = inventoryStart + 26;
    public int containerStart = inventoryEnd + 1;
    public int containerEnd;

    public void addCenteredSlots(int start, int end, BiFunction<Integer, Integer, Slot> slotConstructor) {
        var slotInfo = slotConstructor.apply(0, 0);

        var slotAmount = end - start + 1;
        var slotSize = 18;
        var startX = 8 + slotSize * (9 - slotAmount) / 2 + getOffsetX();

        IntStream.range(0, slotAmount).forEach(i -> {
            var index = i + start;
            var x = startX + slotSize * i;
            var slot = slotConstructor.apply(index, x);

            addSlot(shouldLock(slotInfo, index) ? new LockedSlot(slot) : slot);
        });

        if (!(slotInfo.container instanceof Inventory))
            addContainerEnd(slotAmount);
    }

    public void addContainerEnd(int add) {
        containerEnd = containerEnd != 0 ? containerEnd + add : containerStart + add - 1;
    }

    public int getOffsetX() {
        return 0;
    }

    public int getOffsetY() {//TODO このオフセットはインベントリの始点を表す。インベントリの始点なんて画像の下から数えれば固定だから、自動化死体
        return bgHeight - 82;
    }

    public boolean shouldLock(Slot info, int index) {
        return false;
    }

    //返り値がEmptyかスロットのものと違う種類になるまで呼ばれ続ける
    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        var quickMovedSlot = this.slots.get(quickMovedSlotIndex);
        if (quickMovedSlot.hasItem()) {
            var item = quickMovedSlot.getItem();

            if (containerStart <= quickMovedSlotIndex && quickMovedSlotIndex <= containerEnd) {//コンテナスロットなら
                if (!moveItemTo(item, hotbarStart, inventoryEnd, true))//インベントリに差して
                    return ItemStack.EMPTY;//差せなければ終わり
                else
                    sendContentsChanged(Stream.of(quickMovedSlot));//差せたら更新
            } else if (hotbarStart <= quickMovedSlotIndex && quickMovedSlotIndex <= inventoryEnd) {//インベントリスロットなら
                //まず優先度の高いコンテナスロットに差してから
                var highPriorities = getHighPriorityContainerSlots(item);
                var slotChanged = false;
                if (highPriorities != null)
                    slotChanged = moveItemTo(item, highPriorities.getFirst(), highPriorities.getSecond(), false);

                //普通にコンテナに差す
                if (!moveItemTo(item, containerStart, containerEnd, false)) {
                    //コンテナに空きなければインベントリ<=>ホットバーで差しあう
                    if (quickMovedSlotIndex <= hotbarEnd) {
                        if (!moveItemTo(item, inventoryStart, inventoryEnd, false) && !slotChanged)
                            return ItemStack.EMPTY;//それでも空きがなくて、高優先度スロットでも変化が無ければ
                    } else if (!moveItemTo(item, hotbarStart, hotbarEnd, false) && !slotChanged)
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
        var slotsChanged = moveItemStackTo(item, minSlot, maxSlot + 1, fillMaxToMin);//safeInsertはしないけどmayPlaceは見て入れる

        if (slotsChanged)/*SlotItemHandlerから取られたアイテムは本来加工してはいけないが*/
            sendContentsChanged(IntStream.rangeClosed(minSlot, maxSlot).mapToObj(slots::get));

        return slotsChanged;
    }

    public void sendContentsChanged(Stream<Slot> slot) {
        slot
                .flatMap(filterType(SlotItemHandler.class))
                .map(SlotItemHandler::getItemHandler)
                .flatMap(filterType(ItemStackHandler.class))
                .findFirst().ifPresent(inventory ->
                        inventory.setStackInSlot(0, inventory.getStackInSlot(0)));//適当に何もしない更新をかけることで、このmodにおいては大丈夫
    }

    public static <Super, Sub extends Super> Function<Super, Stream<Sub>> filterType(Class<Sub> clz) {
        return obj -> clz.isInstance(obj) ? Stream.of(clz.cast(obj)) : Stream.empty();
    }

    public Pair<Integer, Integer> getHighPriorityContainerSlots(ItemStack item) {
        return null;
    }
}

