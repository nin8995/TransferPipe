package nin.transferpipe.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import nin.transferpipe.item.RationingUpgradeItem;

public class RegulateRationPacket extends BasePacket.arg2<Integer, Integer> {

    private int itemRation;
    private int liquidRation;

    @Override
    public FriendlyByteBuf encode(FriendlyByteBuf buf) {
        buf.writeInt(itemRation);
        buf.writeInt(liquidRation);
        return buf;
    }

    @Override
    public BasePacket decode(FriendlyByteBuf buf) {
        itemRation = buf.readInt();
        liquidRation = buf.readInt();
        return this;
    }

    @Override
    public BasePacket init(Integer integer, Integer integer2) {
        itemRation = integer;
        liquidRation = integer2;
        return this;
    }

    @Override
    public void handleOnServer(ServerPlayer sp) {
        var slot = sp.getInventory().selected;
        var item = sp.getInventory().getItem(slot);
        if (item.getItem() instanceof RationingUpgradeItem.Regulatable regulatable) {
            regulatable.setRations(item, itemRation, liquidRation);//なのでここでもタグ作る必要
            sp.getInventory().setItem(slot, item);
        }
    }
}
