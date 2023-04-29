package nin.transferpipe.network;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.function.*;

public class TPPackets {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(TPUtils.modLoc("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int i = 0;

    public static BiConsumer<Integer, Integer> REGULATE_RATION = sendToServer2(register(RegulateRationPacket::new));

    public static void init() {

    }

    public static <T extends BasePacket> Supplier<T> register(Supplier<T> packetSup) {
        var p = packetSup.get();
        INSTANCE.registerMessage(i++, (Class<T>) p.getClass(), BasePacket::encode, (buf) -> {
            p.decode(buf);
            return p;
        }, BasePacket::handle);
        return packetSup;
    }

    private static <A, T extends BasePacket.arg1<A>> Function<A, T> func1(Supplier<T> packetSup) {
        return a -> (T) packetSup.get().init(a);
    }

    private static <A, B, T extends BasePacket.arg2<A, B>> BiFunction<A, B, T> func2(Supplier<T> packetSup) {
        return (a, b) -> (T) packetSup.get().init(a, b);
    }

    private static <A, B, C, T extends BasePacket.arg3<A, B, C>> Function3<A, B, C, T> func3(Supplier<T> packetSup) {
        return (a, b, c) -> (T) packetSup.get().init(a, b, c);
    }

    private static <A, B, C, D, T extends BasePacket.arg4<A, B, C, D>> Function4<A, B, C, D, T> func4(Supplier<T> packetSup) {
        return (a, b, c, d) -> (T) packetSup.get().init(a, b, c, d);
    }

    private static <A, B, C, D, E, T extends BasePacket.arg5<A, B, C, D, E>> Function5<A, B, C, D, E, T> func5(Supplier<T> packetSup) {
        return (a, b, c, d, e) -> (T) packetSup.get().init(a, b, c, d, e);
    }

    private static <A, B, C, D, E, F, T extends BasePacket.arg6<A, B, C, D, E, F>> Function6<A, B, C, D, E, F, T> func6(Supplier<T> packetSup) {
        return (a, b, c, d, e, f) -> (T) packetSup.get().init(a, b, c, d, e, f);
    }

    private static <A, T extends BasePacket.arg1<A>> Consumer<A> sendToServer1(Supplier<T> packetSup) {
        return a -> func1(packetSup).apply(a).toServer();
    }

    private static <A, B, T extends BasePacket.arg2<A, B>> BiConsumer<A, B> sendToServer2(Supplier<T> packetSup) {
        return (a, b) -> func2(packetSup).apply(a, b).toServer();
    }

    private static <A, B, C, T extends BasePacket.arg3<A, B, C>> Consumer3<A, B, C> sendToServer3(Supplier<T> packetSup) {
        return (a, b, c) -> func3(packetSup).apply(a, b, c).toServer();
    }

    private static <A, B, C, D, T extends BasePacket.arg4<A, B, C, D>> Consumer4<A, B, C, D> sendToServer4(Supplier<T> packetSup) {
        return (a, b, c, d) -> func4(packetSup).apply(a, b, c, d).toServer();
    }

    private static <A, B, C, D, E, T extends BasePacket.arg5<A, B, C, D, E>> Consumer5<A, B, C, D, E> sendToServer5(Supplier<T> packetSup) {
        return (a, b, c, d, e) -> func5(packetSup).apply(a, b, c, d, e).toServer();
    }

    private static <A, B, C, D, E, F, T extends BasePacket.arg6<A, B, C, D, E, F>> Consumer6<A, B, C, D, E, F> sendToServer6(Supplier<T> packetSup) {
        return (a, b, c, d, e, f) -> func6(packetSup).apply(a, b, c, d, e, f).toServer();
    }

    public interface Consumer3<A, B, C> {
        void accept(A a, B b, C c);
    }

    public interface Consumer4<A, B, C, D> {
        void accept(A a, B b, C c, D d);
    }

    public interface Consumer5<A, B, C, D, E> {
        void accept(A a, B b, C c, D d, E e);
    }

    public interface Consumer6<A, B, C, D, E, F> {
        void accept(A a, B b, C c, D d, E e, F f);
    }
}
