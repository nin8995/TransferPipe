package nin.transferpipe.network;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import nin.transferpipe.TPMod;
import nin.transferpipe.block.pipe.RegulatableRationingPipe;
import nin.transferpipe.item.upgrade.RegulatableRationingUpgrade;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public interface TPPackets {

    String PROTOCOL_VERSION = "1";
    SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(TPMod.loc("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    AtomicInteger i = new AtomicInteger(0);

    Consumer3<Integer, Integer, Integer> REGULATE_RATION_UPGRADE = sendToServer3(register(RegulatableRationingUpgrade.Packet::new));
    Consumer3<BlockPos, Integer, Integer> REGULATABLE_RATION_PIPE = sendToServer3(register(RegulatableRationingPipe.Packet::new));

    static void init() {
        //load static
        register(CurveParticlePacket::new);
    }

    static <T extends BasePacket> Supplier<T> register(Supplier<T> packetSup) {
        var p = packetSup.get();
        INSTANCE.registerMessage(i.getAndIncrement(), (Class<T>) p.getClass(), BasePacket::encode, (buf) -> {
            p.decode(buf);
            return p;
        }, BasePacket::handle);
        return packetSup;
    }

    static <A, T extends BasePacket.arg1<A>> Function<A, T> func1(Supplier<T> packetSup) {
        return a -> (T) packetSup.get().init(a);
    }

    static <A, B, T extends BasePacket.arg2<A, B>> BiFunction<A, B, T> func2(Supplier<T> packetSup) {
        return (a, b) -> (T) packetSup.get().init(a, b);
    }

    static <A, B, C, T extends BasePacket.arg3<A, B, C>> Function3<A, B, C, T> func3(Supplier<T> packetSup) {
        return (a, b, c) -> (T) packetSup.get().init(a, b, c);
    }

    static <A, B, C, D, T extends BasePacket.arg4<A, B, C, D>> Function4<A, B, C, D, T> func4(Supplier<T> packetSup) {
        return (a, b, c, d) -> (T) packetSup.get().init(a, b, c, d);
    }

    static <A, B, C, D, E, T extends BasePacket.arg5<A, B, C, D, E>> Function5<A, B, C, D, E, T> func5(Supplier<T> packetSup) {
        return (a, b, c, d, e) -> (T) packetSup.get().init(a, b, c, d, e);
    }

    static <A, B, C, D, E, F, T extends BasePacket.arg6<A, B, C, D, E, F>> Function6<A, B, C, D, E, F, T> func6(Supplier<T> packetSup) {
        return (a, b, c, d, e, f) -> (T) packetSup.get().init(a, b, c, d, e, f);
    }

    static <A, T extends BasePacket.arg1<A>> Consumer<A> sendToServer1(Supplier<T> packetSup) {
        return a -> func1(packetSup).apply(a).toServer();
    }

    static <A, B, T extends BasePacket.arg2<A, B>> BiConsumer<A, B> sendToServer2(Supplier<T> packetSup) {
        return (a, b) -> func2(packetSup).apply(a, b).toServer();
    }

    static <A, B, C, T extends BasePacket.arg3<A, B, C>> Consumer3<A, B, C> sendToServer3(Supplier<T> packetSup) {
        return (a, b, c) -> func3(packetSup).apply(a, b, c).toServer();
    }

    static <A, B, C, D, T extends BasePacket.arg4<A, B, C, D>> Consumer4<A, B, C, D> sendToServer4(Supplier<T> packetSup) {
        return (a, b, c, d) -> func4(packetSup).apply(a, b, c, d).toServer();
    }

    static <A, B, C, D, E, T extends BasePacket.arg5<A, B, C, D, E>> Consumer5<A, B, C, D, E> sendToServer5(Supplier<T> packetSup) {
        return (a, b, c, d, e) -> func5(packetSup).apply(a, b, c, d, e).toServer();
    }

    static <A, B, C, D, E, F, T extends BasePacket.arg6<A, B, C, D, E, F>> Consumer6<A, B, C, D, E, F> sendToServer6(Supplier<T> packetSup) {
        return (a, b, c, d, e, f) -> func6(packetSup).apply(a, b, c, d, e, f).toServer();
    }

    interface Consumer3<A, B, C> {
        void accept(A a, B b, C c);
    }

    interface Consumer4<A, B, C, D> {
        void accept(A a, B b, C c, D d);
    }

    interface Consumer5<A, B, C, D, E> {
        void accept(A a, B b, C c, D d, E e);
    }

    interface Consumer6<A, B, C, D, E, F> {
        void accept(A a, B b, C c, D d, E e, F f);
    }
}
