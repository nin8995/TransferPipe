package nin.transferpipe.particle;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static nin.transferpipe.TPMod.MODID;

public interface TPParticles {

    DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);

    RegistryObject<ParticleType<ColorSquare.Option>> COLOR_SQUARE = register("color_square", false, ColorSquare.Option.DESERIALIZER, ColorSquare.Option.CODEC);

    static <T extends ParticleOptions> RegistryObject<ParticleType<T>> register(String name, boolean bool, ParticleOptions.Deserializer<T> deserializer, Codec<T> codec) {
        return PARTICLES.register(name, () -> new ParticleType<>(bool, deserializer) {
            @Override
            public Codec<T> codec() {
                return codec;
            }
        });
    }

    static void init(IEventBus bus) {
        PARTICLES.register(bus);
    }

    static void clientInit(RegisterParticleProvidersEvent e) {
        e.registerSprite(COLOR_SQUARE.get(), ColorSquare::createParticle);
    }

    static void addPipeCorners(Level level, Vec3 pos, Vector3f color) {
        addBoxCorners(level, pos, 0.125F, defaultOption(color));
    }

    static void addBlockCorners(Level level, Vec3 pos, ColorSquare.Option option) {
        addBoxCorners(level, pos, 0.5F, option);
    }

    static void addBoxCorners(Level level, Vec3 pos, float boxRadius, ColorSquare.Option option) {
        plusMinusOne(x -> plusMinusOne(y -> plusMinusOne(z ->
                addParticle(level, pos, new Vec3(x, y, z).scale(boxRadius), option))));
    }

    static void plusMinusOne(IntConsumer func) {
        func.accept(1);
        func.accept(-1);
    }

    Map<Pair<Float, Integer>, List<List<Vec3>>> edgeCaches = new HashMap<>();

    static void addBoxEdges(Level level, Vec3 pos, float boxRadius, int particlePerMeter, Vector3f color) {
        addBoxEdges(level, pos, boxRadius, particlePerMeter, defaultOption(color));
    }

    static void addBoxEdges(Level level, Vec3 pos, float boxRadius, int particlePerMeter, ColorSquare.Option option) {
        var key = Pair.of(boxRadius, particlePerMeter);
        var edges = edgeCaches.get(key);
        if (edges == null) {
            edges = new ArrayList<>();
            addEdges(edges, boxRadius, particlePerMeter, i -> new Vec3(i, 0, 0));
            addEdges(edges, boxRadius, particlePerMeter, i -> new Vec3(0, i, 0));
            addEdges(edges, boxRadius, particlePerMeter, i -> new Vec3(0, 0, i));
            edgeCaches.put(key, edges);
        }
        edges.forEach(vs -> vs.forEach(v -> addParticle(level, pos, v, option)));
    }

    static void addEdges(List<List<Vec3>> edges, float boxRadius, int particlePerMeter, Function<Float, Vec3> toVec3) {
        plusMinusOne(x -> plusMinusOne(y ->
                edges.add(IntStream.range((int) (-boxRadius * particlePerMeter), (int) (boxRadius * particlePerMeter))
                        .mapToObj(i -> i / boxRadius / particlePerMeter)
                        .map(toVec3)
                        .map(v -> v.add(v.x != 0 ? new Vec3(0, x, y)
                                : v.y != 0 ? new Vec3(x, 0, y)
                                : new Vec3(x, y, 0)))
                        .map(v -> v.scale(boxRadius))
                        .toList())));
    }

    static void addParticle(Level level, Vec3 center, Vec3 relative, ParticleOptions option) {
        if (level instanceof ServerLevel sl)
            addParticle(sl, center.add(relative), relative.normalize(), option);
    }

    static void addParticle(ServerLevel sl, Vec3 pos, Vec3 vel, ParticleOptions particle) {
        sl.players().forEach(sp -> sl.sendParticles(sp, particle, true, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1));
    }

    static ColorSquare.Option defaultOption(Vector3f color) {
        return new ColorSquare.Option(color, 1F, 0.4F, 0.005F, 0.05F);
    }
}
