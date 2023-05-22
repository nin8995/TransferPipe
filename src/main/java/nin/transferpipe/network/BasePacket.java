package nin.transferpipe.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public abstract class BasePacket {

    public abstract FriendlyByteBuf encode(FriendlyByteBuf buf);

    public abstract BasePacket decode(FriendlyByteBuf buf);

    public void handle(Supplier<NetworkEvent.Context> c) {
        switch (c.get().getDirection()) {
            case PLAY_TO_SERVER -> c.get().enqueueWork(() -> handleOnServer(c.get().getSender()));
            case PLAY_TO_CLIENT -> c.get().enqueueWork(() -> handleOnClient(Minecraft.getInstance().player));
        }

        c.get().setPacketHandled(true);
    }

    public void handleOnServer(ServerPlayer sp) {
    }

    public void handleOnClient(Player p) {
    }

    public void toClient(ServerPlayer sp) {
        TPPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), this);
    }

    public void toClients(List<ServerPlayer> sps) {
        sps.forEach(sp -> TPPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), this));
    }

    public void toServer() {
        TPPackets.INSTANCE.sendToServer(this);
    }

    public static abstract class arg1<A> extends BasePacket {

        public abstract BasePacket init(A a);
    }

    public static abstract class arg2<A, B> extends BasePacket {

        public abstract BasePacket init(A a, B b);
    }

    public static abstract class arg3<A, B, C> extends BasePacket {

        public abstract BasePacket init(A a, B b, C c);
    }

    public static abstract class arg4<A, B, C, D> extends BasePacket {

        public abstract BasePacket init(A a, B b, C c, D d);
    }

    public static abstract class arg5<A, B, C, D, E> extends BasePacket {

        public abstract BasePacket init(A a, B b, C c, D d, E e);
    }

    public static abstract class arg6<A, B, C, D, E, F> extends BasePacket {

        public abstract BasePacket init(A a, B b, C c, D d, E e, F f);
    }
}
